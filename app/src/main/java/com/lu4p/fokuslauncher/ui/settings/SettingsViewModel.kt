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
import com.lu4p.fokuslauncher.data.model.TemperatureUnit
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.appMetadataKey
import com.lu4p.fokuslauncher.data.model.metadataSettingsStableKey
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.font.CustomFontImportFailure
import com.lu4p.fokuslauncher.data.font.CustomFontImportResult
import com.lu4p.fokuslauncher.data.font.CustomFontStore
import com.lu4p.fokuslauncher.data.font.SystemFontFamiliesProvider
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle
import com.lu4p.fokuslauncher.data.model.PhotoWallpaperDrawerOverlayIntensity
import com.lu4p.fokuslauncher.data.model.PhotoWallpaperOutlineWidthDp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.WidgetTapTarget
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
import kotlinx.coroutines.flow.update
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
        val preferredWeatherTap: WidgetTapTarget? = null,
        val preferredClockTap: WidgetTapTarget? = null,
        val preferredCalendarTap: WidgetTapTarget? = null,
        val showStatusBar: Boolean = false,
        val showHomeClock: Boolean = true,
        val showHomeDate: Boolean = true,
        val showHomeWeather: Boolean = true,
        val showHomeBattery: Boolean = true,
        val homeDateFormatStyle: HomeDateFormatStyle = HomeDateFormatStyle.SYSTEM_DEFAULT,
        val temperatureUnit: TemperatureUnit = TemperatureUnit.SYSTEM_DEFAULT,
        /** Vertical category sidebar in the app drawer. */
        val drawerSidebarCategories: Boolean = false,
        /** Launch the app when search narrows to a single match (drawer search). */
        val drawerSearchAutoLaunch: Boolean = true,
        /** Auto-launch keyboard when scrolling to the top of the app drawer. */
        val drawerScrollToTopAutoKeyboard: Boolean = false,
        /** When true, category rail is on the left; default false places it on the right. */
        val drawerCategorySidebarOnLeft: Boolean = false,
        /** Normalized category key → [MinimalIcons] name for the drawer sidebar rail. */
        val categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        val drawerAppSortMode: DrawerAppSortMode = DrawerAppSortMode.ALPHABETICAL,
        val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
        val launcherFontFamilyName: String = "",
        /** True when an imported `.ttf` is present in app-private storage. */
        val hasCustomFontFile: Boolean = false,
        /** Label shown for the imported font in the font dropdown (file name without `.ttf`). */
        val customFontDisplayName: String = "",
        val launcherFontScale: Float = LauncherFontScale.DEFAULT,
        val launcherVisualStyle: LauncherVisualStyle = LauncherVisualStyle.CLASSIC,
        /** Text shadow + icon halo; independent of [launcherVisualStyle]. */
        val launcherGlowEnabled: Boolean = false,
        /** True when the home wallpaper is not solid black (image or busy wallpaper). */
        val homeUsesPhotoWallpaper: Boolean = false,
        /** Uniform outline stroke in dp on image wallpaper; 0 = launcher defaults per widget. */
        val photoWallpaperOutlineWidthDp: Float = PhotoWallpaperOutlineWidthDp.DEFAULT,
        /** Drawer scrim alpha multiplier; only applied when [homeUsesPhotoWallpaper]. */
        val photoWallpaperDrawerOverlayIntensity: Float = PhotoWallpaperDrawerOverlayIntensity.DEFAULT,
        /** BCP-47 tag; empty = system default. */
        val appLocaleTag: String = "",
        val allowLandscapeRotation: Boolean = false,
        val doubleTapEmptyLock: Boolean = false,
        val longLockReturnHome: Boolean = false,
        val longLockReturnHomeThresholdMinutes: Int =
                PreferencesManager.DEFAULT_LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES,
        val allApps: List<AppInfo> = emptyList(),
        val allShortcutActions: List<AppShortcutAction> = emptyList(),
        /** [appProfileKey] → user-defined profile section title (drawer, badges, settings). */
        val profileDisplayNameOverrides: Map<String, String> = emptyMap(),
)

