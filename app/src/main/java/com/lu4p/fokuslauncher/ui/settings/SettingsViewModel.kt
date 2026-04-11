package com.lu4p.fokuslauncher.ui.settings

import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AddCategoryResult
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeDateFormatStyle
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.appMetadataKey
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.font.SystemFontFamiliesProvider
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.util.AppLocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import android.app.WallpaperManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.drawer.isDrawerWorkProfileApp
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForApp
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel
import com.lu4p.fokuslauncher.ui.util.stateWhileSubscribedIn
import com.lu4p.fokuslauncher.utils.AppDiagnosticLogExporter
import com.lu4p.fokuslauncher.utils.LockScreenHelper
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.WallpaperHelper

data class SettingsUiState(
        val hiddenApps: List<HiddenAppInfo> = emptyList(),
        val renamedApps: List<RenamedAppInfo> = emptyList(),
        val appCategories: Map<String, String> = emptyMap(),
        val categoryDefinitions: List<String> = emptyList(),
        val favorites: List<FavoriteApp> = emptyList(),
        val rightSideShortcuts: List<HomeShortcut> = emptyList(),
        val swipeLeftTarget: ShortcutTarget? = null,
        val swipeRightTarget: ShortcutTarget? = null,
        val preferredWeatherAppPackage: String = "",
        val preferredClockAppPackage: String = "",
        val preferredCalendarAppPackage: String = "",
        val showStatusBar: Boolean = false,
        val showHomeClock: Boolean = true,
        val showHomeDate: Boolean = true,
        val showHomeWeather: Boolean = true,
        val showHomeBattery: Boolean = true,
        val homeDateFormatStyle: HomeDateFormatStyle = HomeDateFormatStyle.SYSTEM_DEFAULT,
        /** Vertical category sidebar in the app drawer. */
        val drawerSidebarCategories: Boolean = false,
        /** Launch the app when search narrows to a single match (drawer search). */
        val drawerSearchAutoLaunch: Boolean = true,
        /** When true, category rail is on the left; default false places it on the right. */
        val drawerCategorySidebarOnLeft: Boolean = false,
        /** Normalized category key → [MinimalIcons] name for the drawer sidebar rail. */
        val categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        val drawerAppSortMode: DrawerAppSortMode = DrawerAppSortMode.ALPHABETICAL,
        val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
        val launcherFontFamilyName: String = "",
        val launcherFontScale: Float = LauncherFontScale.DEFAULT,
        val launcherVisualStyle: LauncherVisualStyle = LauncherVisualStyle.CLASSIC,
        /** Text shadow + icon halo; independent of [launcherVisualStyle]. */
        val launcherGlowEnabled: Boolean = false,
        /** BCP-47 tag; empty = system default. */
        val appLocaleTag: String = "",
        val allowLandscapeRotation: Boolean = false,
        val doubleTapEmptyLock: Boolean = false,
        val longLockReturnHome: Boolean = false,
        val longLockReturnHomeThresholdMinutes: Int =
                PreferencesManager.DEFAULT_LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES,
        val allApps: List<AppInfo> = emptyList(),
        val allShortcutActions: List<AppShortcutAction> = emptyList(),
)

data class HiddenAppInfo(
        val packageName: String,
        val profileKey: String,
        val label: String,
        val profileLabel: String?
)

