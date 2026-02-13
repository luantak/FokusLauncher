package com.lu4p.fokuslauncher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppDrawerUiState(
        val allApps: List<AppInfo> = emptyList(),
        val filteredApps: List<AppInfo> = emptyList(),
        val searchQuery: String = "",
        val selectedCategory: String = "All apps",
        val categories: List<String> = listOf("All apps"),
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
        observeFavorites()
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

    /**
     * Applies hidden + renamed overlays and updates filteredApps. Runs the expensive PackageManager
     * query off the main thread.
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
                        .sortedBy { it.label.lowercase() }

        _uiState.update { state ->
            val filtered = applyFilters(state.searchQuery, state.selectedCategory, visible)
            val categories =
                    deriveCategories(
                            apps = visible,
                            definedCategories = definedCategories,
                            includePrivate =
                                    state.isPrivateSpaceUnlocked &&
                                            state.privateSpaceApps.isNotEmpty()
                    )
            val filteredPrivate =
                    applyPrivateFilter(
                            query = state.searchQuery,
                            selectedCategory = state.selectedCategory,
                            privateApps = state.privateSpaceApps
                    )
            state.copy(allApps = visible, filteredApps = filtered)
                    .copy(categories = categories, filteredPrivateSpaceApps = filteredPrivate)
        }
    }

    // --- Search ---

    fun onSearchQueryChanged(query: String) {
        // Use trimmed query for filtering so a leading-space prefix still matches
        val trimmed = query.trimStart()

        _uiState.update { state ->
            val filtered = applyFilters(trimmed, state.selectedCategory, state.allApps)
            val filteredPrivate =
                    applyPrivateFilter(
                            query = trimmed,
                            selectedCategory = state.selectedCategory,
                            privateApps = state.privateSpaceApps
                    )
            state.copy(
                    searchQuery = query,
                    filteredApps = filtered,
                    filteredPrivateSpaceApps = filteredPrivate
            )
        }

        // Auto-launch when exactly one app matches across both lists.
        // A leading space means "browse mode" – show the result but don't launch.
        val browseMode = query.startsWith(" ")
        val state = _uiState.value
        val allMatches =
                buildLaunchTargets(
                        privateApps = state.filteredPrivateSpaceApps,
                        mainApps = state.filteredApps
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
            val filtered = applyFilters(state.searchQuery, category, state.allApps)
            val filteredPrivate =
                    applyPrivateFilter(
                            query = state.searchQuery,
                            selectedCategory = category,
                            privateApps = state.privateSpaceApps
                    )
            state.copy(
                    selectedCategory = category,
                    filteredApps = filtered,
                    filteredPrivateSpaceApps = filteredPrivate
            )
        }
    }

    fun onCategoryLongPress(category: String) {
        if (category.equals("All apps", ignoreCase = true)) return
        if (category.equals("Private", ignoreCase = true)) return
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
                onCategorySelected("All apps")
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
                                                it.equals("All apps", ignoreCase = true) ||
                                                        it.equals("Private", ignoreCase = true)
                                            },
                            includePrivate = unlocked && apps.isNotEmpty()
                    )
            val filteredPrivate =
                    applyPrivateFilter(
                            query = state.searchQuery,
                            selectedCategory = state.selectedCategory,
                            privateApps = apps
                    )
            state.copy(
                    isPrivateSpaceSupported = supported,
                    isPrivateSpaceUnlocked = unlocked,
                    privateSpaceApps = apps,
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
            val defaultCategory = "All apps"
            state.copy(
                    searchQuery = "",
                    selectedCategory = defaultCategory,
                    filteredApps = applyFilters("", defaultCategory, state.allApps),
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

    private fun applyFilters(query: String, category: String, apps: List<AppInfo>): List<AppInfo> {
        if (category.equals("Private", ignoreCase = true)) return emptyList()
        var result = apps
        if (query.isNotBlank()) {
            result = result.filter { it.label.contains(query, ignoreCase = true) }
        }
        if (category.isNotBlank() && category != "All apps") {
            result = result.filter { it.category.equals(category, ignoreCase = true) }
        }
        return result
    }

    private fun applyPrivateFilter(
            query: String,
            selectedCategory: String,
            privateApps: List<AppInfo>
    ): List<AppInfo> {
        if (selectedCategory != "All apps" && selectedCategory != "Private") return emptyList()
        return if (query.isBlank()) {
            privateApps
        } else {
            privateApps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    private fun deriveCategories(
            apps: List<AppInfo>,
            definedCategories: List<String>,
            includePrivate: Boolean
    ): List<String> {
        val dynamic = apps.map { it.category.trim() }.filter { it.isNotBlank() }.toSet()
        val orderedDefined = definedCategories.distinct()
        val extras = (dynamic - orderedDefined.toSet()).toList().sorted()
        return buildList {
            add("All apps")
            if (includePrivate) add("Private")
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
        val mainTargets = mainApps.map { LaunchTarget.MainApp(it.packageName) }
        return privateTargets + mainTargets
    }

    private data class CombinedCategoryState(
            val hiddenSet: Set<String>,
            val renameMap: Map<String, String>,
            val categoryMap: Map<String, String>,
            val definedCategories: List<String>
    )
}
