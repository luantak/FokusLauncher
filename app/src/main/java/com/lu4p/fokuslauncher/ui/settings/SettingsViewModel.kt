package com.lu4p.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.font.SystemFontFamiliesProvider
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.util.AppLocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
        val hiddenApps: List<HiddenAppInfo> = emptyList(),
        val renamedApps: List<RenamedAppEntity> = emptyList(),
        val appCategories: Map<String, String> = emptyMap(),
        val categoryDefinitions: List<String> = emptyList(),
        val favorites: List<FavoriteApp> = emptyList(),
        val rightSideShortcuts: List<HomeShortcut> = emptyList(),
        val swipeLeftTarget: ShortcutTarget? = null,
        val swipeRightTarget: ShortcutTarget? = null,
        val preferredWeatherAppPackage: String = "",
        val showStatusBar: Boolean = false,
        val showHomeScreenWidgets: Boolean = true,
        val autoOpenDrawerKeyboard: Boolean = true,
        val hideAllAppsSection: Boolean = false,
        val drawerAppSortMode: DrawerAppSortMode = DrawerAppSortMode.ALPHABETICAL,
        val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
        val launcherFontFamilyName: String = "",
        /** BCP-47 tag; empty = system default. */
        val appLocaleTag: String = "",
        val allowLandscapeRotation: Boolean = false,
        val doubleTapEmptyLock: Boolean = false,
        val allApps: List<AppInfo> = emptyList()
)