data class HiddenAppInfo(
        val packageName: String,
        val profileKey: String,
        val launcherShortcutId: String,
        val label: String,
        val profileLabel: String?,
) {
    val stableKey: String
        get() = metadataSettingsStableKey(packageName, profileKey, launcherShortcutId)
}

data class RenamedAppInfo(
        val packageName: String,
        val profileKey: String,
        val launcherShortcutId: String,
        val customName: String,
        val profileLabel: String?,
) {
    val stableKey: String
        get() = metadataSettingsStableKey(packageName, profileKey, launcherShortcutId)
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val preferencesManager: PreferencesManager,
        private val privateSpaceManager: PrivateSpaceManager,
        private val customFontStore: CustomFontStore,
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
                            preferencesManager.preferredClockTapFlow,
                            preferencesManager.preferredCalendarTapFlow,
                            preferencesManager.homeDateFormatStyleFlow,
                            preferencesManager.temperatureUnitFlow,
                    ) { vis, clk, cal, fmt, tempUnit ->
                        HomeWidgetItemSettings(
                                showClock = vis.showClock,
                                showDate = vis.showDate,
                                showWeather = vis.showWeather,
                                showBattery = vis.showBattery,
                                preferredClockTap = clk,
                                preferredCalendarTap = cal,
                                homeDateFormatStyle = fmt,
                                temperatureUnit = tempUnit,
                        )
                    }
            val drawerPrefsFlow =
                    combine(
                            combine(
                                    preferencesManager.swipeRightTargetFlow,
                                    preferencesManager.preferredWeatherTapFlow,
                                    preferencesManager.showStatusBarFlow,
                            ) { swipeRight, weatherTap, showStatusBar ->
                                Triple(swipeRight, weatherTap, showStatusBar)
                            },
                            combine(
                                    preferencesManager.drawerSidebarCategoriesFlow,
                                    preferencesManager.drawerAppSortModeFlow,
                                    preferencesManager.drawerSearchAutoLaunchFlow,
                                    preferencesManager.drawerScrollToTopAutoKeyboardFlow,
                            ) { sidebarCategories, sortMode, searchAutoLaunch, scrollToTopAutoKeyboard ->
                                DrawerPrefs(
                                        swipeRightTarget = null, // placeholder
                                        preferredWeatherTap = null, // placeholder
                                        showStatusBar = false, // placeholder
                                        drawerSidebarCategories = sidebarCategories,
                                        drawerAppSortMode = sortMode,
                                        drawerSearchAutoLaunch = searchAutoLaunch,
                                        drawerScrollToTopAutoKeyboard = scrollToTopAutoKeyboard,
                                )
                            },
                    ) { swipeAndWeather, drawerLayout ->
                        drawerLayout.copy(
                                swipeRightTarget = swipeAndWeather.first,
                                preferredWeatherTap = swipeAndWeather.second,
                                showStatusBar = swipeAndWeather.third,
                        )
                    }
            val fontVisualFlow =
                    combine(
                            preferencesManager.launcherFontFamilyFlow,
                            preferencesManager.launcherFontScaleFlow,
                            preferencesManager.launcherAppearanceFlow,
                            preferencesManager.launcherCustomFontDisplayNameFlow,
                    ) { font, fontScale, appearance, customFontDisplayName ->
                        val resolvedCustomLabel =
                                customFontDisplayName.ifBlank {
                                    customFontStore.readStoredFontDisplayLabel().orEmpty()
                                }
                        FontVisualPrefs(
                                family = font,
                                scale = fontScale,
                                visualStyle = appearance.visualStyle,
                                glowEnabled = appearance.glowEnabled,
                                usesPhotoWallpaper = appearance.usesPhotoWallpaper,
                                customFontDisplayName = resolvedCustomLabel,
                        )
                    }
            val lookPrefsFlow =
                    combine(
                            combine(
                                    fontVisualFlow,
                                    preferencesManager.photoWallpaperOutlineWidthDpFlow,
                                    preferencesManager.photoWallpaperDrawerOverlayIntensityFlow,
                            ) { fontVisual, outlineWidthDp, drawerOverlayIntensity ->
                                Triple(fontVisual, outlineWidthDp, drawerOverlayIntensity)
                            },
                            preferencesManager.appLocaleTagFlow,
                            preferencesManager.homeAlignmentFlow,
                            preferencesManager.allowLandscapeRotationFlow,
                    ) { fontOutlineDrawer, localeTag, homeAlignment, allowLandscape ->
                        val (fontVisual, outlineWidthDp, drawerOverlayIntensity) = fontOutlineDrawer
                        LookPrefs(
                                launcherFontFamilyName = fontVisual.family,
                                hasCustomFontFile = customFontStore.hasStoredFont(),
                                customFontDisplayName = fontVisual.customFontDisplayName,
                                launcherFontScale = fontVisual.scale,
                                launcherVisualStyle = fontVisual.visualStyle,
                                launcherGlowEnabled = fontVisual.glowEnabled,
                                homeUsesPhotoWallpaper = fontVisual.usesPhotoWallpaper,
                                photoWallpaperOutlineWidthDp = outlineWidthDp,
                                photoWallpaperDrawerOverlayIntensity = drawerOverlayIntensity,
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
            combine(categoryStateFlow, homeWidgetItemsFlow, drawerLookLockFlow, preferencesManager.profileDisplayNameOverridesFlow) {
                    left,
                    homeWidgetItems,
                    drawerLookLock,
                    profileDisplayNameOverrides,
                    ->
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
                SettingsUiState(
                        hiddenApps =
                                hiddenInfosForSettings(
                                        left.hiddenApps,
                                        metadataLookupApps,
                                        privateSpaceUnlocked,
                                        privateProfileKey,
                                        profileDisplayNameOverrides,
                                ),
                        renamedApps =
                                renamedInfosForSettings(
                                        left.renamedApps,
                                        metadataLookupApps,
                                        privateSpaceUnlocked,
                                        privateProfileKey,
                                        profileDisplayNameOverrides,
                                ),
                        appCategories = left.appCategories,
                        categoryDefinitions = left.categoryDefinitions,
                        favorites = left.favorites,
                        rightSideShortcuts = left.rightSideShortcuts,
                        swipeLeftTarget = left.swipeLeft,
                        swipeRightTarget = drawer.swipeRightTarget,
                        preferredWeatherTap = drawer.preferredWeatherTap,
                        preferredClockTap = homeWidgetItems.preferredClockTap,
                        preferredCalendarTap = homeWidgetItems.preferredCalendarTap,
                        showStatusBar = drawer.showStatusBar,
                        showHomeClock = homeWidgetItems.showClock,
                        showHomeDate = homeWidgetItems.showDate,
                        showHomeWeather = homeWidgetItems.showWeather,
                        showHomeBattery = homeWidgetItems.showBattery,
                        homeDateFormatStyle = homeWidgetItems.homeDateFormatStyle,
                        temperatureUnit = homeWidgetItems.temperatureUnit,
                        drawerSidebarCategories = drawer.drawerSidebarCategories,
                        drawerSearchAutoLaunch = drawer.drawerSearchAutoLaunch,
                        drawerScrollToTopAutoKeyboard = drawer.drawerScrollToTopAutoKeyboard,
                        drawerCategorySidebarOnLeft = lockRail.drawerCategorySidebarOnLeft,
                        categoryDrawerIconOverrides = lockRail.categoryDrawerIconOverrides,
                        drawerAppSortMode = drawer.drawerAppSortMode,
                        homeAlignment = look.homeAlignment,
                        launcherFontFamilyName = look.launcherFontFamilyName,
                        hasCustomFontFile = look.hasCustomFontFile,
                        customFontDisplayName = look.customFontDisplayName,
                        launcherFontScale = look.launcherFontScale,
                        launcherVisualStyle = look.launcherVisualStyle,
                        launcherGlowEnabled = look.launcherGlowEnabled,
                        homeUsesPhotoWallpaper = look.homeUsesPhotoWallpaper,
                        photoWallpaperOutlineWidthDp = look.photoWallpaperOutlineWidthDp,
                        photoWallpaperDrawerOverlayIntensity =
                                look.photoWallpaperDrawerOverlayIntensity,
                        appLocaleTag = look.appLocaleTag,
                        allowLandscapeRotation = look.allowLandscapeRotation,
                        doubleTapEmptyLock = lockRail.doubleTapEmptyLock,
                        longLockReturnHome = lockRail.longLockReturnHome,
                        longLockReturnHomeThresholdMinutes =
                                lockRail.longLockReturnHomeThresholdMinutes,
                        allApps = installedApps,
                        allShortcutActions = allShortcutActions,
                        profileDisplayNameOverrides = profileDisplayNameOverrides,
                )
            }.collectLatest { _uiState.value = it }
        }
    }

    private data class HomeWidgetItemSettings(
            val showClock: Boolean,
            val showDate: Boolean,
            val showWeather: Boolean,
            val showBattery: Boolean,
            val preferredClockTap: WidgetTapTarget?,
            val preferredCalendarTap: WidgetTapTarget?,
            val homeDateFormatStyle: HomeDateFormatStyle,
            val temperatureUnit: TemperatureUnit
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
            val preferredWeatherTap: WidgetTapTarget?,
            val showStatusBar: Boolean,
            val drawerSidebarCategories: Boolean,
            val drawerAppSortMode: DrawerAppSortMode,
            val drawerSearchAutoLaunch: Boolean,
            val drawerScrollToTopAutoKeyboard: Boolean,
    )

    private data class FontVisualPrefs(
            val family: String,
            val scale: Float,
            val visualStyle: LauncherVisualStyle,
            val glowEnabled: Boolean,
            val usesPhotoWallpaper: Boolean,
            val customFontDisplayName: String,
    )

    private data class LookPrefs(
            val launcherFontFamilyName: String,
            val hasCustomFontFile: Boolean,
            val customFontDisplayName: String,
            val launcherFontScale: Float,
            val launcherVisualStyle: LauncherVisualStyle,
            val launcherGlowEnabled: Boolean,
            val homeUsesPhotoWallpaper: Boolean,
            val photoWallpaperOutlineWidthDp: Float,
            val photoWallpaperDrawerOverlayIntensity: Float,
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
            installedApps: List<AppInfo>,
            privateSpaceUnlocked: Boolean,
            privateProfileKey: String?,
            profileDisplayNameOverrides: Map<String, String>,
            packageName: (T) -> String,
            profileKey: (T) -> String,
            launcherShortcutId: (T) -> String,
            transform: (T, AppInfo?, String?) -> R,
    ): List<R> =
            entities.mapNotNull { entity ->
                val pkg = packageName(entity)
                val prof = profileKey(entity)
                val shortcutId = launcherShortcutId(entity)
                val matchingApp =
                        installedApps.find { app ->
                            app.packageName == pkg &&
                                    appProfileKey(app.userHandle) == prof &&
                                    when (shortcutId) {
                                        com.lu4p.fokuslauncher.data.model.HOST_APP_METADATA_SENTINEL ->
                                                app.launcherShortcutId == null
                                        com.lu4p.fokuslauncher.data.model.LEGACY_PACKAGE_WIDE_METADATA ->
                                                true
                                        else -> app.launcherShortcutId == shortcutId
                                    }
                        }
                if (!privateSpaceUnlocked && prof == privateProfileKey) null
                else
                        transform(
                                entity,
                                matchingApp,
                                profileLabelForSettings(
                                        prof,
                                        matchingApp,
                                        privateProfileKey,
                                        profileDisplayNameOverrides,
                                ),
                        )
            }

    private fun hiddenInfosForSettings(
            hiddenApps: List<HiddenAppEntity>,
            installedApps: List<AppInfo>,
            privateSpaceUnlocked: Boolean,
            privateProfileKey: String?,
            profileDisplayNameOverrides: Map<String, String>,
    ): List<HiddenAppInfo> =
            mapEntitiesForSettings(
                    hiddenApps,
                    installedApps,
                    privateSpaceUnlocked,
                    privateProfileKey,
                    profileDisplayNameOverrides,
                    packageName = { it.packageName },
                    profileKey = { it.profileKey },
                    launcherShortcutId = { it.launcherShortcutId },
            ) { hiddenApp, matchingApp, profileLabel ->
                HiddenAppInfo(
                        packageName = hiddenApp.packageName,
                        profileKey = hiddenApp.profileKey,
                        launcherShortcutId = hiddenApp.launcherShortcutId,
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
            installedApps: List<AppInfo>,
            privateSpaceUnlocked: Boolean,
            privateProfileKey: String?,
            profileDisplayNameOverrides: Map<String, String>,
    ): List<RenamedAppInfo> =
            mapEntitiesForSettings(
                    renamedApps,
                    installedApps,
                    privateSpaceUnlocked,
                    privateProfileKey,
                    profileDisplayNameOverrides,
                    packageName = { it.packageName },
                    profileKey = { it.profileKey },
                    launcherShortcutId = { it.launcherShortcutId },
            ) { renamedApp, matchingApp, profileLabel ->
                RenamedAppInfo(
                        packageName = renamedApp.packageName,
                        profileKey = renamedApp.profileKey,
                        launcherShortcutId = renamedApp.launcherShortcutId,
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
            privateProfileKey: String?,
            profileDisplayNameOverrides: Map<String, String>,
    ): String? {
        val custom = profileDisplayNameOverrides[profileKey]?.trim()
        if (!custom.isNullOrEmpty()) return custom
        matchingApp?.let { app ->
            val userHandle = app.userHandle
            if (userHandle != null && privateSpaceManager.isPrivateSpaceProfile(userHandle)) {
                return categoryChipDisplayLabel(context, ReservedCategoryNames.PRIVATE)
            }
            if (isDrawerWorkProfileApp(context, app)) {
                return categoryChipDisplayLabel(context, ReservedCategoryNames.WORK)
            }
            return profileOriginLabelForApp(context, app, profileDisplayNameOverrides)
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

    fun unhideApp(packageName: String, profileKey: String, launcherShortcutId: String) {
        viewModelScope.launch {
            appRepository.unhideApp(packageName, profileKey, launcherShortcutId)
        }
    }

    // --- Renamed Apps ---

    fun removeRename(packageName: String, profileKey: String, launcherShortcutId: String) {
        viewModelScope.launch {
            appRepository.removeRename(packageName, profileKey, launcherShortcutId)
        }
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

    fun setProfileDisplayName(profileKey: String, displayName: String) {
        viewModelScope.launch { preferencesManager.setProfileDisplayName(profileKey, displayName) }
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

    fun setPreferredWeatherTap(action: AppShortcutAction?) =
            launchPreferences {
                setPreferredWeatherTap(
                        action?.let { WidgetTapTarget(it.target, it.profileKey) }
                )
            }

    fun setPreferredClockTap(action: AppShortcutAction?) =
            launchPreferences {
                setPreferredClockTap(
                        action?.let { WidgetTapTarget(it.target, it.profileKey) }
                )
            }

    fun setPreferredCalendarTap(action: AppShortcutAction?) =
            launchPreferences {
                setPreferredCalendarTap(
                        action?.let { WidgetTapTarget(it.target, it.profileKey) }
                )
            }

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

    fun setTemperatureUnit(unit: TemperatureUnit) =
            launchPreferences { setTemperatureUnit(unit) }

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

    fun setDrawerScrollToTopAutoKeyboard(enabled: Boolean) =
            launchPreferences { setDrawerScrollToTopAutoKeyboard(enabled) }

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

    fun resolveCustomFontFile(storageValue: String) = customFontStore.resolveFile(storageValue)

    fun importCustomFont(uri: Uri, onResult: (CustomFontImportFailure?) -> Unit) {
        viewModelScope.launch {
            val failure =
                    withContext(Dispatchers.IO) {
                        when (val result = customFontStore.importFromUri(uri)) {
                            is CustomFontImportResult.Success -> {
                                preferencesManager.setLauncherFontFamilyName(result.storageValue)
                                preferencesManager.setLauncherCustomFontDisplayName(
                                        result.displayLabel
                                )
                                _uiState.update {
                                    it.copy(
                                            hasCustomFontFile = true,
                                            customFontDisplayName = result.displayLabel,
                                    )
                                }
                                null
                            }
                            is CustomFontImportResult.Failure -> result.reason
                        }
                    }
            onResult(failure)
        }
    }

    fun clearCustomFont(selectSystemDefault: Boolean = true) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                customFontStore.deleteStoredFont()
                preferencesManager.clearLauncherCustomFontDisplayName()
                if (selectSystemDefault &&
                                LauncherFontPreferences.isCustomFont(
                                        _uiState.value.launcherFontFamilyName
                                )
                ) {
                    preferencesManager.setLauncherFontFamilyName(
                            LauncherFontPreferences.DEFAULT_FONT_FAMILY_STORAGE
                    )
                }
            }
            _uiState.update { it.copy(hasCustomFontFile = false, customFontDisplayName = "") }
        }
    }

    fun setLauncherFontScale(scale: Float) =
            launchPreferences { setLauncherFontScale(scale) }

    fun setLauncherVisualStyle(style: LauncherVisualStyle) {
        viewModelScope.launch {
            if (preferencesManager.launcherAppearanceFlow.first().usesPhotoWallpaper) return@launch
            preferencesManager.setLauncherVisualStyle(style)
        }
    }

    fun setLauncherGlowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (preferencesManager.launcherAppearanceFlow.first().usesPhotoWallpaper) return@launch
            preferencesManager.setLauncherGlowEnabled(enabled)
        }
    }

    fun setPhotoWallpaperOutlineWidthDp(widthDp: Float) =
            launchPreferences { setPhotoWallpaperOutlineWidthDp(widthDp) }

    fun setPhotoWallpaperDrawerOverlayIntensity(intensity: Float) =
            launchPreferences { setPhotoWallpaperDrawerOverlayIntensity(intensity) }

    fun setAppLocaleTag(tag: String) {
        viewModelScope.launch {
            preferencesManager.setAppLocaleTag(tag)
            AppLocaleHelper.applyLocaleTag(tag)
            appRepository.invalidateCache()
        }
    }

    /** Builds a share intent for a diagnostic text file (metadata + logcat, including prior sessions). */
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
        customFontStore.deleteStoredFont()
        preferencesManager.clearAll()
        appRepository.clearAllAppData()
        appRepository.invalidateCache()
    }

    fun setSystemWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                stream.use { wallpaperManager.setStream(it) }
                preferencesManager.setHomeUsesPhotoWallpaper(true)
            } catch (e: Exception) {
                // Ignore or handle error
            }
        }
    }

    fun setBlackWallpaper() {
        viewModelScope.launch {
            WallpaperHelper.setBlackWallpaper(context)
            preferencesManager.setHomeUsesPhotoWallpaper(false)
        }
    }
}
