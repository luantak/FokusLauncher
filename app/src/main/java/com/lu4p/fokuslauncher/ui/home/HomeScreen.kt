package com.lu4p.fokuslauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.components.ClockWidget
import com.lu4p.fokuslauncher.ui.components.DateBatteryRow
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.components.WeatherWidget

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {},
    onOpenEditHomeApps: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val rightSideShortcuts by viewModel.rightSideShortcuts.collectAsStateWithLifecycle()
    val allInstalledApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val showWeatherAppPicker by viewModel.showWeatherAppPicker.collectAsStateWithLifecycle()
    val appMenuTarget by viewModel.appMenuTarget.collectAsStateWithLifecycle()
    val showHomeScreenMenu by viewModel.showHomeScreenMenu.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshInstalledApps()
                viewModel.recheckDefaultLauncher()
                viewModel.refreshWeather()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            favorites = favorites,
            rightSideShortcuts = rightSideShortcuts,
            onLabelClick = { packageName -> viewModel.launchApp(packageName) },
            onLabelLongPress = { fav -> viewModel.onFavoriteLongPress(fav) },
            onHomeScreenLongPress = { viewModel.onHomeScreenLongPress() },
            onIconClick = { target -> viewModel.launchShortcut(target) },
            onSetDefaultLauncher = { viewModel.openDefaultLauncherSettings() },
            onClockClick = { viewModel.openClockApp() },
            onDateClick = { viewModel.openCalendarApp() },
            onWeatherClick = { viewModel.openWeatherAppPicker() }
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
    favorites: List<FavoriteApp>,
    rightSideShortcuts: List<HomeShortcut>,
    onLabelClick: (String) -> Unit,
    onIconClick: (ShortcutTarget) -> Unit,
    modifier: Modifier = Modifier,
    onLabelLongPress: (FavoriteApp) -> Unit = {},
    onHomeScreenLongPress: () -> Unit = {},
    onSetDefaultLauncher: () -> Unit = {},
    onClockClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {}
) {
    val noIndication = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (uiState.showWallpaper) Color.Transparent else Color.Black)
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
                .padding(top = 80.dp, bottom = 48.dp)
        ) {
            // Top row: Clock on left, Weather on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ClockWidget(
                    time = uiState.currentTime,
                    onClick = onClockClick,
                    modifier = Modifier.testTag("clock_widget")
                )
                if (uiState.showWeatherWidget) {
                    WeatherWidget(
                        weather = uiState.weather,
                        onClick = onWeatherClick,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            // Date + Battery (clickable -> calendar)
            DateBatteryRow(
                date = uiState.currentDate,
                batteryPercent = uiState.batteryPercent,
                onDateClick = onDateClick,
                modifier = Modifier.testTag("date_battery_row")
            )

            // Push favorites to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // Favorite apps: layout depends on alignment setting
            when (uiState.homeAlignment) {
                HomeAlignment.CENTER -> {
                    // Centered layout: labels stacked above icons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("favorites_list"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        favorites.forEach { fav ->
                            Text(
                                text = fav.label,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .combinedClickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { onLabelClick(fav.packageName) },
                                        onLongClick = { onLabelLongPress(fav) }
                                    )
                                    .testTag("favorite_${fav.label}")
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        if (rightSideShortcuts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rightSideShortcuts.reversed().forEachIndexed { index, shortcut ->
                                    Icon(
                                        imageVector = MinimalIcons.iconFor(shortcut.iconName),
                                        contentDescription = "Shortcut icon",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onIconClick(shortcut.target) }
                                            .testTag("right_shortcut_icon_$index")
                                    )
                                }
                            }
                        }
                    }
                }

                HomeAlignment.RIGHT -> {
                    // Swapped: icons on the left, labels on the right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("favorites_list"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left column: icon shortcuts
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            rightSideShortcuts.reversed().forEachIndexed { index, shortcut ->
                                Icon(
                                    imageVector = MinimalIcons.iconFor(shortcut.iconName),
                                    contentDescription = "Shortcut icon",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onIconClick(shortcut.target) }
                                        .testTag("right_shortcut_icon_$index")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // Right column: app labels
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            favorites.forEach { fav ->
                                Text(
                                    text = fav.label,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .combinedClickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = { onLabelClick(fav.packageName) },
                                            onLongClick = { onLabelLongPress(fav) }
                                        )
                                        .testTag("favorite_${fav.label}")
                                )
                            }
                        }
                    }
                }

                HomeAlignment.LEFT -> {
                    // Default: labels left, icons right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("favorites_list"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left column: app labels
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            favorites.forEach { fav ->
                                Text(
                                    text = fav.label,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .combinedClickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = { onLabelClick(fav.packageName) },
                                            onLongClick = { onLabelLongPress(fav) }
                                        )
                                        .testTag("favorite_${fav.label}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // Right column: independent icon shortcuts (first in list = bottom)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            rightSideShortcuts.reversed().forEachIndexed { index, shortcut ->
                                Icon(
                                    imageVector = MinimalIcons.iconFor(shortcut.iconName),
                                    contentDescription = "Shortcut icon",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onIconClick(shortcut.target) }
                                        .testTag("right_shortcut_icon_$index")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // "Set as default launcher" banner at bottom
        if (!uiState.isDefaultLauncher) {
            OutlinedButton(
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
                    text = "Set as default launcher",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenLongPressSheet(
    onDismiss: () -> Unit,
    onEditHomeScreen: () -> Unit,
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
                    .clickable(onClick = {
                        onEditHomeScreen()
                    })
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Edit home screen",
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Edit home screen",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        onOpenSettings()
                    })
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings",
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Weather App", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val filtered = if (filter.isBlank()) allApps else allApps.filter { it.label.contains(filter, true) }
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filtered) { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app.packageName) }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