data class RenamedAppInfo(
        val packageName: String,
        val profileKey: String,
        val customName: String,
        val profileLabel: String?
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val preferencesManager: PreferencesManager,
        private val privateSpaceManager: PrivateSpaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val accessibilityProminentDisclosureAccepted =
            preferencesManager.accessibilityProminentDisclosureAcceptedFlow.stateWhileSubscribedIn(
                    viewModelScope,
                    false,
            )

    private val _addCategoryResults = MutableSharedFlow<AddCategoryResult>(extraBufferCapacity = 1)
    val addCategoryResults: SharedFlow<AddCategoryResult> = _addCategoryResults.asSharedFlow()

    private val _installedFontFamilies = MutableStateFlow<List<String>>(emptyList())
    val installedFontFamilies: StateFlow<List<String>> = _installedFontFamilies.asStateFlow()
    private val privateSpaceRefreshTick = MutableStateFlow(0)

    init {
        observeState()
        viewModelScope.launch {
            privateSpaceManager.profileStateChanged.collect {
                privateSpaceRefreshTick.value += 1
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _installedFontFamilies.value = SystemFontFamiliesProvider.loadSortedDistinct()
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            val favoritesBaseFlow =
                    combine(
                            appRepository.getHiddenApps(),
                            appRepository.getAllRenamedApps(),
                            preferencesManager.favoritesFlow,
                            preferencesManager.rightSideShortcutsFlow,
                            preferencesManager.swipeLeftTargetFlow,
                    ) { hiddenApps, renamedApps, favorites, rightSideShortcuts, swipeLeft ->
                        FavoritesBase(
                                hiddenApps = hiddenApps,
                                renamedApps = renamedApps,
                                favorites = favorites,
                                rightSideShortcuts = rightSideShortcuts,
                                swipeLeft = swipeLeft,
                        )
                    }
            val categoryStateFlow =
                    combine(
                            appRepository.getInstalledAppsVersion(),
                            favoritesBaseFlow,
                            privateSpaceRefreshTick,
                            appRepository.getAllAppCategories(),
                            appRepository.getAllCategoryDefinitions(),
                    ) { _, base, _, categories, definitions ->
                        CategoryState(
                                hiddenApps = base.hiddenApps,
                                renamedApps = base.renamedApps,
                                favorites = base.favorites,
                                rightSideShortcuts = base.rightSideShortcuts,
                                swipeLeft = base.swipeLeft,
                                appCategories =
                                        categories.associate {
                                            appMetadataKey(it.packageName, it.profileKey) to it.category
                                        },
                                categoryDefinitions = definitions.map { it.name },
                        )
                    }
            val homeWidgetItemsFlow =
                    combine(
                            preferencesManager.homeWidgetVisibilityFlow,
                            combine(
                                    preferencesManager.preferredClockAppFlow,
                                    preferencesManager.preferredCalendarAppFlow,
                                    preferencesManager.homeDateFormatStyleFlow,
                            ) { clk, cal, fmt -> Triple(clk, cal, fmt) },
                    ) { vis, p ->
                        HomeWidgetItemSettings(
                                showClock = vis.showClock,
                                showDate = vis.showDate,
                                showWeather = vis.showWeather,
                                showBattery = vis.showBattery,
                                preferredClockAppPackage = p.first,
                                preferredCalendarAppPackage = p.second,
                                homeDateFormatStyle = p.third,
                        )
                    }
            val drawerPrefsFlow =
                    combine(
                            combine(
                                    preferencesManager.swipeRightTargetFlow,
                                    preferencesManager.preferredWeatherAppFlow,
                                    preferencesManager.showStatusBarFlow,
                            ) { swipeRight, weatherPkg, showStatusBar ->
                                Triple(swipeRight, weatherPkg, showStatusBar)
                            },
                            combine(
                                    preferencesManager.drawerSidebarCategoriesFlow,
                                    preferencesManager.drawerAppSortModeFlow,
                                    preferencesManager.drawerSearchAutoLaunchFlow,
                            ) { sidebarCategories, sortMode, searchAutoLaunch ->
                                Triple(sidebarCategories, sortMode, searchAutoLaunch)
                            },
                    ) { swipeAndWeather, drawerLayout ->
                        DrawerPrefs(
                                swipeRightTarget = swipeAndWeather.first,
                                preferredWeatherAppPackage = swipeAndWeather.second,
                                showStatusBar = swipeAndWeather.third,
                                drawerSidebarCategories = drawerLayout.first,
                                drawerAppSortMode = drawerLayout.second,
                                drawerSearchAutoLaunch = drawerLayout.third,
                        )
                    }
            val fontVisualFlow =
                    combine(
                            preferencesManager.launcherFontFamilyFlow,
                            preferencesManager.launcherFontScaleFlow,
                            preferencesManager.launcherAppearanceFlow,
                    ) { font, fontScale, appearance ->
                        FontVisualPrefs(
                                family = font,
                                scale = fontScale,
                                visualStyle = appearance.visualStyle,
                                glowEnabled = appearance.glowEnabled,
                        )
                    }
            val lookPrefsFlow =
                    combine(
                            fontVisualFlow,
                            preferencesManager.appLocaleTagFlow,
                            preferencesManager.homeAlignmentFlow,
                            preferencesManager.allowLandscapeRotationFlow,
                    ) { fontVisual, localeTag, homeAlignment, allowLandscape ->
                        LookPrefs(
                                launcherFontFamilyName = fontVisual.family,
                                launcherFontScale = fontVisual.scale,
                                launcherVisualStyle = fontVisual.visualStyle,
                                launcherGlowEnabled = fontVisual.glowEnabled,
                                appLocaleTag = localeTag,
                                homeAlignment = homeAlignment,
                                allowLandscapeRotation = allowLandscape,
                        )
                    }
            val lockRailPrefsFlow =
                    combine(
                            preferencesManager.doubleTapEmptyLockFlow,
                            preferencesManager.longLockReturnHomeFlow,
                            preferencesManager.longLockReturnHomeThresholdMinutesFlow,
                            preferencesManager.drawerCategorySidebarOnLeftFlow,
                            preferencesManager.drawerCategoryIconsFlow,
                    ) { doubleTap, longLockReturn, longLockMinutes, railOnLeft, iconOverrides ->
                        LockRailPrefs(
                                doubleTapEmptyLock = doubleTap,
                                longLockReturnHome = longLockReturn,
                                longLockReturnHomeThresholdMinutes = longLockMinutes,
                                drawerCategorySidebarOnLeft = railOnLeft,
                                categoryDrawerIconOverrides = iconOverrides,
                        )
                    }
            val drawerLookLockFlow =
                    combine(drawerPrefsFlow, lookPrefsFlow, lockRailPrefsFlow) {
                            drawer,
                            look,
                            lockRail ->
                        Triple(drawer, look, lockRail)
                    }
            combine(categoryStateFlow, homeWidgetItemsFlow, drawerLookLockFlow) {
                    left,
                    homeWidgetItems,
                    drawerLookLock ->
                val (drawer, look, lockRail) = drawerLookLock
                val privateSpaceUnlocked = privateSpaceManager.isPrivateSpaceUnlocked()
                val privateProfileKey =
                        privateSpaceManager
                                .getPrivateSpaceProfile()
                                ?.takeIf { it != Process.myUserHandle() }
                                ?.let(::appProfileKey)
                val categoryMap = left.appCategories
                val installedApps =
                        appRepository.getInstalledAppsOnBackground().map { app ->
                            app.copy(
                                    category =
                                            categoryMap[
                                                            appMetadataKey(
                                                                    app.packageName,
                                                                    app.userHandle,
                                                            )
                                                    ]
                                                    ?: app.category,
                            )
                        }
                val privateApps =
                        if (privateSpaceUnlocked) {
                            privateSpaceManager.getPrivateSpaceApps()
                        } else {
                            emptyList()
                        }
                val metadataLookupApps = installedApps + privateApps
                val allShortcutActions = appRepository.getAllShortcutActionsOnBackground()
                val hiddenLabels =
                        metadataLookupApps.associate {
                            appMetadataKey(it.packageName, it.userHandle) to it
                        }
                SettingsUiState(
                        hiddenApps =
                                hiddenInfosForSettings(
                                        left.hiddenApps,
                                        hiddenLabels,
                                        privateSpaceUnlocked,
                                        privateProfileKey,
                                ),
                        renamedApps =
                                renamedInfosForSettings(
                                        left.renamedApps,
                                        hiddenLabels,
                                        privateSpaceUnlocked,
                                        privateProfileKey,
                                ),
                        appCategories = left.appCategories,
                        categoryDefinitions = left.categoryDefinitions,
                        favorites = left.favorites,
                        rightSideShortcuts = left.rightSideShortcuts,
                        swipeLeftTarget = left.swipeLeft,
                        swipeRightTarget = drawer.swipeRightTarget,
                        preferredWeatherAppPackage = drawer.preferredWeatherAppPackage,
                        preferredClockAppPackage = homeWidgetItems.preferredClockAppPackage,
                        preferredCalendarAppPackage =
                                homeWidgetItems.preferredCalendarAppPackage,
                        showStatusBar = drawer.showStatusBar,
                        showHomeClock = homeWidgetItems.showClock,
                        showHomeDate = homeWidgetItems.showDate,
                        showHomeWeather = homeWidgetItems.showWeather,
                        showHomeBattery = homeWidgetItems.showBattery,
                        homeDateFormatStyle = homeWidgetItems.homeDateFormatStyle,
                        drawerSidebarCategories = drawer.drawerSidebarCategories,
                        drawerSearchAutoLaunch = drawer.drawerSearchAutoLaunch,
                        drawerCategorySidebarOnLeft = lockRail.drawerCategorySidebarOnLeft,
                        categoryDrawerIconOverrides = lockRail.categoryDrawerIconOverrides,
                        drawerAppSortMode = drawer.drawerAppSortMode,
                        homeAlignment = look.homeAlignment,
                        launcherFontFamilyName = look.launcherFontFamilyName,
                        launcherFontScale = look.launcherFontScale,
                        launcherVisualStyle = look.launcherVisualStyle,
                        launcherGlowEnabled = look.launcherGlowEnabled,
                        appLocaleTag = look.appLocaleTag,
                        allowLandscapeRotation = look.allowLandscapeRotation,
                        doubleTapEmptyLock = lockRail.doubleTapEmptyLock,
                        longLockReturnHome = lockRail.longLockReturnHome,
                        longLockReturnHomeThresholdMinutes =
                                lockRail.longLockReturnHomeThresholdMinutes,
                        allApps = installedApps,
                        allShortcutActions = allShortcutActions,
                )
            }.collectLatest { _uiState.value = it }
        }
    }

    private data class HomeWidgetItemSettings(
            val showClock: Boolean,
            val showDate: Boolean,
            val showWeather: Boolean,
            val showBattery: Boolean,
            val preferredClockAppPackage: String,
            val preferredCalendarAppPackage: String,
            val homeDateFormatStyle: HomeDateFormatStyle
    )

    private data class FavoritesBase(
            val hiddenApps: List<HiddenAppEntity>,
            val renamedApps: List<RenamedAppEntity>,
            val favorites: List<FavoriteApp>,
            val rightSideShortcuts: List<HomeShortcut>,
            val swipeLeft: ShortcutTarget?,
    )

    private data class CategoryState(
            val hiddenApps: List<HiddenAppEntity>,
            val renamedApps: List<RenamedAppEntity>,
            val favorites: List<FavoriteApp>,
            val rightSideShortcuts: List<HomeShortcut>,
            val swipeLeft: ShortcutTarget?,
            val appCategories: Map<String, String>,
            val categoryDefinitions: List<String>,
    )

    private data class DrawerPrefs(
            val swipeRightTarget: ShortcutTarget?,
            val preferredWeatherAppPackage: String,
            val showStatusBar: Boolean,
            val drawerSidebarCategories: Boolean,
            val drawerAppSortMode: DrawerAppSortMode,
            val drawerSearchAutoLaunch: Boolean,
    )

    private data class FontVisualPrefs(
            val family: String,
            val scale: Float,
            val visualStyle: LauncherVisualStyle,
            val glowEnabled: Boolean,
    )

    private data class LookPrefs(
            val launcherFontFamilyName: String,
            val launcherFontScale: Float,
            val launcherVisualStyle: LauncherVisualStyle,
            val launcherGlowEnabled: Boolean,
            val appLocaleTag: String,
            val homeAlignment: HomeAlignment,
            val allowLandscapeRotation: Boolean,
    )

    private data class LockRailPrefs(
            val doubleTapEmptyLock: Boolean,
            val longLockReturnHome: Boolean,
            val longLockReturnHomeThresholdMinutes: Int,
            val drawerCategorySidebarOnLeft: Boolean,
            val categoryDrawerIconOverrides: Map<String, String>,
    )

    private inline fun <T, R> mapEntitiesForSettings(
            entities: List<T>,
            hiddenLabels: Map<String, AppInfo>,
            privateSpaceUnlocked: Boolean,
            privateProfileKey: String?,
            packageName: (T) -> String,
            profileKey: (T) -> String,
            transform: (T, AppInfo?, String?) -> R,
    ): List<R> =
            entities.mapNotNull { entity ->
                val pkg = packageName(entity)
                val prof = profileKey(entity)
                val key = appMetadataKey(pkg, prof)
                val matchingApp = hiddenLabels[key]
                if (!privateSpaceUnlocked && prof == privateProfileKey) null
                else
                        transform(
                                entity,
                                matchingApp,
                                profileLabelForSettings(prof, matchingApp, privateProfileKey),
                        )
            }

    private fun hiddenInfosForSettings(
            hiddenApps: List<HiddenAppEntity>,
            hiddenLabels: Map<String, AppInfo>,
            privateSpaceUnlocked: Boolean,
            privateProfileKey: String?,
    ): List<HiddenAppInfo> =
            mapEntitiesForSettings(
                    hiddenApps,
                    hiddenLabels,
                    privateSpaceUnlocked,
                    privateProfileKey,
                    packageName = { it.packageName },
                    profileKey = { it.profileKey },
            ) { hiddenApp, matchingApp, profileLabel ->
                HiddenAppInfo(
                        packageName = hiddenApp.packageName,
                        profileKey = hiddenApp.profileKey,
                        label = matchingApp?.label ?: hiddenApp.packageName,
                        profileLabel = profileLabel,
                )
            }.sortedWith(
                    compareBy(
                            { profileSortBucket(it.profileLabel) },
                            { it.profileLabel ?: "" },
                            { it.label.lowercase() },
                            { it.packageName.lowercase() },
                    ),
            )

    private fun renamedInfosForSettings(
            renamedApps: List<RenamedAppEntity>,
            hiddenLabels: Map<String, AppInfo>,
            privateSpaceUnlocked: Boolean,
            privateProfileKey: String?,
    ): List<RenamedAppInfo> =
            mapEntitiesForSettings(
                    renamedApps,
                    hiddenLabels,
                    privateSpaceUnlocked,
                    privateProfileKey,
                    packageName = { it.packageName },
                    profileKey = { it.profileKey },
            ) { renamedApp, matchingApp, profileLabel ->
                RenamedAppInfo(
                        packageName = renamedApp.packageName,
                        profileKey = renamedApp.profileKey,
                        customName = renamedApp.customName,
                        profileLabel = profileLabel,
                )
            }.sortedWith(
                    compareBy(
                            { profileSortBucket(it.profileLabel) },
                            { it.profileLabel ?: "" },
                            { it.customName.lowercase() },
                            { it.packageName.lowercase() },
                    ),
            )

    private fun profileLabelForSettings(
            profileKey: String,
            matchingApp: AppInfo?,
            privateProfileKey: String?
    ): String? {
        matchingApp?.let { app ->
            val userHandle = app.userHandle
            if (userHandle != null && privateSpaceManager.isPrivateSpaceProfile(userHandle)) {
                return categoryChipDisplayLabel(context, ReservedCategoryNames.PRIVATE)
            }
            if (isDrawerWorkProfileApp(context, app)) {
                return categoryChipDisplayLabel(context, ReservedCategoryNames.WORK)
            }
            return profileOriginLabelForApp(context, app)
        }
        if (profileKey == privateProfileKey) {
            return categoryChipDisplayLabel(context, ReservedCategoryNames.PRIVATE)
        }
        return if (profileKey == "0") {
            null
        } else {
            categoryChipDisplayLabel(context, ReservedCategoryNames.WORK)
        }
    }

    private fun profileSortBucket(profileLabel: String?): Int =
            if (profileLabel == null) 0 else 1

    // --- Hidden Apps ---

    fun unhideApp(packageName: String, profileKey: String) {
        viewModelScope.launch { appRepository.unhideApp(packageName, profileKey) }
    }

    // --- Renamed Apps ---

    fun removeRename(packageName: String, profileKey: String) {
        viewModelScope.launch { appRepository.removeRename(packageName, profileKey) }
    }

    fun addCategoryDefinition(name: String) {
        viewModelScope.launch { _addCategoryResults.emit(appRepository.addCategoryDefinition(name)) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            preferencesManager.clearDrawerCategoryIcon(name)
            appRepository.deleteCategory(name)
        }
    }

    fun setAppCategory(packageName: String, profileKey: String, category: String) {
        viewModelScope.launch { appRepository.setAppCategory(packageName, profileKey, category) }
    }

    fun reorderCategories(categories: List<String>) {
        viewModelScope.launch { appRepository.reorderCategoryDefinitions(categories) }
    }

    // --- Swipe gestures ---

    private fun launchPreferences(block: suspend PreferencesManager.() -> Unit) {
        viewModelScope.launch { preferencesManager.block() }
    }

    fun setSwipeLeftTarget(target: ShortcutTarget?) =
            launchPreferences { setSwipeLeftTarget(target) }

    fun setSwipeRightTarget(target: ShortcutTarget?) =
            launchPreferences { setSwipeRightTarget(target) }

    fun setPreferredWeatherApp(packageName: String) =
            launchPreferences { setPreferredWeatherApp(packageName) }

    fun setPreferredClockApp(packageName: String) =
            launchPreferences { setPreferredClockApp(packageName) }

    fun setPreferredCalendarApp(packageName: String) =
            launchPreferences { setPreferredCalendarApp(packageName) }

    fun setShowStatusBar(show: Boolean) = launchPreferences { setShowStatusBar(show) }

    fun setAllowLandscapeRotation(allow: Boolean) =
            launchPreferences { setAllowLandscapeRotation(allow) }

    fun setDoubleTapEmptyLock(enabled: Boolean) =
            launchPreferences { setDoubleTapEmptyLock(enabled) }

    fun setLongLockReturnHome(enabled: Boolean) =
            launchPreferences { setLongLockReturnHome(enabled) }

    fun setLongLockReturnHomeThresholdMinutes(minutes: Int) =
            launchPreferences { setLongLockReturnHomeThresholdMinutes(minutes) }

    fun acceptAccessibilityProminentDisclosureAndOpenSettings() {
        viewModelScope.launch {
            preferencesManager.setAccessibilityProminentDisclosureAccepted(true)
            LockScreenHelper.openAccessibilitySettings(context)
        }
    }

    fun setShowHomeClock(show: Boolean) = launchPreferences { setShowHomeClock(show) }

    fun setShowHomeDate(show: Boolean) = launchPreferences { setShowHomeDate(show) }

    fun setShowHomeWeather(show: Boolean) = launchPreferences { setShowHomeWeather(show) }

    fun setShowHomeBattery(show: Boolean) = launchPreferences { setShowHomeBattery(show) }

    fun setHomeDateFormatStyle(style: HomeDateFormatStyle) =
            launchPreferences { setHomeDateFormatStyle(style) }

    fun setDrawerSidebarCategories(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                if (preferencesManager.drawerAppSortModeFlow.first() == DrawerAppSortMode.CUSTOM) {
                    preferencesManager.setDrawerAppSortMode(DrawerAppSortMode.ALPHABETICAL)
                }
            }
            preferencesManager.setDrawerSidebarCategories(enabled)
        }
    }

    fun setDrawerSearchAutoLaunch(enabled: Boolean) =
            launchPreferences { setDrawerSearchAutoLaunch(enabled) }

    fun setDrawerCategorySidebarOnLeft(onLeft: Boolean) =
            launchPreferences { setDrawerCategorySidebarOnLeft(onLeft) }

    fun setCategoryDrawerIcon(category: String, iconName: String) {
        if (!MinimalIcons.all.containsKey(iconName)) return
        viewModelScope.launch { preferencesManager.setDrawerCategoryIcon(category, iconName) }
    }

    fun clearCategoryDrawerIcon(category: String) =
            launchPreferences { clearDrawerCategoryIcon(category) }

    fun setDrawerAppSortMode(mode: DrawerAppSortMode) {
        viewModelScope.launch {
            if (mode == DrawerAppSortMode.CUSTOM &&
                            !preferencesManager.drawerSidebarCategoriesFlow.first()
            ) {
                return@launch
            }
            preferencesManager.setDrawerAppSortMode(mode)
        }
    }

    // --- Home alignment ---

    fun setHomeAlignment(alignment: HomeAlignment) =
            launchPreferences { setHomeAlignment(alignment) }

    fun setLauncherFontFamilyName(familyName: String) =
            launchPreferences { setLauncherFontFamilyName(familyName) }

    fun setLauncherFontScale(scale: Float) =
            launchPreferences { setLauncherFontScale(scale) }

    fun setLauncherVisualStyle(style: LauncherVisualStyle) =
            launchPreferences { setLauncherVisualStyle(style) }

    fun setLauncherGlowEnabled(enabled: Boolean) =
            launchPreferences { setLauncherGlowEnabled(enabled) }

    fun setAppLocaleTag(tag: String) {
        viewModelScope.launch {
            preferencesManager.setAppLocaleTag(tag)
            AppLocaleHelper.applyLocaleTag(tag)
            appRepository.invalidateCache()
        }
    }

    /** Builds a share intent for a diagnostic text file (metadata + app process logcat). */
    suspend fun createLogShareIntent(): Intent? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = AppDiagnosticLogExporter.writeExportFile(context)
                    val uri =
                            FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                            )
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.settings_export_logs_share_subject)
                        )
                        clipData =
                                ClipData.newUri(
                                        context.contentResolver,
                                        context.getString(R.string.settings_export_logs_title),
                                        uri
                                )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }.getOrNull()
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
        viewModelScope.launch { WallpaperHelper.setBlackWallpaper(context) }
    }
}
