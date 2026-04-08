package com.lu4p.fokuslauncher.ui.drawer

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.DotSearchTargetPreference
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.drawerOpenCountKey
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.utils.DotSearchParsed
import com.lu4p.fokuslauncher.utils.DotSearchSyntax
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AppDrawerUiState(
        val allApps: List<AppInfo> = emptyList(),
        /** One entry per Android user profile (personal, work, clone, …), after search/category. */
        val filteredProfileSections: List<DrawerProfileSectionUi> = emptyList(),
        val searchQuery: String = "",
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
        val selectedCategoryForActions: String? = null,
        /**
         * Vertical category sidebar layout (optional setting). When true, the drawer omits the
         * search bar and uses a leading sidebar instead of horizontal chips.
         */
        val useSidebarCategoryDrawer: Boolean = false,
        /** When true (default), the category rail is on the physical right; false = left rail. */
        val drawerCategorySidebarOnRight: Boolean = true,
        /** Normalized category key → MinimalIcons name for the vertical rail and chip affordances. */
        val categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        val drawerAppSortMode: DrawerAppSortMode = DrawerAppSortMode.ALPHABETICAL,
        /**
         * When true with [drawerAppSortMode] CUSTOM and sidebar layout, the list shows drag handles and
         * can be reordered. Cleared when the drawer closes, search filters, or CUSTOM layout is unavailable.
         */
        val drawerReorderSessionActive: Boolean = false
)

sealed interface DrawerEvent {
    data class AutoLaunch(val target: LaunchTarget) : DrawerEvent
}

sealed interface LaunchTarget {
    data class MainApp(val packageName: String) : LaunchTarget
    data class PrivateApp(
            val packageName: String,
            val componentName: ComponentName,
            val userHandle: UserHandle
    ) : LaunchTarget
}

private data class FilteredDrawerContent(
        val filteredProfileSections: List<DrawerProfileSectionUi>,
        val filteredPrivateSpaceApps: List<AppInfo>
)

