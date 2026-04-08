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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.lu4p.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedAppItems
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForFavorite
import com.lu4p.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.ui.components.ClockWidget
import com.lu4p.fokuslauncher.ui.components.DateBatteryRow
import com.lu4p.fokuslauncher.ui.components.FokusOutlinedButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.components.WeatherWidget
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.LocalSystemClickSound
import com.lu4p.fokuslauncher.utils.LockScreenHelper
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

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
            onRename = { newName -> viewModel.renameApp(fav.packageName, newName) },
            onRemoveFromHome = { viewModel.removeFavorite(fav) },
            onEditHomeScreen = {
                viewModel.dismissAppMenu()
                onOpenEditHomeApps()
            },
            onAppInfo = { viewModel.openAppInfo(fav.packageName) },
            onHide = { viewModel.hideApp(fav.packageName) },
            onUninstall = { viewModel.uninstallApp(fav.packageName) }
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
        WeatherAppPickerDialog(
            allApps = allInstalledApps,
            onSelect = { packageName -> viewModel.setPreferredWeatherApp(packageName) },
            onDismiss = { viewModel.closeWeatherAppPicker() }
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
    installedApps: List<AppInfo> = emptyList(),
    rightSideShortcuts: List<HomeShortcut>,
    onLabelClick: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
    modifier: Modifier = Modifier,
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
private fun HomeFavoritesSection(
    homeAlignment: HomeAlignment,
    favorites: List<FavoriteApp>,
    installedApps: List<AppInfo>,
    rightSideShortcuts: List<HomeShortcut>,
    onLabelClick: (FavoriteApp) -> Unit,
    onLabelLongPress: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
) {
    when (homeAlignment) {
        HomeAlignment.CENTER -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("favorites_list"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                favorites.forEach { fav ->
                    FavoriteAppItem(
                        fav = fav,
                        installedApps = installedApps,
                        onClick = { onLabelClick(fav) },
                        onLongPress = { onLabelLongPress(fav) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                if (rightSideShortcuts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RightShortcutIcons(
                            shortcuts = rightSideShortcuts,
                            onIconClick = onIconClick
                        )
                    }
                }
            }
        }

        HomeAlignment.RIGHT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("favorites_list"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RightShortcutIcons(
                        shortcuts = rightSideShortcuts,
                        onIconClick = onIconClick
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    favorites.forEach { fav ->
                        FavoriteAppItem(
                            fav = fav,
                            installedApps = installedApps,
                            onClick = { onLabelClick(fav) },
                            onLongPress = { onLabelLongPress(fav) },
                            horizontalAlignment = Alignment.End,
                        )
                    }
                }
            }
        }

        HomeAlignment.LEFT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("favorites_list"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    favorites.forEach { fav ->
                        FavoriteAppItem(
                            fav = fav,
                            installedApps = installedApps,
                            onClick = { onLabelClick(fav) },
                            onLongPress = { onLabelLongPress(fav) },
                            horizontalAlignment = Alignment.Start,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RightShortcutIcons(
                        shortcuts = rightSideShortcuts,
                        onIconClick = onIconClick
                    )
                }
            }
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
                .clickableWithSystemSound(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onIconClick(shortcut) }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableWithSystemSound(onClick = {
                        onEditHomeScreen()
                    })
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.cd_edit_home_screen),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_edit_home_screen),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableWithSystemSound(onClick = {
                        onEditShortcuts()
                    })
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = stringResource(R.string.settings_edit_shortcuts),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_edit_shortcuts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableWithSystemSound(onClick = {
                        onOpenSettings()
                    })
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun WeatherAppPickerDialog(
    allApps: List<AppInfo>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val context = LocalContext.current
    val filtered =
        remember(filter, allApps) {
            if (filter.isBlank()) allApps
            else allApps.filter { it.label.containsNormalizedSearch(filter) }
        }
    val filteredSections =
        remember(filtered, context) {
            groupAppsIntoProfileSections(context, filtered, ::sortAppsAlphabeticallyByProfileSection)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.home_weather_app_picker_title), color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    profileGroupedAppItems(
                        sections = filteredSections,
                        keyPrefix = "weather_app_pick",
                        horizontalPadding = 8.dp,
                    ) { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableWithSystemSound { onSelect(app.packageName) }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            FokusTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
