package com.lu4p.fokuslauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.ui.drawer.GroupedAppPickerDialog
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForFavorite
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.ui.components.ClockWidget
import com.lu4p.fokuslauncher.ui.components.DateBatteryRow
import com.lu4p.fokuslauncher.ui.components.FokusBottomSheet
import com.lu4p.fokuslauncher.ui.components.FokusOutlinedButton
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.components.SheetActionRow
import com.lu4p.fokuslauncher.ui.components.WeatherWidget
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.LocalSystemClickSound
import com.lu4p.fokuslauncher.utils.LockScreenHelper

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {},
    onOpenEditHomeApps: () -> Unit = {},
    onOpenEditShortcuts: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockUiState by viewModel.clockUiState.collectAsStateWithLifecycle()
    val weatherUiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val rightSideShortcuts by viewModel.rightSideShortcuts.collectAsStateWithLifecycle()
    val allInstalledApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val showWeatherAppPicker by viewModel.showWeatherAppPicker.collectAsStateWithLifecycle()
    val appMenuTarget by viewModel.appMenuTarget.collectAsStateWithLifecycle()
    val showHomeScreenMenu by viewModel.showHomeScreenMenu.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onFavoriteClick = viewModel::launchFavorite
    val onFavoriteLongPress = viewModel::onFavoriteLongPress
    val onHomeLongPress = viewModel::onHomeScreenLongPress
    val onShortcutClick = viewModel::launchShortcut
    val onSetDefaultLauncher = viewModel::openDefaultLauncherSettings
    val onClockClick = viewModel::openClockApp
    val onDateClick = viewModel::openCalendarApp
    val onWeatherClick = viewModel::openWeatherAppPicker
    val onDoubleTapEmptyLock = viewModel::onDoubleTapEmptyLock

    LaunchedEffect(viewModel) {
        viewModel.requestLockAccessibilitySettings.collect {
            LockScreenHelper.openAccessibilitySettings(context)
        }
    }

    OnResumeEffect(lifecycleOwner, viewModel, alsoRunIfAlreadyResumed = true) {
        viewModel.refreshInstalledApps(forceReload = false)
        viewModel.recheckDefaultLauncher()
        viewModel.refreshDoubleTapLockEffective()
        viewModel.refreshWeather()
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            clockUiState = clockUiState,
            weatherUiState = weatherUiState,
            favorites = favorites,
            installedApps = allInstalledApps,
            rightSideShortcuts = rightSideShortcuts,
            onLabelClick = onFavoriteClick,
            onLabelLongPress = onFavoriteLongPress,
            onHomeScreenLongPress = onHomeLongPress,
            onIconClick = onShortcutClick,
            onSetDefaultLauncher = onSetDefaultLauncher,
            onClockClick = onClockClick,
            onDateClick = onDateClick,
            onWeatherClick = onWeatherClick,
            doubleTapEmptyLockEnabled = uiState.doubleTapEmptyLockEnabled,
            onDoubleTapEmptyLock = onDoubleTapEmptyLock,
        )
    }

    // ── Dialogs & sheets (render as overlay windows) ────────────────

    // App menu bottom sheet (opened directly on long-press)
    appMenuTarget?.let { fav ->
        HomeAppMenuSheet(
            fav = fav,
            onDismiss = { viewModel.dismissAppMenu() },
            onRename = { newName -> viewModel.renameApp(fav, newName) },
            onRemoveFromHome = { viewModel.removeFavorite(fav) },
            onEditHomeScreen = {
                viewModel.dismissAppMenu()
                onOpenEditHomeApps()
            },
            onAppInfo = { viewModel.openAppInfo(fav) },
            onHide = { viewModel.hideApp(fav) },
            onUninstall = { viewModel.uninstallApp(fav) }
        )
    }

    if (showHomeScreenMenu) {
        HomeScreenLongPressSheet(
            onDismiss = { viewModel.dismissHomeScreenMenu() },
            onEditHomeScreen = {
                viewModel.dismissHomeScreenMenu()
                onOpenEditHomeApps()
            },
            onEditShortcuts = {
                viewModel.dismissHomeScreenMenu()
                onOpenEditShortcuts()
            },
            onOpenSettings = {
                viewModel.dismissHomeScreenMenu()
                onOpenSettings()
            }
        )
    }

    if (showWeatherAppPicker) {
        GroupedAppPickerDialog(
            apps = allInstalledApps,
            title = stringResource(R.string.home_weather_app_picker_title),
            keyPrefix = "weather_app_pick",
            onSelect = { app -> viewModel.setPreferredWeatherApp(app.packageName) },
            onDismiss = { viewModel.closeWeatherAppPicker() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    clockUiState: HomeClockUiState,
    weatherUiState: HomeWeatherUiState,
    favorites: List<FavoriteApp>,
    rightSideShortcuts: List<HomeShortcut>,
    onLabelClick: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
    modifier: Modifier = Modifier,
    installedApps: List<AppInfo> = emptyList(),
    onLabelLongPress: (FavoriteApp) -> Unit = {},
    onHomeScreenLongPress: () -> Unit = {},
    onSetDefaultLauncher: () -> Unit = {},
    onClockClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    doubleTapEmptyLockEnabled: Boolean = false,
    onDoubleTapEmptyLock: () -> Unit = {},
) {
    val play = LocalSystemClickSound.current
    val noIndication = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .combinedClickable(
                indication = null,
                interactionSource = noIndication,
                onClick = { },
                onLongClick = onHomeScreenLongPress
            )
            .testTag("home_screen")
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding()
                .padding(top = 48.dp)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
        ) {
            HomeWidgetsSection(
                uiState = uiState,
                clockUiState = clockUiState,
                weatherUiState = weatherUiState,
                onClockClick = onClockClick,
                onDateClick = onDateClick,
                onWeatherClick = onWeatherClick
            )

            // Push favorites to the bottom; optional double-tap to lock on this empty band
            if (doubleTapEmptyLockEnabled) {
                val emptyTapSource = remember { MutableInteractionSource() }
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                                indication = null,
                                                interactionSource = emptyTapSource,
                                                onClick = {},
                                                onLongClick = onHomeScreenLongPress,
                                                onDoubleClick = {
                                                    play()
                                                    onDoubleTapEmptyLock()
                                                },
                                        )
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            HomeFavoritesSection(
                homeAlignment = uiState.homeAlignment,
                favorites = favorites,
                installedApps = installedApps,
                rightSideShortcuts = rightSideShortcuts,
                launcherFontScale = uiState.launcherFontScale,
                onLabelClick = onLabelClick,
                onLabelLongPress = onLabelLongPress,
                onIconClick = onIconClick
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        HomeDefaultLauncherBanner(
            isDefaultLauncher = uiState.isDefaultLauncher,
            onSetDefaultLauncher = onSetDefaultLauncher
        )
    }
}

/**
 * Clock [TopStart], weather [TopEnd] on the full content width. Baseline-based placement was wrong:
 * the clock’s first baseline sits far below the top, which shoved weather down into the AM/PM
 * cluster. A small top inset on weather matches roughly where [displayLarge] glyphs start.
 */
@Composable
private fun HomeClockWeatherHeader(
        clockUiState: HomeClockUiState,
        weatherUiState: HomeWeatherUiState,
        showWeather: Boolean,
        onClockClick: () -> Unit,
        onWeatherClick: () -> Unit,
) {
    val density = LocalDensity.current
    val clockStyle = MaterialTheme.typography.displayLarge
    val weatherTopPad =
            remember(clockStyle, density.density, density.fontScale) {
                val lead =
                        ((clockStyle.lineHeight.value - clockStyle.fontSize.value) / 2f)
                                .coerceAtLeast(0f)
                with(density) { lead.sp.toDp() }
            }
    val launcherScale =
            LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
    // Use padding (not offset): offset does not change layout height, so the header Box and
    // parents can clip or resolve hits as if the weather were still at y=0.
    val weatherLowerInset =
            remember(density.density, density.fontScale, launcherScale) {
                with(density) { (10f * launcherScale).sp.toDp() } + 8.dp
            }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (showWeather) {
            WeatherWidget(
                    weather = weatherUiState.weather,
                    useFahrenheit = weatherUiState.weatherUseFahrenheit,
                    prominent = false,
                    onClick = onWeatherClick,
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .padding(top = weatherTopPad + weatherLowerInset),
            )
        }
        ClockWidget(
                time = clockUiState.currentTime,
                is24HourFormat = clockUiState.is24HourFormat,
                onClick = onClockClick,
                modifier = Modifier.align(Alignment.TopStart).testTag("clock_widget"),
        )
    }
}

@Composable
private fun HomeWidgetsSection(
    uiState: HomeUiState,
    clockUiState: HomeClockUiState,
    weatherUiState: HomeWeatherUiState,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
) {
    val showClock = uiState.showHomeClock
    val showWeather = uiState.showHomeWeather && weatherUiState.showWeatherWidget
    val showDateOrBattery = uiState.showHomeDate || uiState.showHomeBattery

    when {
        showClock -> {
            HomeClockWeatherHeader(
                    clockUiState = clockUiState,
                    weatherUiState = weatherUiState,
                    showWeather = showWeather,
                    onClockClick = onClockClick,
                    onWeatherClick = onWeatherClick,
            )
        }
        showWeather -> {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                WeatherWidget(
                        weather = weatherUiState.weather,
                        useFahrenheit = weatherUiState.weatherUseFahrenheit,
                        prominent = false,
                        onClick = onWeatherClick,
                )
            }
        }
    }

    if (showDateOrBattery) {
        DateBatteryRow(
                date = clockUiState.currentDate,
                batteryPercent = clockUiState.batteryPercent,
                showDate = uiState.showHomeDate,
                showBattery = uiState.showHomeBattery,
                onDateClick = onDateClick,
                modifier = Modifier.fillMaxWidth().testTag("date_battery_row"),
        )
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteApp>,
    installedApps: List<AppInfo>,
    horizontalAlignment: Alignment.Horizontal,
    onLabelClick: (FavoriteApp) -> Unit,
    onLabelLongPress: (FavoriteApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        favorites.forEach { fav ->
            FavoriteAppItem(
                fav = fav,
                installedApps = installedApps,
                onClick = { onLabelClick(fav) },
                onLongPress = { onLabelLongPress(fav) },
                horizontalAlignment = horizontalAlignment,
            )
        }
    }
}

@Composable
private fun ShortcutIconsColumn(
    shortcuts: List<HomeShortcut>,
    onIconClick: (HomeShortcut) -> Unit,
    iconSize: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.wrapContentHeight(align = Alignment.Bottom),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RightShortcutIcons(
                shortcuts = shortcuts,
                onIconClick = onIconClick,
                iconSize = iconSize,
        )
    }
}