@HiltViewModel
class AppDrawerViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val privateSpaceManager: PrivateSpaceManager,
        private val preferencesManager: PreferencesManager,
        @param:Named("DrawerComputation") private val drawerComputationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DrawerEvent>()
    val events: SharedFlow<DrawerEvent> = _events.asSharedFlow()
    private var latestHiddenSet: Set<String> = emptySet()
    private var latestRenameMap: Map<String, String> = emptyMap()
    private var latestCategoryMap: Map<String, String> = emptyMap()
    private var latestDefinedCategories: List<String> = emptyList()
    private var latestDrawerSortMode: DrawerAppSortMode = DrawerAppSortMode.ALPHABETICAL
    private var latestOpenCounts: Map<String, Int> = emptyMap()
    /** Profile key → ordered drawer open-count keys ([drawerOpenCountKey]); used when sort is CUSTOM. */
    private var latestCustomOrderByProfile: Map<String, List<String>> = emptyMap()
    private var latestUseSidebarCategoryDrawer: Boolean = false


    // --- Profile sections cache (invalidates when apps list identity or sort inputs change) ---
    private val profileSectionCacheLock = Any()
    private var profileSectionsCache: List<DrawerProfileSectionUi>? = null
    private var profileSectionsCacheApps: List<AppInfo>? = null
    private var profileSectionsCacheSortMode: DrawerAppSortMode? = null
    private var profileSectionsCacheCounts: Map<String, Int>? = null
    private var profileSectionsCacheCustomOrder: Map<String, List<String>>? = null

    // --- Private space list cache (sorted order; raw list often new instance, same contents) ---
    private val privateSortCacheLock = Any()
    private var privateSortCacheFingerprint: Int = 0
    private var privateSortCacheSortMode: DrawerAppSortMode? = null
    private var privateSortCacheCounts: Map<String, Int>? = null
    private var privateSortCacheResult: List<AppInfo>? = null

    private var searchQueryApplyJob: Job? = null
    private var searchQueryRequestId: Long = 0

    private var drawerDotSearchDefault: DotSearchTargetPreference = DotSearchTargetPreference()
    private var drawerDotSearchAliases: Map<Char, DotSearchTargetPreference> = emptyMap()

    /**
     * Package/profile pairs removed via [applyImmediatePackageRemoval] before the next successful
     * [rebuildVisibleApps] from [AppRepository.getInstalledApps]. Prevents a slower in-flight rebuild
     * (e.g. from init) from overwriting the drawer with stale install data.
     */
    private val optimisticallyRemovedKeys = mutableSetOf<String>()

    /**
     * [rebuildVisibleApps] is triggered from several collectors at once (init, Room metadata,
     * package-cache invalidation). Without serialization, a slow load that briefly gets an empty
     * [LauncherApps] result can finish after a successful rebuild and wipe the drawer until
     * process death.
     */
    private val drawerListRebuildMutex = Mutex()

    init {
        loadApps()
        observeHiddenAndRenamed()
        observeInstalledApps()
        observeRemovedPackages()
        observeFavorites()
        observeDrawerSidebarPreference()
        observeDrawerCategoryRailAndIcons()
        observeDrawerSortOpenCountsAndCustomOrder()
        observeDrawerDotSearchPreferences()
        refreshPrivateSpaceState()
        observePrivateSpaceChanges()
        scheduleDrawerCachePrewarm()
    }

    private fun observeDrawerDotSearchPreferences() {
        viewModelScope.launch {
            preferencesManager.drawerDotSearchDefaultFlow.collect { drawerDotSearchDefault = it }
        }
        viewModelScope.launch {
            preferencesManager.drawerDotSearchAliasesFlow.collect { drawerDotSearchAliases = it }
        }
    }

    /**
     * Loads profile-section and private-app sort caches for the current [AppDrawerUiState] without
     * publishing UI. Schedules work on [viewModelScope] so the drawer’s first open avoids cold CPU
     * work when possible.
     */
    private fun scheduleDrawerCachePrewarm() {
        viewModelScope.launch { prewarmDrawerCachesSuspend() }
    }

    private suspend fun prewarmDrawerCachesSuspend() {
        val state = _uiState.value
        buildProfileSectionsSuspend(state.allApps)
        if (state.privateSpaceApps.isNotEmpty()) {
            sortPrivateSpaceAppsCachedSuspend(state.privateSpaceApps)
        }
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

    private fun observeDrawerSidebarPreference() {
        viewModelScope.launch {
            preferencesManager.drawerSidebarCategoriesFlow.collect { sidebarEnabled ->
                latestUseSidebarCategoryDrawer = sidebarEnabled
                if (!sidebarEnabled) {
                    val sortMode = preferencesManager.drawerAppSortModeFlow.first()
                    if (sortMode == DrawerAppSortMode.CUSTOM) {
                        preferencesManager.setDrawerAppSortMode(DrawerAppSortMode.ALPHABETICAL)
                    }
                }
                val state = _uiState.value
                val categories =
                        deriveCategories(
                                apps = state.allApps,
                                definedCategories = latestDefinedCategories,
                                includePrivate =
                                        state.isPrivateSpaceUnlocked &&
                                                state.privateSpaceApps.isNotEmpty(),
                                includeWork =
                                        state.allApps.any { isDrawerWorkProfileApp(context, it) },
                                includeAllAppsSection = !sidebarEnabled
                        )
                val selectedCategory =
                        resolveSelectedCategory(
                                currentCategory = state.selectedCategory,
                                categories = categories,
                                skipAllAppsCategory = sidebarEnabled
                        )
                val filteredContent =
                        buildFilteredDrawerContent(
                                allApps = state.allApps,
                                privateApps = state.privateSpaceApps,
                                rawSearchQuery = state.searchQuery,
                                category = selectedCategory
                        )
                _uiState.update {
                    it.copy(
                            useSidebarCategoryDrawer = sidebarEnabled,
                            selectedCategory = selectedCategory,
                            categories = categories,
                            filteredProfileSections = filteredContent.filteredProfileSections,
                            filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps,
                            drawerReorderSessionActive =
                                    if (!sidebarEnabled) false else it.drawerReorderSessionActive
                    )
                }
                scheduleDrawerCachePrewarm()
            }
        }
    }

    private fun observeDrawerCategoryRailAndIcons() {
        viewModelScope.launch {
            preferencesManager.drawerCategorySidebarOnLeftFlow.collect { onLeft ->
                _uiState.update { state ->
                    state.copy(drawerCategorySidebarOnRight = !onLeft)
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.drawerCategoryIconsFlow.collect { icons ->
                _uiState.update { state -> state.copy(categoryDrawerIconOverrides = icons) }
            }
        }
    }

    private fun observeDrawerSortOpenCountsAndCustomOrder() {
        viewModelScope.launch {
            combine(
                            preferencesManager.drawerAppSortModeFlow,
                            preferencesManager.drawerAppOpenCountsFlow,
                            preferencesManager.drawerCustomAppOrderFlow
                    ) { mode, counts, customOrder -> Triple(mode, counts, customOrder) }
                    .collect { (mode, counts, customOrder) ->
                        latestDrawerSortMode = mode
                        latestOpenCounts = counts
                        latestCustomOrderByProfile = customOrder
                        val state = _uiState.value
                        val reorderedPrivate =
                                if (state.privateSpaceApps.isEmpty()) {
                                    state.privateSpaceApps
                                } else {
                                    sortPrivateSpaceAppsCachedSuspend(state.privateSpaceApps)
                                }
                        val filteredContent =
                                buildFilteredDrawerContent(
                                        allApps = state.allApps,
                                        privateApps = reorderedPrivate,
                                        rawSearchQuery = state.searchQuery,
                                        category = state.selectedCategory
                                )
                        _uiState.update {
                            it.copy(
                                    privateSpaceApps = reorderedPrivate,
                                    filteredProfileSections = filteredContent.filteredProfileSections,
                                    filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps,
                                    drawerAppSortMode = mode,
                                    drawerReorderSessionActive =
                                            if (mode != DrawerAppSortMode.CUSTOM) {
                                                false
                                            } else {
                                                it.drawerReorderSessionActive
                                            }
                            )
                        }
                        scheduleDrawerCachePrewarm()
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
                synchronized(optimisticallyRemovedKeys) { optimisticallyRemovedKeys.clear() }
                rebuildVisibleApps(
                        hiddenSet = latestHiddenSet,
                        renameMap = latestRenameMap,
                        categoryMap = latestCategoryMap,
                        definedCategories = latestDefinedCategories
                )
            }
        }
    }

    private fun observeRemovedPackages() {
        viewModelScope.launch {
            appRepository.getRemovedPackages().collect { removedApp ->
                applyImmediatePackageRemoval(removedApp.packageName, removedApp.profileKey)
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
        drawerListRebuildMutex.withLock {
            val base = withContext(Dispatchers.IO) { appRepository.getInstalledApps() }
            val removedSnapshot =
                    synchronized(optimisticallyRemovedKeys) { optimisticallyRemovedKeys.toSet() }
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
                            .filterNot { app ->
                                drawerOpenCountKey(app.packageName, app.userHandle) in
                                        removedSnapshot
                            }

            val stateSnapshot = _uiState.value
            val privateAppsFiltered =
                    stateSnapshot.privateSpaceApps.filterNot { app ->
                        drawerOpenCountKey(app.packageName, app.userHandle) in removedSnapshot
                    }
            val categories =
                    deriveCategories(
                            apps = visible,
                            definedCategories = definedCategories,
                            includePrivate =
                                    stateSnapshot.isPrivateSpaceUnlocked &&
                                            privateAppsFiltered.isNotEmpty(),
                            includeWork = visible.any { isDrawerWorkProfileApp(context, it) },
                            includeAllAppsSection = !stateSnapshot.useSidebarCategoryDrawer
                    )
            val selectedCategory =
                    resolveSelectedCategory(
                            currentCategory = stateSnapshot.selectedCategory,
                            categories = categories,
                            skipAllAppsCategory = stateSnapshot.useSidebarCategoryDrawer
                    )
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = visible,
                            privateApps = privateAppsFiltered,
                            rawSearchQuery = stateSnapshot.searchQuery,
                            category = selectedCategory
                    )
            _uiState.update { state ->
                state.copy(
                                allApps = visible,
                                privateSpaceApps = privateAppsFiltered,
                                selectedCategory = selectedCategory,
                                filteredProfileSections = filteredContent.filteredProfileSections
                        )
                        .copy(
                                categories = categories,
                                filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps
                        )
            }
            scheduleDrawerCachePrewarm()
        }
    }

    private suspend fun applyImmediatePackageRemoval(packageName: String, profileKey: String) {
        drawerListRebuildMutex.withLock {
            synchronized(optimisticallyRemovedKeys) {
                optimisticallyRemovedKeys.add(drawerOpenCountKey(packageName, profileKey))
            }
            val stateSnapshot = _uiState.value
            val visible =
                    stateSnapshot.allApps.filterNot {
                        it.packageName == packageName && appProfileKey(it.userHandle) == profileKey
                    }
            val privateApps =
                    stateSnapshot.privateSpaceApps.filterNot {
                        it.packageName == packageName && appProfileKey(it.userHandle) == profileKey
                    }
            val categories =
                    deriveCategories(
                            apps = visible,
                            definedCategories = latestDefinedCategories,
                            includePrivate =
                                    stateSnapshot.isPrivateSpaceUnlocked &&
                                            privateApps.isNotEmpty(),
                            includeWork = visible.any { isDrawerWorkProfileApp(context, it) },
                            includeAllAppsSection = !stateSnapshot.useSidebarCategoryDrawer
                    )
            val selectedCategory =
                    resolveSelectedCategory(
                            currentCategory = stateSnapshot.selectedCategory,
                            categories = categories,
                            skipAllAppsCategory = stateSnapshot.useSidebarCategoryDrawer
                    )
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = visible,
                            privateApps = privateApps,
                            rawSearchQuery = stateSnapshot.searchQuery,
                            category = selectedCategory
                    )
            _uiState.update { state ->
                state.copy(
                        allApps = visible,
                        privateSpaceApps = privateApps,
                        selectedCategory = selectedCategory,
                        categories = categories,
                        filteredProfileSections = filteredContent.filteredProfileSections,
                        filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps,
                        selectedApp =
                                state.selectedApp?.takeUnless {
                                    it.packageName == packageName &&
                                        appProfileKey(it.userHandle) == profileKey
                                }
                )
            }
            scheduleDrawerCachePrewarm()
        }
    }

    // --- Search ---

    private fun drawerSearchFilterActive(raw: String): Boolean {
        val trimmed = raw.trimStart()
        val q =
                if (DotSearchSyntax.isPossibleDotSearchPrefix(trimmed)) "" else trimmed.trim()
        return q.isNotBlank()
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryApplyJob?.cancel()
        searchQueryRequestId += 1
        val requestId = searchQueryRequestId
        val exitReorder = drawerSearchFilterActive(query)
        _uiState.update { state ->
            state.copy(
                    searchQuery = query,
                    drawerReorderSessionActive =
                            if (exitReorder) false else state.drawerReorderSessionActive
            )
        }
        searchQueryApplyJob =
                viewModelScope.launch {
                    val snapshot = _uiState.value
                    val trimmed = query.trimStart()
                    val filteredContent =
                            buildFilteredDrawerContent(
                                    allApps = snapshot.allApps,
                                    privateApps = snapshot.privateSpaceApps,
                                    rawSearchQuery = query,
                                    category = snapshot.selectedCategory
                            )
                    _uiState.update { state ->
                        if (requestId != searchQueryRequestId || state.searchQuery != query) {
                            state
                        } else {
                            state.copy(
                                    filteredProfileSections = filteredContent.filteredProfileSections,
                                    filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps
                            )
                        }
                    }

                    // Auto-launch when exactly one app matches across both lists.
                    // A leading space means "browse mode" – show the result but don't launch.
                    // Dot-prefixed queries are handled via IME / dot-search, not single-app auto-launch.
                    val browseMode = query.startsWith(" ")
                    if (requestId == searchQueryRequestId &&
                                    !browseMode &&
                                    !trimmed.startsWith(".") &&
                                    trimmed.isNotBlank() &&
                                    !_uiState.value.drawerReorderSessionActive
                    ) {
                        val mainFlat = filteredContent.filteredProfileSections.flatMap { it.apps }
                        val allMatches =
                                buildLaunchTargets(
                                        privateApps = filteredContent.filteredPrivateSpaceApps,
                                        mainApps = mainFlat
                                )
                        if (allMatches.size == 1) {
                            val target = allMatches[0]
                            if (launchTarget(target)) {
                                resetSearchState()
                                _events.emit(DrawerEvent.AutoLaunch(target))
                            }
                        }
                    }
                }
    }

    /**
     * Launches the first visible search hit in drawer list order (profile sections, then private
     * space), when the query is non-blank and not in leading-space browse mode. Used for IME
     * confirm when multiple apps match.
     */
    fun tryLaunchFirstSearchResult(): Boolean {
        val state = _uiState.value
        if (state.drawerReorderSessionActive) return false
        val raw = state.searchQuery
        if (raw.startsWith(" ")) return false
        val trimmed = raw.trimStart()
        if (trimmed.isBlank()) return false

        when (val parsed = DotSearchSyntax.parse(trimmed)) {
            is DotSearchParsed.Default -> {
                val pref = drawerDotSearchDefault
                if (appRepository.launchDotSearch(pref.profileKey, pref.target, parsed.searchText)) {
                    resetSearchState()
                    return true
                }
                return false
            }
            is DotSearchParsed.Alias -> {
                val pref = drawerDotSearchAliases[parsed.aliasChar] ?: return false
                if (appRepository.launchDotSearch(pref.profileKey, pref.target, parsed.searchText)) {
                    resetSearchState()
                    return true
                }
                return false
            }
            null -> {}
        }

        val firstMain = state.filteredProfileSections.flatMap { it.apps }.firstOrNull()
        if (firstMain != null) {
            return launchTarget(launchTargetFromAppInfo(firstMain))
        }
        val firstPrivate = state.filteredPrivateSpaceApps.firstOrNull() ?: return false
        val cn = firstPrivate.componentName ?: return false
        val uh = firstPrivate.userHandle ?: return false
        return launchTarget(
                LaunchTarget.PrivateApp(
                        packageName = firstPrivate.packageName,
                        componentName = cn,
                        userHandle = uh
                )
        )
    }

    private fun launchTargetFromAppInfo(app: AppInfo): LaunchTarget {
        val uh = app.userHandle
        val cn = app.componentName
        return if (uh != null && cn != null) {
            LaunchTarget.PrivateApp(
                    packageName = app.packageName,
                    componentName = cn,
                    userHandle = uh
            )
        } else {
            LaunchTarget.MainApp(app.packageName)
        }
    }

    fun onCategorySelected(category: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = state.allApps,
                            privateApps = state.privateSpaceApps,
                            rawSearchQuery = state.searchQuery,
                            category = category
                    )
            _uiState.update {
                it.copy(
                        selectedCategory = category,
                        filteredProfileSections = filteredContent.filteredProfileSections,
                        filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps
                )
            }
        }
    }

    fun onCategoryLongPress(category: String) {
        _uiState.update { it.copy(selectedCategoryForActions = category) }
    }

    fun dismissCategoryActionSheet() {
        _uiState.update { it.copy(selectedCategoryForActions = null) }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            appRepository.renameCategory(oldName, newName)
            preferencesManager.renameDrawerCategoryIcon(oldName, newName)
            dismissCategoryActionSheet()
        }
    }

    fun setCategoryDrawerIcon(category: String, iconName: String) {
        if (!MinimalIcons.all.containsKey(iconName)) return
        viewModelScope.launch { preferencesManager.setDrawerCategoryIcon(category, iconName) }
    }

    fun clearCategoryDrawerIcon(category: String) {
        viewModelScope.launch { preferencesManager.clearDrawerCategoryIcon(category) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            preferencesManager.clearDrawerCategoryIcon(name)
            appRepository.deleteCategory(name)
            if (_uiState.value.selectedCategory.equals(name, ignoreCase = true)) {
                val categoriesAfterDelete =
                        _uiState.value.categories.filterNot { it.equals(name, ignoreCase = true) }
                onCategorySelected(
                        defaultCategory(
                                categories = categoriesAfterDelete,
                                skipAllAppsCategory = _uiState.value.useSidebarCategoryDrawer
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
        val ok =
                when (target) {
                    is LaunchTarget.MainApp -> appRepository.launchApp(target.packageName)
                    is LaunchTarget.PrivateApp ->
                            privateSpaceManager.launchApp(target.componentName, target.userHandle)
                }
        if (ok) {
            viewModelScope.launch {
                when (target) {
                    is LaunchTarget.MainApp ->
                            preferencesManager.recordDrawerAppOpen(target.packageName, null)
                    is LaunchTarget.PrivateApp ->
                            preferencesManager.recordDrawerAppOpen(
                                    target.packageName,
                                    target.userHandle
                            )
                }
            }
        }
        return ok
    }

    // --- Long-press actions ---

    fun onAppLongPress(app: AppInfo) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun dismissActionSheet() {
        _uiState.update { it.copy(selectedApp = null) }
    }

    fun reorderDrawerProfileSectionApps(sectionId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            if (latestDrawerSortMode != DrawerAppSortMode.CUSTOM || !latestUseSidebarCategoryDrawer) {
                return@launch
            }
            val state = _uiState.value
            val trimmed = state.searchQuery.trimStart()
            val filterQuery =
                    if (DotSearchSyntax.isPossibleDotSearchPrefix(trimmed)) "" else trimmed.trim()
            if (filterQuery.isNotBlank() || !state.drawerReorderSessionActive) return@launch
            val section = state.filteredProfileSections.find { it.id == sectionId } ?: return@launch
            val visible = section.apps
            if (fromIndex !in visible.indices ||
                            toIndex !in visible.indices ||
                            fromIndex == toIndex
            ) {
                return@launch
            }
            val profileKey = appProfileKey(visible.first().userHandle)
            val merged =
                    mergeCustomOrderMaps(state.allApps, state.privateSpaceApps, latestCustomOrderByProfile)
            val profileFullOrder = merged[profileKey] ?: return@launch
            val subsetKeys =
                    visible.map { drawerOpenCountKey(it.packageName, it.userHandle) }
            val newProfileOrder =
                    reorderSubsetInFullOrder(profileFullOrder, subsetKeys, fromIndex, toIndex)
            val newMap = merged.toMutableMap()
            newMap[profileKey] = newProfileOrder
            latestCustomOrderByProfile = newMap
            preferencesManager.setDrawerCustomAppOrder(newMap)
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = state.allApps,
                            privateApps = state.privateSpaceApps,
                            rawSearchQuery = state.searchQuery,
                            category = state.selectedCategory
                    )
            _uiState.update {
                it.copy(
                        filteredProfileSections = filteredContent.filteredProfileSections,
                        filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps
                )
            }
        }
    }

    fun reorderPrivateDrawerApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            if (latestDrawerSortMode != DrawerAppSortMode.CUSTOM || !latestUseSidebarCategoryDrawer) {
                return@launch
            }
            val state = _uiState.value
            val trimmed = state.searchQuery.trimStart()
            val filterQuery =
                    if (DotSearchSyntax.isPossibleDotSearchPrefix(trimmed)) "" else trimmed.trim()
            if (filterQuery.isNotBlank() || !state.drawerReorderSessionActive) return@launch
            val visible = state.filteredPrivateSpaceApps
            if (fromIndex !in visible.indices ||
                            toIndex !in visible.indices ||
                            fromIndex == toIndex ||
                            visible.isEmpty()
            ) {
                return@launch
            }
            val profileKey = appProfileKey(visible.first().userHandle)
            val merged =
                    mergeCustomOrderMaps(state.allApps, state.privateSpaceApps, latestCustomOrderByProfile)
            val profileFullOrder = merged[profileKey] ?: return@launch
            val subsetKeys =
                    visible.map { drawerOpenCountKey(it.packageName, it.userHandle) }
            val newProfileOrder =
                    reorderSubsetInFullOrder(profileFullOrder, subsetKeys, fromIndex, toIndex)
            val newMap = merged.toMutableMap()
            newMap[profileKey] = newProfileOrder
            latestCustomOrderByProfile = newMap
            preferencesManager.setDrawerCustomAppOrder(newMap)
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = state.allApps,
                            privateApps = state.privateSpaceApps,
                            rawSearchQuery = state.searchQuery,
                            category = state.selectedCategory
                    )
            _uiState.update {
                it.copy(
                        filteredProfileSections = filteredContent.filteredProfileSections,
                        filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps
                )
            }
        }
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

    fun toggleDrawerReorderSession() {
        _uiState.update { state ->
            val canReorder =
                    state.useSidebarCategoryDrawer &&
                            state.drawerAppSortMode == DrawerAppSortMode.CUSTOM &&
                            !drawerSearchFilterActive(state.searchQuery)
            if (!canReorder) {
                state.copy(showMenu = false)
            } else {
                state.copy(
                        drawerReorderSessionActive = !state.drawerReorderSessionActive,
                        showMenu = false
                )
            }
        }
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
        viewModelScope.launch {
            val supported = privateSpaceManager.isSupported
            val unlocked = privateSpaceManager.isPrivateSpaceUnlocked()
            val apps =
                    if (unlocked) {
                        sortPrivateSpaceAppsCachedSuspend(
                                privateSpaceManager.getPrivateSpaceApps()
                        )
                    } else {
                        emptyList()
                    }
            val state = _uiState.value
            val categories =
                    deriveCategories(
                            apps = state.allApps,
                            definedCategories = latestDefinedCategories,
                            includePrivate = unlocked && apps.isNotEmpty(),
                            includeWork = state.allApps.any { isDrawerWorkProfileApp(context, it) },
                            includeAllAppsSection = !state.useSidebarCategoryDrawer
                    )
            val selectedCategory =
                    resolveSelectedCategory(
                            currentCategory = state.selectedCategory,
                            categories = categories,
                            skipAllAppsCategory = state.useSidebarCategoryDrawer
                    )
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = state.allApps,
                            privateApps = apps,
                            rawSearchQuery = state.searchQuery,
                            category = selectedCategory
                    )
            _uiState.update {
                it.copy(
                        isPrivateSpaceSupported = supported,
                        isPrivateSpaceUnlocked = unlocked,
                        privateSpaceApps = apps,
                        selectedCategory = selectedCategory,
                        filteredProfileSections = filteredContent.filteredProfileSections,
                        filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps,
                        categories = categories
                )
            }
            scheduleDrawerCachePrewarm()
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

    /**
     * Forces a reload of install state on next access. [observeInstalledApps] performs a single
     * [rebuildVisibleApps] when the cache version bumps — avoids doubling work from also launching
     * a rebuild here.
     */
    fun refresh() {
        appRepository.invalidateCache()
    }

    fun resetSearchState() {
        viewModelScope.launch {
            val state = _uiState.value
            val defaultCategory =
                    defaultCategory(
                            categories = state.categories,
                            skipAllAppsCategory = state.useSidebarCategoryDrawer
                    )
            val filteredContent =
                    buildFilteredDrawerContent(
                            allApps = state.allApps,
                            privateApps = state.privateSpaceApps,
                            rawSearchQuery = "",
                            category = defaultCategory
                    )
            _uiState.update {
                it.copy(
                        searchQuery = "",
                        selectedCategory = defaultCategory,
                        filteredProfileSections = filteredContent.filteredProfileSections,
                        filteredPrivateSpaceApps = filteredContent.filteredPrivateSpaceApps,
                        drawerReorderSessionActive = false
                )
            }
        }
    }

    /**
     * Clears search/category to defaults only when needed (e.g. drawer was dismissed without
     * [resetSearchState], such as go-home). Skips redundant main-thread section rebuild when
     * already showing the default drawer.
     */
    fun resetSearchStateIfNeeded() {
        val state = _uiState.value
        val expectedDefault =
                defaultCategory(
                        categories = state.categories,
                        skipAllAppsCategory = state.useSidebarCategoryDrawer
                )
        if (state.searchQuery.isBlank() &&
                        state.selectedCategory.equals(expectedDefault, ignoreCase = true)
        ) {
            return
        }
        resetSearchState()
    }

    // --- Filtering ---

    private fun sortDrawerAppsWith(
            apps: List<AppInfo>,
            mode: DrawerAppSortMode,
            counts: Map<String, Int>
    ): List<AppInfo> {
        if (apps.isEmpty()) return apps
        return when (mode) {
            DrawerAppSortMode.MOST_OPENED -> {
                if (counts.values.none { it > 0 }) {
                    apps.sortedWith(alphabeticalAppComparatorForProfiles)
                } else {
                    apps.sortedWith(
                            compareByDescending<AppInfo> { app ->
                                counts[drawerOpenCountKey(app.packageName, app.userHandle)] ?: 0
                            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                    )
                }
            }
            DrawerAppSortMode.CUSTOM -> {
                if (!latestUseSidebarCategoryDrawer) {
                    apps.sortedWith(alphabeticalAppComparatorForProfiles)
                } else {
                    val profileKey = appProfileKey(apps.first().userHandle)
                    val order = latestCustomOrderByProfile[profileKey].orEmpty()
                    val index = order.withIndex().associate { it.value to it.index }
                    apps.sortedWith(
                            compareBy<AppInfo> { app ->
                                val k = drawerOpenCountKey(app.packageName, app.userHandle)
                                index[k] ?: Int.MAX_VALUE
                            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                    )
                }
            }
            DrawerAppSortMode.ALPHABETICAL ->
                    apps.sortedWith(alphabeticalAppComparatorForProfiles)
        }
    }

    private fun fingerprintPrivateApps(apps: List<AppInfo>): Int {
        var h = apps.size
        for (a in apps) {
            h = 31 * h + a.packageName.hashCode()
            h = 31 * h + (a.userHandle?.hashCode() ?: 0)
        }
        return h
    }

    /**
     * Sorts private-space apps with the same rules as the main drawer, reusing the result when the
     * underlying app set and sort inputs are unchanged (the platform often returns a fresh list
     * instance with identical contents).
     */
    private suspend fun sortPrivateSpaceAppsCachedSuspend(raw: List<AppInfo>): List<AppInfo> {
        if (raw.isEmpty()) return raw
        val sortMode = latestDrawerSortMode
        val counts = latestOpenCounts
        if (sortMode == DrawerAppSortMode.CUSTOM) {
            return withContext(drawerComputationDispatcher) {
                sortDrawerAppsWith(raw, sortMode, counts)
            }
        }
        val fp = fingerprintPrivateApps(raw)
        synchronized(privateSortCacheLock) {
            if (privateSortCacheResult != null &&
                            fp == privateSortCacheFingerprint &&
                            privateSortCacheSortMode == sortMode &&
                            privateSortCacheCounts === counts
            ) {
                return privateSortCacheResult!!
            }
        }
        val sorted =
                withContext(drawerComputationDispatcher) {
                    sortDrawerAppsWith(raw, sortMode, counts)
                }
        synchronized(privateSortCacheLock) {
            privateSortCacheFingerprint = fp
            privateSortCacheSortMode = sortMode
            privateSortCacheCounts = counts
            privateSortCacheResult = sorted
        }
        return sorted
    }

    /**
     * Groups and sorts apps by profile on a worker thread; updates the profile-section cache.
     */
    private suspend fun buildProfileSectionsSuspend(apps: List<AppInfo>): List<DrawerProfileSectionUi> {
        val sortMode = latestDrawerSortMode
        val counts = latestOpenCounts
        synchronized(profileSectionCacheLock) {
            if (profileSectionsCache != null &&
                            profileSectionsCacheApps === apps &&
                            profileSectionsCacheSortMode == sortMode &&
                            profileSectionsCacheCounts === counts &&
                            profileSectionsCacheCustomOrder ==
                                    if (sortMode == DrawerAppSortMode.CUSTOM) {
                                        latestCustomOrderByProfile
                                    } else {
                                        null
                                    }
            ) {
                return profileSectionsCache!!
            }
        }
        val built =
                withContext(drawerComputationDispatcher) {
                    groupAppsIntoProfileSections(context, apps) { list ->
                        sortDrawerAppsWith(list, sortMode, counts)
                    }
                }
        synchronized(profileSectionCacheLock) {
            profileSectionsCache = built
            profileSectionsCacheApps = apps
            profileSectionsCacheSortMode = sortMode
            profileSectionsCacheCounts = counts
            profileSectionsCacheCustomOrder =
                    if (sortMode == DrawerAppSortMode.CUSTOM) latestCustomOrderByProfile else null
        }
        return built
    }

    private fun filterProfileSections(
            sections: List<DrawerProfileSectionUi>,
            query: String,
            category: String
    ): List<DrawerProfileSectionUi> {
        if (category.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) {
            return sections.map { it.copy(apps = emptyList()) }
        }
        if (query.isBlank() &&
                        category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)
        ) {
            return sections
        }
        return sections.map { section ->
            var apps = section.apps
            if (query.isNotBlank()) {
                apps = apps.filter { it.label.containsNormalizedSearch(query) }
            }
            if (category.isNotBlank() && !category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) {
                apps =
                        if (category.equals(ReservedCategoryNames.WORK, ignoreCase = true)) {
                            apps.filter { isDrawerWorkProfileApp(context, it) }
                        } else if (category.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)
                        ) {
                            apps.filter { it.category.isBlank() }
                        } else {
                            apps.filter { it.category.equals(category, ignoreCase = true) }
                        }
            }
            section.copy(apps = apps)
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
            privateApps.filter { it.label.containsNormalizedSearch(query) }
        }
    }

    private fun resolveSelectedCategory(
            currentCategory: String,
            categories: List<String>,
            skipAllAppsCategory: Boolean
    ): String {
        return if (categories.any { it.equals(currentCategory, ignoreCase = true) }) {
            currentCategory
        } else {
            defaultCategory(categories, skipAllAppsCategory)
        }
    }

    private fun defaultCategory(categories: List<String>, skipAllAppsCategory: Boolean): String {
        if (!skipAllAppsCategory) return ReservedCategoryNames.ALL_APPS
        return categories.firstOrNull() ?: ReservedCategoryNames.ALL_APPS
    }

    private fun deriveCategories(
            apps: List<AppInfo>,
            definedCategories: List<String>,
            includePrivate: Boolean,
            includeWork: Boolean,
            includeAllAppsSection: Boolean
    ): List<String> {
        val privateSpaceLast = !includeAllAppsSection
        val hasUncategorizedApps = apps.any { it.category.isBlank() }
        val dynamic = apps.map { it.category.trim() }.filter { it.isNotBlank() }.toSet()
        val orderedDefined = definedCategories.distinct()
        val extras = (dynamic - orderedDefined.toSet()).toList().sorted()
        val reservedLower =
                buildSet {
                    if (includeAllAppsSection) add(ReservedCategoryNames.ALL_APPS.lowercase())
                    if (includePrivate) add(ReservedCategoryNames.PRIVATE.lowercase())
                    if (includeWork) add(ReservedCategoryNames.WORK.lowercase())
                }
        val definedFiltered =
                orderedDefined.filterNot { it.lowercase() in reservedLower }
        val extrasFiltered =
                extras.filterNot {
                    it.lowercase() in reservedLower ||
                            it.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)
                }
        val definedSansSyntheticUncategorized =
                definedFiltered.filterNot {
                    it.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)
                }
        return buildList {
            if (includeAllAppsSection) add(ReservedCategoryNames.ALL_APPS)
            if (privateSpaceLast) {
                if (includeWork) add(ReservedCategoryNames.WORK)
            } else {
                if (includePrivate) add(ReservedCategoryNames.PRIVATE)
                if (includeWork) add(ReservedCategoryNames.WORK)
            }
            addAll(definedSansSyntheticUncategorized)
            addAll(extrasFiltered)
            if (hasUncategorizedApps) add(ReservedCategoryNames.UNCATEGORIZED)
            if (privateSpaceLast && includePrivate) add(ReservedCategoryNames.PRIVATE)
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
        val mainTargets = mainApps.map { launchTargetFromAppInfo(it) }
        return privateTargets + mainTargets
    }

    private suspend fun buildFilteredDrawerContent(
            allApps: List<AppInfo>,
            privateApps: List<AppInfo>,
            rawSearchQuery: String,
            category: String
    ): FilteredDrawerContent {
        if (latestDrawerSortMode == DrawerAppSortMode.CUSTOM && _uiState.value.useSidebarCategoryDrawer) {
            val merged = mergeCustomOrderMaps(allApps, privateApps, latestCustomOrderByProfile)
            if (merged != latestCustomOrderByProfile) {
                latestCustomOrderByProfile = merged
                preferencesManager.setDrawerCustomAppOrder(merged)
            }
        }
        val sections = buildProfileSectionsSuspend(allApps)
        val trimmed = rawSearchQuery.trimStart()
        val filterQuery =
                if (DotSearchSyntax.isPossibleDotSearchPrefix(trimmed)) "" else trimmed
        return withContext(drawerComputationDispatcher) {
            FilteredDrawerContent(
                    filteredProfileSections =
                            filterProfileSections(
                                    sections = sections,
                                    query = filterQuery,
                                    category = category
                            ),
                    filteredPrivateSpaceApps =
                            applyPrivateFilter(
                                    query = filterQuery,
                                    selectedCategory = category,
                                    privateApps = privateApps
                            )
            )
        }
    }

    private fun mergeCustomOrderMaps(
            mainApps: List<AppInfo>,
            privateApps: List<AppInfo>,
            stored: Map<String, List<String>>
    ): Map<String, List<String>> {
        val result = stored.toMutableMap()
        val byProfile = mainApps.groupBy { appProfileKey(it.userHandle) }
        for ((pk, list) in byProfile) {
            result[pk] = mergeOrderListForProfile(pk, list, result)
        }
        if (privateApps.isNotEmpty()) {
            val pk = appProfileKey(privateApps.first().userHandle)
            result[pk] = mergeOrderListForProfile(pk, privateApps, result)
        }
        return result
    }

    private fun mergeOrderListForProfile(
            profileKey: String,
            appsInProfile: List<AppInfo>,
            map: Map<String, List<String>>
    ): List<String> {
        val orderedKeys =
                appsInProfile.map { drawerOpenCountKey(it.packageName, it.userHandle) }
        val set = orderedKeys.toSet()
        val prev = map[profileKey].orEmpty().filter { it in set }
        val missing = set - prev.toSet()
        val tail =
                appsInProfile
                        .filter { drawerOpenCountKey(it.packageName, it.userHandle) in missing }
                        .sortedWith(alphabeticalAppComparatorForProfiles)
                        .map { drawerOpenCountKey(it.packageName, it.userHandle) }
        return prev + tail
    }

    private fun reorderSubsetInFullOrder(
            fullOrder: List<String>,
            visibleOrderedKeys: List<String>,
            fromIndex: Int,
            toIndex: Int,
    ): List<String> {
        if (fromIndex == toIndex) return fullOrder
        if (fromIndex !in visibleOrderedKeys.indices || toIndex !in visibleOrderedKeys.indices) {
            return fullOrder
        }
        val subsetSet = visibleOrderedKeys.toSet()
        val reordered = visibleOrderedKeys.toMutableList()
        val item = reordered.removeAt(fromIndex)
        reordered.add(toIndex, item)
        val queue = ArrayDeque(reordered)
        return fullOrder.map { key -> if (key in subsetSet) queue.removeFirst() else key }
    }

    private data class CombinedCategoryState(
            val hiddenSet: Set<String>,
            val renameMap: Map<String, String>,
            val categoryMap: Map<String, String>,
            val definedCategories: List<String>
    )
}