data class HiddenAppInfo(val packageName: String, val label: String)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _installedFontFamilies = MutableStateFlow<List<String>>(emptyList())
    val installedFontFamilies: StateFlow<List<String>> = _installedFontFamilies.asStateFlow()

    init {
        observeState()
        viewModelScope.launch(Dispatchers.IO) {
            _installedFontFamilies.value = SystemFontFamiliesProvider.loadSortedDistinct()
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            val favoritesQuintupleFlow =
                    combine(
                            appRepository.getHiddenPackageNames(),
                            appRepository.getAllRenamedApps(),
                            preferencesManager.favoritesFlow,
                            preferencesManager.rightSideShortcutsFlow,
                            preferencesManager.swipeLeftTargetFlow
                    ) { hiddenNames, renamedApps, favorites, rightSideShortcuts, swipeLeft ->
                        Quintuple(
                                hiddenNames = hiddenNames,
                                renamedApps = renamedApps,
                                favorites = favorites,
                                rightSideShortcuts = rightSideShortcuts,
                                swipeLeft = swipeLeft
                        )
                    }
            combine(appRepository.getInstalledAppsVersion(), favoritesQuintupleFlow) { _, base ->
                base
            }
                    .combine(appRepository.getAllAppCategories()) { leftState, appCategories ->
                        leftState to appCategories.associate { it.packageName to it.category }
                    }
                    .combine(appRepository.getAllCategoryDefinitions()) { stateWithCategories, definitions ->
                        val (leftState, appCategories) = stateWithCategories
                        CategoryState(
                                base = leftState,
                                appCategories = appCategories,
                                categoryDefinitions = definitions.map { it.name }
                        )
                    }
                    .combine(preferencesManager.swipeRightTargetFlow) { leftState, swipeRight ->
                        leftState to swipeRight
                    }
                    .combine(preferencesManager.preferredWeatherAppFlow) { swipeState, preferredWeatherApp ->
                        Pair(swipeState, preferredWeatherApp)
                    }
                    .combine(preferencesManager.showStatusBarFlow) { weatherState, showStatusBar ->
                        weatherState to showStatusBar
                    }
                    .combine(preferencesManager.showHomeScreenWidgetsFlow) {
                        weatherWithStatusBar, showHomeScreenWidgets ->
                        weatherWithStatusBar to showHomeScreenWidgets
                    }
                    .combine(preferencesManager.autoOpenDrawerKeyboardFlow) {
                        weatherWithWidgets, autoOpenDrawerKeyboard ->
                        weatherWithWidgets to autoOpenDrawerKeyboard
                    }
                    .combine(preferencesManager.hideAllAppsSectionFlow) {
                        weatherWithSettingsState, hideAllAppsSection ->
                        weatherWithSettingsState to hideAllAppsSection
                    }
                    .combine(preferencesManager.drawerAppSortModeFlow) { hidePair, drawerAppSortMode ->
                        Triple(hidePair.first, hidePair.second, drawerAppSortMode)
                    }
                    .combine(preferencesManager.launcherFontFamilyFlow) { triple, fontFamilyName ->
                        Pair(
                                Triple(triple.first, triple.second, triple.third),
                                fontFamilyName
                        )
                    }
                    .combine(preferencesManager.appLocaleTagFlow) { pair, appLocaleTag ->
                        Pair(pair.first, Pair(pair.second, appLocaleTag))
                    }
                    .combine(preferencesManager.homeAlignmentFlow) { nested, homeAlignment ->
                        Pair(nested, homeAlignment)
                    }
                    .combine(preferencesManager.allowLandscapeRotationFlow) {
                            nestedAndHome,
                            allowLandscapeRotation ->
                        nestedAndHome to allowLandscapeRotation
                    }
                    .combine(preferencesManager.doubleTapEmptyLockFlow) { nestedAndRotation, doubleTapEmptyLock ->
                        Triple(nestedAndRotation.first, nestedAndRotation.second, doubleTapEmptyLock)
                    }
                    .collectLatest { (nestedAndHome, allowLandscapeRotation, doubleTapEmptyLock) ->
                        val (nested, homeAlignment) = nestedAndHome
                        val (sortTriple, fontAndLocale) = nested
                        val (fontFamilyName, appLocaleTag) = fontAndLocale
                        val (weatherWithSettingsState, hideAllAppsSection, drawerAppSortMode) =
                                sortTriple
                        val (weatherWithWidgets, autoOpenDrawerKeyboard) = weatherWithSettingsState
                        val (weatherWithStatusBar, showHomeScreenWidgets) = weatherWithWidgets
                        val (weatherState, showStatusBar) = weatherWithStatusBar
                        val (swipeState, preferredWeatherApp) = weatherState
                        val (leftState, swipeRight) = swipeState
                        val categoryMap = leftState.appCategories
                        val allApps =
                                appRepository.getInstalledAppsOnBackground().map { app ->
                                    app.copy(category = categoryMap[app.packageName] ?: app.category)
                                }
                        val hiddenLabels = allApps.associate { it.packageName to it.label }
                        val hiddenInfos =
                                leftState.base.hiddenNames.map { pkg ->
                                    HiddenAppInfo(packageName = pkg, label = hiddenLabels[pkg] ?: pkg)
                                }
                        _uiState.value =
                                SettingsUiState(
                                        hiddenApps = hiddenInfos,
                                        renamedApps = leftState.base.renamedApps,
                                        appCategories = leftState.appCategories,
                                        categoryDefinitions = leftState.categoryDefinitions,
                                        favorites = leftState.base.favorites,
                                        rightSideShortcuts = leftState.base.rightSideShortcuts,
                                        swipeLeftTarget = leftState.base.swipeLeft,
                                        swipeRightTarget = swipeRight,
                                        preferredWeatherAppPackage = preferredWeatherApp,
                                        showStatusBar = showStatusBar,
                                        showHomeScreenWidgets = showHomeScreenWidgets,
                                        autoOpenDrawerKeyboard = autoOpenDrawerKeyboard,
                                        hideAllAppsSection = hideAllAppsSection,
                                        drawerAppSortMode = drawerAppSortMode,
                                        homeAlignment = homeAlignment,
                                        launcherFontFamilyName = fontFamilyName,
                                        appLocaleTag = appLocaleTag,
                                        allowLandscapeRotation = allowLandscapeRotation,
                                        doubleTapEmptyLock = doubleTapEmptyLock,
                                        allApps = allApps
                                )
                    }
        }
    }

    private data class Quintuple(
            val hiddenNames: List<String>,
            val renamedApps: List<RenamedAppEntity>,
            val favorites: List<FavoriteApp>,
            val rightSideShortcuts: List<HomeShortcut>,
            val swipeLeft: ShortcutTarget?
    )

    private data class CategoryState(
            val base: Quintuple,
            val appCategories: Map<String, String>,
            val categoryDefinitions: List<String>
    )

    // --- Hidden Apps ---

    fun unhideApp(packageName: String) {
        viewModelScope.launch { appRepository.unhideApp(packageName) }
    }

    // --- Renamed Apps ---

    fun removeRename(packageName: String) {
        viewModelScope.launch { appRepository.removeRename(packageName) }
    }

    fun addCategoryDefinition(name: String) {
        viewModelScope.launch { appRepository.addCategoryDefinition(name) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch { appRepository.deleteCategory(name) }
    }

    fun setAppCategory(packageName: String, category: String) {
        viewModelScope.launch { appRepository.setAppCategory(packageName, category) }
    }

    fun reorderCategories(categories: List<String>) {
        viewModelScope.launch { appRepository.reorderCategoryDefinitions(categories) }
    }

    // --- Swipe gestures ---

    fun setSwipeLeftTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeLeftTarget(target) }
    }

    fun setSwipeRightTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeRightTarget(target) }
    }

    fun setPreferredWeatherApp(packageName: String) {
        viewModelScope.launch { preferencesManager.setPreferredWeatherApp(packageName) }
    }

    fun setShowStatusBar(show: Boolean) {
        viewModelScope.launch { preferencesManager.setShowStatusBar(show) }
    }

    fun setAllowLandscapeRotation(allow: Boolean) {
        viewModelScope.launch { preferencesManager.setAllowLandscapeRotation(allow) }
    }

    fun setDoubleTapEmptyLock(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDoubleTapEmptyLock(enabled) }
    }

    fun setShowHomeScreenWidgets(show: Boolean) {
        viewModelScope.launch { preferencesManager.setShowHomeScreenWidgets(show) }
    }

    fun setAutoOpenDrawerKeyboard(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoOpenDrawerKeyboard(enabled) }
    }

    fun setHideAllAppsSection(hide: Boolean) {
        viewModelScope.launch { preferencesManager.setHideAllAppsSection(hide) }
    }

    fun setDrawerAppSortMode(mode: DrawerAppSortMode) {
        viewModelScope.launch { preferencesManager.setDrawerAppSortMode(mode) }
    }

    // --- Home alignment ---

    fun setHomeAlignment(alignment: HomeAlignment) {
        viewModelScope.launch { preferencesManager.setHomeAlignment(alignment) }
    }

    fun setLauncherFontFamilyName(familyName: String) {
        viewModelScope.launch { preferencesManager.setLauncherFontFamilyName(familyName) }
    }

    fun setAppLocaleTag(tag: String) {
        viewModelScope.launch {
            preferencesManager.setAppLocaleTag(tag)
            AppLocaleHelper.applyLocaleTag(tag)
            appRepository.invalidateCache()
        }
    }

    /** Clears all app state (preferences + database), equivalent to clearing storage. */
    suspend fun resetAllState() {
        preferencesManager.clearAll()
        appRepository.clearAllAppData()
        appRepository.invalidateCache()
    }

    fun setSystemWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    wallpaperManager.setStream(stream)
                }
            } catch (e: Exception) {
                // Ignore or handle error
            }
        }
    }

    fun setBlackWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create a full-screen black bitmap
                val width = context.resources.displayMetrics.widthPixels
                val height = context.resources.displayMetrics.heightPixels
                val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.BLACK)

                val wallpaperManager = WallpaperManager.getInstance(context)
                // Set for home screen
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                // Try to set for lock screen too
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } catch (_: Exception) {
                    // Lock screen wallpaper may fail on some devices, ignore
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