@Composable
private fun HomeFavoritesSection(
    homeAlignment: HomeAlignment,
    favorites: List<FavoriteApp>,
    installedApps: List<AppInfo>,
    rightSideShortcuts: List<HomeShortcut>,
    launcherFontScale: Float,
    onLabelClick: (FavoriteApp) -> Unit,
    onLabelLongPress: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
) {
    val sc =
            launcherFontScale.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
    val shortcutIconSize = (24f * sc).dp
    val shortcutIconSpacingH = (24f * sc).dp
    val shortcutIconSpacingV = (20f * sc).dp
    val shortcutGutter = (24f * sc).dp
    val shortcutRowTopSpacer = (20f * sc).dp

    val listModifier =
        Modifier.fillMaxWidth().testTag("favorites_list")
    when (homeAlignment) {
        HomeAlignment.CENTER ->
            Column(
                modifier = listModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FavoritesList(
                    favorites = favorites,
                    installedApps = installedApps,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    onLabelClick = onLabelClick,
                    onLabelLongPress = onLabelLongPress,
                )
                if (rightSideShortcuts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(shortcutRowTopSpacer))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(shortcutIconSpacingH),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RightShortcutIcons(
                                shortcuts = rightSideShortcuts,
                                onIconClick = onIconClick,
                                iconSize = shortcutIconSize,
                        )
                    }
                }
            }

        HomeAlignment.LEFT, HomeAlignment.RIGHT -> {
            val favAlign =
                    if (homeAlignment == HomeAlignment.LEFT) Alignment.Start else Alignment.End
            Row(
                    modifier = listModifier,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
            ) {
                val favs: @Composable () -> Unit = {
                    FavoritesList(
                            favorites = favorites,
                            installedApps = installedApps,
                            horizontalAlignment = favAlign,
                            onLabelClick = onLabelClick,
                            onLabelLongPress = onLabelLongPress,
                            modifier = Modifier.weight(1f),
                    )
                }
                val icons: @Composable () -> Unit = {
                    ShortcutIconsColumn(
                            shortcuts = rightSideShortcuts,
                            onIconClick = onIconClick,
                            iconSize = shortcutIconSize,
                            verticalSpacing = shortcutIconSpacingV,
                    )
                }
                if (homeAlignment == HomeAlignment.LEFT) {
                    favs()
                    Spacer(modifier = Modifier.width(shortcutGutter))
                    icons()
                } else {
                    icons()
                    Spacer(modifier = Modifier.width(shortcutGutter))
                    favs()
                }
            }
        }
    }
}

