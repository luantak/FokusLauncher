package com.lu4p.fokuslauncher.ui.drawer

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.ProfileHeuristics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppDrawerUiState(
        val allApps: List<AppInfo> = emptyList(),
        /** One entry per Android user profile (personal, work, clone, …), after search/category. */
        val filteredProfileSections: List<DrawerProfileSectionUi> = emptyList(),
        val searchQuery: String = "",
        val autoOpenKeyboard: Boolean = true,
        val hideAllAppsSection: Boolean = false,
        val selectedCategory: String = ReservedCategoryNames.ALL_APPS,
        val categories: List<String> = listOf(ReservedCategoryNames.ALL_APPS),
        val selectedApp: AppInfo? = null,
        val showMenu: Boolean = false,
        val isPrivateSpaceSupported: Boolean = false,
        val isPrivateSpaceUnlocked: Boolean = false,
        /** Full (unfiltered) private space app list – used for launch lookups. */
        val privateSpaceApps: List<AppInfo> = emptyList(),
        /** Private space apps filtered by the current search query – used for display. */
        val filteredPrivateSpaceApps: List<AppInfo> = emptyList(),
        /** Package names of apps already on the home screen. */
        val favoritePackageNames: Set<String> = emptySet(),
        val selectedCategoryForActions: String? = null
)

sealed interface DrawerEvent {
    data class AutoLaunch(val target: LaunchTarget) : DrawerEvent
}

sealed interface LaunchTarget {
    data class MainApp(val packageName: String) : LaunchTarget
    data class PrivateApp(
            val packageName: String,
            val componentName: android.content.ComponentName,
            val userHandle: android.os.UserHandle
    ) : LaunchTarget
}

