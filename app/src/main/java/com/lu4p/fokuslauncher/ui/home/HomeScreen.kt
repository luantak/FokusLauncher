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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.components.SheetActionRow
import com.lu4p.fokuslauncher.ui.components.WeatherWidget
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
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
    val onFavoriteClick = remember(viewModel) { { fav: FavoriteApp -> viewModel.launchFavorite(fav) } }
    val onFavoriteLongPress = remember(viewModel) { { fav: FavoriteApp -> viewModel.onFavoriteLongPress(fav) } }
    val onHomeLongPress = remember(viewModel) { { viewModel.onHomeScreenLongPress() } }
    val onShortcutClick = remember(viewModel) { { shortcut: HomeShortcut -> viewModel.launchShortcut(shortcut) } }
    val onSetDefaultLauncher = remember(viewModel) { { viewModel.openDefaultLauncherSettings() } }
    val onClockClick = remember(viewModel) { { viewModel.openClockApp() } }
    val onDateClick = remember(viewModel) { { viewModel.openCalendarApp() } }
    val onWeatherClick = remember(viewModel) { { viewModel.openWeatherAppPicker() } }
    val onDoubleTapEmptyLock = remember(viewModel) { { viewModel.onDoubleTapEmptyLock() } }

    LaunchedEffect(viewModel) {
        viewModel.requestLockAccessibilitySettings.collect {
            LockScreenHelper.openAccessibilitySettings(context)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val runResumeActions = {
            viewModel.refreshInstalledApps(forceReload = false)
            viewModel.recheckDefaultLauncher()
            viewModel.refreshDoubleTapLockEffective()
            viewModel.refreshWeather()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runResumeActions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Navigating back to Home composes this screen while the activity is already RESUMED,
        // so ON_RESUME is not delivered to a newly registered observer — refresh once now.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            runResumeActions()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                .padding(top = 80.dp)
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
                onLabelClick = onLabelClick,
                onLabelLongPress = onLabelLongPress,
                onIconClick = onIconClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        HomeDefaultLauncherBanner(
            isDefaultLauncher = uiState.isDefaultLauncher,
            onSetDefaultLauncher = onSetDefaultLauncher
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
    if (showClock || showWeather) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                    when {
                        showClock && showWeather -> Arrangement.SpaceBetween
                        showWeather -> Arrangement.End
                        else -> Arrangement.Start
                    },
            verticalAlignment = Alignment.Top
        ) {
            if (showClock) {
                ClockWidget(
                    time = clockUiState.currentTime,
                    onClick = onClockClick,
                    modifier = Modifier.testTag("clock_widget")
                )
            }
            if (showWeather) {
                WeatherWidget(
                    weather = weatherUiState.weather,
                    useFahrenheit = weatherUiState.weatherUseFahrenheit,
                    onClick = onWeatherClick,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }

    if (uiState.showHomeDate || uiState.showHomeBattery) {
        DateBatteryRow(
            date = clockUiState.currentDate,
            batteryPercent = clockUiState.batteryPercent,
            showDate = uiState.showHomeDate,
            showBattery = uiState.showHomeBattery,
            onDateClick = onDateClick,
            modifier = Modifier.testTag("date_battery_row")
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RightShortcutIcons(shortcuts = shortcuts, onIconClick = onIconClick)
    }
}

@Composable
private fun HomeFavoritesSection(
    homeAlignment: HomeAlignment,
    favorites: List<FavoriteApp>,
    installedApps: List<AppInfo>,
    rightSideShortcuts: List<HomeShortcut>,
    onLabelClick: (FavoriteApp) -> Unit,
    onLabelLongPress: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
) {
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RightShortcutIcons(
                            shortcuts = rightSideShortcuts,
                            onIconClick = onIconClick,
                        )
                    }
                }
            }

        HomeAlignment.RIGHT ->
            Row(
                modifier = listModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                ShortcutIconsColumn(
                    shortcuts = rightSideShortcuts,
                    onIconClick = onIconClick,
                )
                Spacer(modifier = Modifier.width(24.dp))
                FavoritesList(
                    favorites = favorites,
                    installedApps = installedApps,
                    horizontalAlignment = Alignment.End,
                    onLabelClick = onLabelClick,
                    onLabelLongPress = onLabelLongPress,
                    modifier = Modifier.weight(1f),
                )
            }

        HomeAlignment.LEFT ->
            Row(
                modifier = listModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                FavoritesList(
                    favorites = favorites,
                    installedApps = installedApps,
                    horizontalAlignment = Alignment.Start,
                    onLabelClick = onLabelClick,
                    onLabelLongPress = onLabelLongPress,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(24.dp))
                ShortcutIconsColumn(
                    shortcuts = rightSideShortcuts,
                    onIconClick = onIconClick,
                )
            }
    }
}

@Composable
private fun RightShortcutIcons(
    shortcuts: List<HomeShortcut>,
    onIconClick: (HomeShortcut) -> Unit,
) {
    shortcuts.reversed().forEachIndexed { index, shortcut ->
        Icon(
            imageVector = MinimalIcons.iconFor(shortcut.iconName),
            contentDescription = stringResource(R.string.cd_shortcut_icon),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(24.dp)
                .clickableNoRippleWithSystemSound { onIconClick(shortcut) }
                .testTag("right_shortcut_icon_$index")
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