@Composable
private fun RightShortcutIcons(
    shortcuts: List<HomeShortcut>,
    onIconClick: (HomeShortcut) -> Unit,
    iconSize: Dp,
) {
    shortcuts.reversed().forEachIndexed { index, shortcut ->
        LauncherIcon(
                imageVector = MinimalIcons.iconFor(shortcut.iconName),
                contentDescription = stringResource(R.string.cd_shortcut_icon),
                tint = MaterialTheme.colorScheme.onBackground,
                iconSize = iconSize,
                modifier =
                        Modifier.clickableNoRippleWithSystemSound { onIconClick(shortcut) }
                                .testTag("right_shortcut_icon_$index"),
        )
    }
}

@Composable
private fun BoxScope.HomeDefaultLauncherBanner(
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit,
) {
    if (isDefaultLauncher) return

    FokusOutlinedButton(
        onClick = onSetDefaultLauncher,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp)
            .testTag("set_default_launcher_button")
    ) {
        Text(
            text = stringResource(R.string.home_set_default_launcher),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppItem(
        fav: FavoriteApp,
        installedApps: List<AppInfo>,
        onClick: () -> Unit,
        onLongPress: () -> Unit,
        horizontalAlignment: Alignment.Horizontal,
) {
    val context = LocalContext.current
    val badge =
            remember(fav, installedApps, context) {
                val match =
                        installedApps.find {
                            it.packageName == fav.packageName &&
                                    appProfileKey(it.userHandle) == fav.profileKey
                        }
                profileOriginLabelForFavorite(context, fav, match)
            }
    Column(
            horizontalAlignment = horizontalAlignment,
            modifier =
                    Modifier.combinedClickableWithSystemSound(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onClick,
                                    onLongClick = onLongPress,
                            )
                            .testTag("favorite_${fav.label}"),
    ) {
        Text(
                text = fav.label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
        )
        if (badge != null) {
            Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenLongPressSheet(
    onDismiss: () -> Unit,
    onEditHomeScreen: () -> Unit,
    onEditShortcuts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    FokusBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
            SheetActionRow(
                label = stringResource(R.string.settings_edit_home_screen),
                onClick = onEditHomeScreen,
                icon = Icons.Default.Home,
                iconContentDescription = stringResource(R.string.cd_edit_home_screen),
            )
            SheetActionRow(
                label = stringResource(R.string.settings_edit_shortcuts),
                onClick = onEditShortcuts,
                icon = Icons.Filled.TouchApp,
                iconContentDescription = stringResource(R.string.settings_edit_shortcuts),
            )
            SheetActionRow(
                label = stringResource(R.string.settings_title),
                onClick = onOpenSettings,
                icon = Icons.Default.Settings,
                iconContentDescription = stringResource(R.string.cd_settings),
            )
    }
}