@HiltViewModel
class AppDrawerViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val privateSpaceManager: PrivateSpaceManager,
        private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DrawerEvent>()
    val events: SharedFlow<DrawerEvent> = _events.asSharedFlow()
    private var latestHiddenSet: Set<String> = emptySet()
    private var latestRenameMap: Map<String, String> = emptyMap()
    private var latestCategoryMap: Map<String, String> = emptyMap()
    private var latestDefinedCategories: List<String> = emptyList()

    init {
        loadApps()
        observeHiddenAndRenamed()
        observeInstalledApps()
        observeFavorites()
        observeDrawerKeyboardPreference()
        observeHideAllAppsPreference()
        refreshPrivateSpaceState()
        observePrivateSpaceChanges()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            preferencesManager.favoritesFlow.collect { favorites ->
                _uiState.update { state ->
                    state.copy(
                        favoritePackageNames = favorites.map { it.packageName }.toSet()
                    )
                }
            }
        }
    }

    private fun observeDrawerKeyboardPreference() {
        viewModelScope.launch {
            preferencesManager.autoOpenDrawerKeyboardFlow.collect { enabled ->
                _uiState.update { state -> state.copy(autoOpenKeyboard = enabled) }
            }
        }
    }

    private fun observeHideAllAppsPreference() {
        viewModelScope.launch {
            preferencesManager.hideAllAppsSectionFlow.collect { hideAllAppsSection ->
                _uiState.update { state ->
                    val categories =
                            deriveCategories(
                                    apps = state.allApps,
                                    definedCategories = latestDefinedCategories,
                                    includePrivate =
                                            state.isPrivateSpaceUnlocked &&
                                                    state.privateSpaceApps.isNotEmpty(),
                                    includeAllAppsSection = !hideAllAppsSection
                            )
                    val selectedCategory =
                            resolveSelectedCategory(
                                    currentCategory = state.selectedCategory,
                                    categories = categories,
                                    hideAllAppsSection = hideAllAppsSection
                            )
                    val sections = buildProfileSections(state.allApps)
                    val filteredSections =
                            filterProfileSections(
                                    sections = sections,
                                    query = state.searchQuery,
                                    category = selectedCategory
                            )
                    state.copy(
                            hideAllAppsSection = hideAllAppsSection,
                            selectedCategory = selectedCategory,
                            categories = categories,
                            filteredProfileSections = filteredSections,
                            filteredPrivateSpaceApps =
                                    applyPrivateFilter(
                                            query = state.searchQuery,
                                            selectedCategory = selectedCategory,
                                            privateApps = state.privateSpaceApps
                                    )
                    )
                }
            }
        }
    }

    /**
     * Loads raw installed apps on a background thread and stores them. The hidden/renamed overlay
     * is applied reactively via [observeHiddenAndRenamed].
     */
    private fun loadApps() {
        viewModelScope.launch {
            rebuildVisibleApps(
                    hiddenSet = latestHiddenSet,
                    renameMap = latestRenameMap,
                    categoryMap = latestCategoryMap,
                    definedCategories = latestDefinedCategories
            )
        }
    }

    /**
     * Observes the hidden-package-names and renamed-apps Flows from Room and rebuilds the visible
     * app list whenever either changes.
     */
    private fun observeHiddenAndRenamed() {
        viewModelScope.launch {
            combine(
                            appRepository.getHiddenPackageNames(),
                            appRepository.getAllRenamedApps(),
                            appRepository.getAllAppCategories(),
                            appRepository.getAllCategoryDefinitions()
                    ) { hiddenNames, renamedApps, categories, categoryDefinitions ->
                CombinedCategoryState(
                        hiddenSet = hiddenNames.toSet(),
                        renameMap = renamedApps.associate { it.packageName to it.customName },
                        categoryMap = categories.associate { it.packageName to it.category },
                        definedCategories = categoryDefinitions.map { it.name }
                )
            }
                    .collect { state ->
                        latestHiddenSet = state.hiddenSet
                        latestRenameMap = state.renameMap
                        latestCategoryMap = state.categoryMap
                        latestDefinedCategories = state.definedCategories
                        rebuildVisibleApps(
                                hiddenSet = state.hiddenSet,
                                renameMap = state.renameMap,
                                categoryMap = state.categoryMap,
                                definedCategories = state.definedCategories
                        )
                    }
        }
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            appRepository.getInstalledAppsVersion().drop(1).collect {
                rebuildVisibleApps(
                        hiddenSet = latestHiddenSet,
                        renameMap = latestRenameMap,
                        categoryMap = latestCategoryMap,
                        definedCategories = latestDefinedCategories
                )
            }
        }
    }

    /**
     * Applies hidden + renamed overlays and updates profile sections. Runs the expensive
     * PackageManager query off the main thread.
     */
    private suspend fun rebuildVisibleApps(
            hiddenSet: Set<String>,
            renameMap: Map<String, String>,
            categoryMap: Map<String, String>,
            definedCategories: List<String>
    ) {
        val base = withContext(Dispatchers.IO) { appRepository.getInstalledApps() }
        val visible =
                base
                        .filter { it.packageName !in hiddenSet }
                        .map { app ->
                            val customName = renameMap[app.packageName]
                            val customCategory = categoryMap[app.packageName]
                            app.copy(
                                    label = customName ?: app.label,
                                    category = customCategory ?: app.category
                            )
                        }

        _uiState.update { state ->
            val categories =
                    deriveCategories(
                            apps = visible,
                            definedCategories = definedCategories,
                            includePrivate =
                                    state.isPrivateSpaceUnlocked &&
                                            state.privateSpaceApps.isNotEmpty(),
                            includeAllAppsSection = !state.hideAllAppsSection
                    )
            val selectedCategory =
                    resolveSelectedCategory(
                            currentCategory = state.selectedCategory,
                            categories = categories,
                            hideAllAppsSection = state.hideAllAppsSection
                    )
            val sections = buildProfileSections(visible)
            val filteredSections =
                    filterProfileSections(
                            sections = sections,
                            query = state.searchQuery,
                            category = selectedCategory
                    )
            val filteredPrivate =
                    applyPrivateFilter(
                            query = state.searchQuery,
                            selectedCategory = selectedCategory,
                            privateApps = state.privateSpaceApps
                    )
            state.copy(
                    allApps = visible,
                    selectedCategory = selectedCategory,
                    filteredProfileSections = filteredSections
            )
                    .copy(categories = categories, filteredPrivateSpaceApps = filteredPrivate)
        }
    }

    // --- Search ---

    fun onSearchQueryChanged(query: String) {
        // Use trimmed query for filtering so a leading-space prefix still matches
        val trimmed = query.trimStart()

        _uiState.update { state ->
            val sections = buildProfileSections(state.allApps)
            val filteredSections =
                    filterProfileSections(
                            sections = sections,
                            query = trimmed,
                            category = state.selectedCategory
                    )
            val filteredPrivate =
                    applyPrivateFilter(
                            query = trimmed,
                            selectedCategory = state.selectedCategory,
                            privateApps = state.privateSpaceApps
                    )
            state.copy(
                    searchQuery = query,
                    filteredProfileSections = filteredSections,
                    filteredPrivateSpaceApps = filteredPrivate
            )
        }

        // Auto-launch when exactly one app matches across both lists.
        // A leading space means "browse mode" – show the result but don't launch.
        val browseMode = query.startsWith(" ")
        val state = _uiState.value
        val mainFlat = state.filteredProfileSections.flatMap { it.apps }
        val allMatches =
                buildLaunchTargets(
                        privateApps = state.filteredPrivateSpaceApps,
                        mainApps = mainFlat
                )
        if (!browseMode && trimmed.isNotBlank() && allMatches.size == 1) {
            val target = allMatches[0]
            if (launchTarget(target)) {
                resetSearchState()
                viewModelScope.launch { _events.emit(DrawerEvent.AutoLaunch(target)) }
            }
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            val sections = buildProfileSections(state.allApps)
            val filteredSections =
                    filterProfileSections(
                            sections = sections,
                            query = state.searchQuery,
                            category = category
                    )
            val filteredPrivate =
                    applyPrivateFilter(
                            query = state.searchQuery,
                            selectedCategory = category,
                            privateApps = state.privateSpaceApps
                    )
            state.copy(
                    selectedCategory = category,
                    filteredProfileSections = filteredSections,
                    filteredPrivateSpaceApps = filteredPrivate
            )
        }
    }

    fun onCategoryLongPress(category: String) {
        if (category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (category.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        _uiState.update { it.copy(selectedCategoryForActions = category) }
    }

    fun dismissCategoryActionSheet() {
        _uiState.update { it.copy(selectedCategoryForActions = null) }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            appRepository.renameCategory(oldName, newName)
            dismissCategoryActionSheet()
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            appRepository.deleteCategory(name)
            if (_uiState.value.selectedCategory.equals(name, ignoreCase = true)) {
                val categoriesAfterDelete =
                        _uiState.value.categories.filterNot { it.equals(name, ignoreCase = true) }
                onCategorySelected(
                        defaultCategory(
                                categories = categoriesAfterDelete,
                                hideAllAppsSection = _uiState.value.hideAllAppsSection
                        )
                )
            }
            dismissCategoryActionSheet()
        }
    }

    // --- Launch ---

    fun launchApp(packageName: String): Boolean {
        return launchTarget(LaunchTarget.MainApp(packageName))
    }

    fun launchTarget(target: LaunchTarget): Boolean {
        return when (target) {
            is LaunchTarget.MainApp -> appRepository.launchApp(target.packageName)
            is LaunchTarget.PrivateApp ->
                    privateSpaceManager.launchApp(target.componentName, target.userHandle)
        }
    }

    // --- Long-press actions ---

    fun onAppLongPress(app: AppInfo) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun dismissActionSheet() {
        _uiState.update { it.copy(selectedApp = null) }
    }

    fun hideApp(app: AppInfo) {
        viewModelScope.launch {
            appRepository.hideApp(app.packageName)
            // The Flow observer in observeHiddenAndRenamed will rebuild the list
        }
    }

    fun addToHomeScreen(app: AppInfo) {
        viewModelScope.launch {
            if (app.userHandle != null) return@launch
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (current.any { it.packageName == app.packageName }) return@launch
            current.add(
                    0,
                    FavoriteApp(
                            label = app.label,
                            packageName = app.packageName,
                            iconName = "circle",
                            iconPackage = app.packageName
                    )
            )
            preferencesManager.setFavorites(current)
        }
    }

    fun renameApp(packageName: String, newName: String) {
        viewModelScope.launch {
            appRepository.renameApp(packageName, newName)
            // The Flow observer in observeHiddenAndRenamed will rebuild the list
        }
    }

    // --- Overflow menu ---

    fun toggleMenu() {
        _uiState.update { it.copy(showMenu = !it.showMenu) }
    }
    fun dismissMenu() {
        _uiState.update { it.copy(showMenu = false) }
    }

    // --- Private Space ---

    /**
     * Reacts to system broadcasts when the Private Space profile becomes available (unlocked) or
     * unavailable (locked), so the UI updates immediately instead of requiring a manual drawer
     * reopen.
     */
    private fun observePrivateSpaceChanges() {
        viewModelScope.launch {
            privateSpaceManager.profileStateChanged.collect { refreshPrivateSpaceState() }
        }
    }

    fun refreshPrivateSpaceState() {
        val supported = privateSpaceManager.isSupported
        val unlocked = privateSpaceManager.isPrivateSpaceUnlocked()
        val apps = if (unlocked) privateSpaceManager.getPrivateSpaceApps() else emptyList()
        _uiState.update { state ->
            val categories =
                    deriveCategories(
                            apps = state.allApps,
                            definedCategories =
                                    state.categories
                                            .filterNot {
                                                it.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ||
                                                        it.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)
                                            },
                            includePrivate = unlocked && apps.isNotEmpty(),
                            includeAllAppsSection = !state.hideAllAppsSection
                    )
            val selectedCategory =
                    resolveSelectedCategory(
                            currentCategory = state.selectedCategory,
                            categories = categories,
                            hideAllAppsSection = state.hideAllAppsSection
                    )
            val filteredPrivate =
                    applyPrivateFilter(
                            query = state.searchQuery,
                            selectedCategory = selectedCategory,
                            privateApps = apps
                    )
            val sections = buildProfileSections(state.allApps)
            val filteredSections =
                    filterProfileSections(
                            sections = sections,
                            query = state.searchQuery,
                            category = selectedCategory
                    )
            state.copy(
                    isPrivateSpaceSupported = supported,
                    isPrivateSpaceUnlocked = unlocked,
                    privateSpaceApps = apps,
                    selectedCategory = selectedCategory,
                    filteredProfileSections = filteredSections,
                    filteredPrivateSpaceApps = filteredPrivate,
                    categories = categories
            )
        }
    }

    fun togglePrivateSpace() {
        if (_uiState.value.isPrivateSpaceUnlocked) {
            privateSpaceManager.lock()
            _uiState.update {
                it.copy(
                        isPrivateSpaceUnlocked = false,
                        privateSpaceApps = emptyList(),
                        filteredPrivateSpaceApps = emptyList()
                )
            }
        } else {
            privateSpaceManager.requestUnlock()
            // After the system auth prompt completes, caller should call refreshPrivateSpaceState()
        }
        dismissMenu()
    }

    fun refresh() {
        appRepository.invalidateCache()
        viewModelScope.launch {
            rebuildVisibleApps(
                    hiddenSet = latestHiddenSet,
                    renameMap = latestRenameMap,
                    categoryMap = latestCategoryMap,
                    definedCategories = latestDefinedCategories
            )
        }
    }

    fun resetSearchState() {
        _uiState.update { state ->
            val defaultCategory =
                    defaultCategory(
                            categories = state.categories,
                            hideAllAppsSection = state.hideAllAppsSection
                    )
            val sections = buildProfileSections(state.allApps)
            val filteredSections =
                    filterProfileSections(
                            sections = sections,
                            query = "",
                            category = defaultCategory
                    )
            state.copy(
                    searchQuery = "",
                    selectedCategory = defaultCategory,
                    filteredProfileSections = filteredSections,
                    filteredPrivateSpaceApps =
                            applyPrivateFilter(
                                    query = "",
                                    selectedCategory = defaultCategory,
                                    privateApps = state.privateSpaceApps
                            )
            )
        }
    }

    // --- Filtering ---

    private fun buildProfileSections(apps: List<AppInfo>): List<DrawerProfileSectionUi> {
        val userManager =
                try {
                    context.getSystemService(Context.USER_SERVICE) as? UserManager
                } catch (_: Exception) {
                    null
                }
        if (userManager == null) {
            val ownerApps = apps.filter { it.userHandle == null }.sortedBy { it.label.lowercase() }
            val byUser = apps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
            return buildList {
                if (ownerApps.isNotEmpty()) {
                    add(
                            DrawerProfileSectionUi(
                                    id = "owner",
                                    title = context.getString(R.string.drawer_section_personal),
                                    apps = ownerApps
                            )
                    )
                }
                for (user in byUser.keys) {
                    val list = byUser.getValue(user).sortedBy { it.label.lowercase() }
                    val title =
                            when {
                                byUser.keys.size == 1 &&
                                        !ProfileHeuristics.isLikelyCloneOrParallelProfile(
                                                context,
                                                user
                                        ) ->
                                        context.getString(R.string.drawer_section_work_profile)
                                ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                        context.getString(R.string.drawer_section_clone_profile)
                                else -> context.getString(R.string.drawer_section_other_profile)
                            }
                    add(
                            DrawerProfileSectionUi(
                                    id = "u_${user.hashCode()}",
                                    title = title,
                                    apps = list
                            )
                    )
                }
            }
        }

        val ownerApps = apps.filter { it.userHandle == null }.sortedBy { it.label.lowercase() }
        val byUser = apps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
        val orderedUsers =
                byUser.keys.sortedBy { uh ->
                    try {
                        userManager.getSerialNumberForUser(uh)
                    } catch (_: Exception) {
                        Long.MAX_VALUE
                    }
                }

        return buildList {
            if (ownerApps.isNotEmpty()) {
                add(
                        DrawerProfileSectionUi(
                                id = "owner",
                                title = context.getString(R.string.drawer_section_personal),
                                apps = ownerApps
                        )
                )
            }
            for (user in orderedUsers) {
                val list = byUser.getValue(user).sortedBy { it.label.lowercase() }
                add(
                        DrawerProfileSectionUi(
                                id = "u_${user.hashCode()}",
                                title =
                                        profileTitleForUser(
                                                user = user,
                                                userManager = userManager,
                                                totalSecondaryProfiles = orderedUsers.size
                                        ),
                                apps = list
                        )
                )
            }
        }
    }

    private fun filterProfileSections(
            sections: List<DrawerProfileSectionUi>,
            query: String,
            category: String
    ): List<DrawerProfileSectionUi> {
        if (category.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) {
            return sections.map { it.copy(apps = emptyList()) }
        }
        return sections.map { section ->
            var apps = section.apps
            if (query.isNotBlank()) {
                apps = apps.filter { it.label.contains(query, ignoreCase = true) }
            }
            if (category.isNotBlank() && !category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) {
                apps = apps.filter { it.category.equals(category, ignoreCase = true) }
            }
            section.copy(apps = apps)
        }
    }

    private fun profileTitleForUser(
            user: UserHandle,
            userManager: UserManager,
            totalSecondaryProfiles: Int
    ): String {
        if (ProfileHeuristics.isManagedProfileForUser(userManager, user)) {
            return context.getString(R.string.drawer_section_work_profile)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcherApps =
                    try {
                        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                    } catch (_: Exception) {
                        null
                    }
            if (launcherApps != null) {
                try {
                    when (launcherApps.getLauncherUserInfo(user)?.userType) {
                        "android.os.usertype.profile.MANAGED" ->
                                return context.getString(R.string.drawer_section_work_profile)
                    }
                } catch (_: Exception) {}
            }
        }
        if (ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user)) {
            return context.getString(R.string.drawer_section_clone_profile)
        }
        if (totalSecondaryProfiles == 1) {
            return context.getString(R.string.drawer_section_work_profile)
        }
        val serial =
                try {
                    userManager.getSerialNumberForUser(user)
                } catch (_: Exception) {
                    -1L
                }
        return if (serial >= 0L) {
            context.getString(R.string.drawer_section_profile_numbered, serial)
        } else {
            context.getString(R.string.drawer_section_other_profile)
        }
    }

    private fun applyPrivateFilter(
            query: String,
            selectedCategory: String,
            privateApps: List<AppInfo>
    ): List<AppInfo> {
        if (selectedCategory != ReservedCategoryNames.ALL_APPS &&
                        selectedCategory != ReservedCategoryNames.PRIVATE
        ) {
            return emptyList()
        }
        return if (query.isBlank()) {
            privateApps
        } else {
            privateApps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    private fun resolveSelectedCategory(
            currentCategory: String,
            categories: List<String>,
            hideAllAppsSection: Boolean
    ): String {
        return if (categories.any { it.equals(currentCategory, ignoreCase = true) }) {
            currentCategory
        } else {
            defaultCategory(categories, hideAllAppsSection)
        }
    }

    private fun defaultCategory(categories: List<String>, hideAllAppsSection: Boolean): String {
        if (!hideAllAppsSection) return ReservedCategoryNames.ALL_APPS
        return categories.firstOrNull() ?: ReservedCategoryNames.ALL_APPS
    }

    private fun deriveCategories(
            apps: List<AppInfo>,
            definedCategories: List<String>,
            includePrivate: Boolean,
            includeAllAppsSection: Boolean
    ): List<String> {
        val dynamic = apps.map { it.category.trim() }.filter { it.isNotBlank() }.toSet()
        val orderedDefined = definedCategories.distinct()
        val extras = (dynamic - orderedDefined.toSet()).toList().sorted()
        return buildList {
            if (includeAllAppsSection) add(ReservedCategoryNames.ALL_APPS)
            if (includePrivate) add(ReservedCategoryNames.PRIVATE)
            addAll(orderedDefined)
            addAll(extras)
        }
    }

    private fun buildLaunchTargets(
            privateApps: List<AppInfo>,
            mainApps: List<AppInfo>
    ): List<LaunchTarget> {
        val privateTargets =
                privateApps.mapNotNull { app ->
                    val componentName = app.componentName ?: return@mapNotNull null
                    val userHandle = app.userHandle ?: return@mapNotNull null
                    LaunchTarget.PrivateApp(
                            packageName = app.packageName,
                            componentName = componentName,
                            userHandle = userHandle
                    )
                }
        val mainTargets =
                mainApps.map { app ->
                    val uh = app.userHandle
                    val cn = app.componentName
                    if (uh != null && cn != null) {
                        LaunchTarget.PrivateApp(
                                packageName = app.packageName,
                                componentName = cn,
                                userHandle = uh
                        )
                    } else {
                        LaunchTarget.MainApp(app.packageName)
                    }
                }
        return privateTargets + mainTargets
    }

    private data class CombinedCategoryState(
            val hiddenSet: Set<String>,
            val renameMap: Map<String, String>,
            val categoryMap: Map<String, String>,
            val definedCategories: List<String>
    )
}
