package com.lu4p.fokuslauncher.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ShortcutTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onEditHomeScreen: () -> Unit = {},
        onEditRightShortcuts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog states
    var showAppPickerFor by remember { mutableStateOf<String?>(null) } // swipeLeft/swipeRight
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().testTag("settings_screen")) {
        TopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors =
                        TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                        )
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ========== HOME SCREEN APPS ==========
            item { SectionHeader("Home Screen Apps") }

            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable(onClick = onEditHomeScreen)
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                            text = "Edit home screen",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                val context = LocalContext.current
                val activity = context as? android.app.Activity
                val hasLocationPermission =
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                val locationPermissionLauncher =
                        rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                        ) { granted ->
                                if (!granted &&
                                                activity != null &&
                                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                                        activity,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                )) {
                                        context.startActivity(
                                                Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                                )
                                                        .apply {
                                                                data =
                                                                        Uri.fromParts(
                                                                                "package",
                                                                                context.packageName,
                                                                                null
                                                                        )
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                        )
                                }
                        }
                Column {
                    if (!hasLocationPermission) {
                        LocationWeatherRow(onEnableClick = {
                                locationPermissionLauncher.launch(
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                        })
                    } else {
                        val weatherAppLabel =
                                formatWeatherAppLabel(
                                        context,
                                        uiState.preferredWeatherAppPackage,
                                        uiState.allApps
                                )
                        ShortcutTargetRow(
                                label = "Weather app",
                                currentTarget = weatherAppLabel,
                                onPickApp = { showAppPickerFor = "weather" },
                                onClear = { viewModel.setPreferredWeatherApp("") }
                        )
                    }
                }
            }

            item { SettingsDivider() }

            // ========== SWIPE SHORTCUTS ==========
            item { SectionHeader("Swipe Shortcuts") }

            item {
                ShortcutTargetRow(
                        label = "Swipe left",
                        currentTarget =
                                formatShortcutTarget(uiState.swipeLeftTarget, uiState.allApps),
                        onPickApp = { showAppPickerFor = "swipeLeft" },
                        onClear = { viewModel.setSwipeLeftTarget(null) }
                )
            }

            item {
                ShortcutTargetRow(
                        label = "Swipe right",
                        currentTarget =
                                formatShortcutTarget(uiState.swipeRightTarget, uiState.allApps),
                        onPickApp = { showAppPickerFor = "swipeRight" },
                        onClear = { viewModel.setSwipeRightTarget(null) }
                )
            }

            item { SettingsDivider() }

            // ========== RIGHT-SIDE ICON SHORTCUTS ==========
            item { SectionHeader("Right-side icon shortcuts") }
            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable(onClick = onEditRightShortcuts)
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Edit right-side shortcuts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                text = "${uiState.rightSideShortcuts.size} shortcuts configured",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item { SettingsDivider() }

            // ========== HIDDEN APPS ==========
            item { SectionHeader("Hidden Apps") }

            if (uiState.hiddenApps.isEmpty()) {
                item {
                    Text(
                            text = "No hidden apps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.hiddenApps) { hiddenApp ->
                    HiddenAppRow(
                            app = hiddenApp,
                            onUnhide = { viewModel.unhideApp(hiddenApp.packageName) }
                    )
                }
            }

            item { SettingsDivider() }

            // ========== RENAMED APPS ==========
            item { SectionHeader("Renamed Apps") }

            if (uiState.renamedApps.isEmpty()) {
                item {
                    Text(
                            text = "No renamed apps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.renamedApps) { renamedApp ->
                    RenamedAppRow(
                            packageName = renamedApp.packageName,
                            customName = renamedApp.customName,
                            onRemoveRename = { viewModel.removeRename(renamedApp.packageName) }
                    )
                }
            }

            item { SettingsDivider() }

            // ========== RESET ==========
            item { SectionHeader("Data") }
            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable(onClick = { showResetConfirm = true })
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Icon(
                            Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                            text = "Reset all data",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // --- Dialogs ---

    if (showResetConfirm) {
        AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset all data?", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Text(
                            "This will clear favorites, shortcuts, hidden apps, renamed apps, and all other settings. The app will return to its initial state.",
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.resetAllState()
                                showResetConfirm = false
                                onNavigateBack()
                            }
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // App picker dialog (used for swipe shortcuts)
    if (showAppPickerFor != null) {
        AppPickerDialog(
                allApps = uiState.allApps,
                onSelect = { packageName ->
                    val targetKey = showAppPickerFor ?: return@AppPickerDialog
                    when (targetKey) {
                        "swipeLeft" -> viewModel.setSwipeLeftTarget(ShortcutTarget.App(packageName))
                        "swipeRight" ->
                                viewModel.setSwipeRightTarget(ShortcutTarget.App(packageName))
                        "weather" -> viewModel.setPreferredWeatherApp(packageName)
                    }
                    showAppPickerFor = null
                },
                onDismiss = { showAppPickerFor = null }
        )
    }
}

// =========================  SUB-COMPOSABLES  =========================

@Composable
private fun SectionHeader(title: String) {
    Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    Spacer(Modifier.height(16.dp))
}

// --- Location for weather row (shown only when permission disabled) ---

@Composable
private fun LocationWeatherRow(onEnableClick: () -> Unit) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onEnableClick)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = stringResource(R.string.settings_weather_location_disabled),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    text = stringResource(R.string.settings_weather_location_disabled_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        TextButton(onClick = onEnableClick) {
            Text(stringResource(R.string.settings_weather_location_enable_button))
        }
    }
}

// --- Swipe shortcut row ---

@Composable
private fun ShortcutTargetRow(
        label: String,
        currentTarget: String,
        onPickApp: () -> Unit,
        onClear: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    currentTarget,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        TextButton(onClick = onPickApp) { Text("Change") }
        IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
            Icon(
                    Icons.Default.Close,
                    "Clear",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
            )
        }
    }
}

// --- Hidden / Renamed rows (unchanged) ---

@Composable
private fun HiddenAppRow(app: HiddenAppInfo, onUnhide: () -> Unit) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onUnhide)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Visibility, "Unhide", tint = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun RenamedAppRow(packageName: String, customName: String, onRemoveRename: () -> Unit) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onRemoveRename)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    customName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    packageName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Close, "Remove rename", tint = MaterialTheme.colorScheme.secondary)
    }
}

// =====================  DIALOGS  =====================

@Composable
private fun AppPickerDialog(
        allApps: List<AppInfo>,
        onSelect: (String) -> Unit,
        onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select App", color = MaterialTheme.colorScheme.onBackground) },
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
                    val filtered =
                            if (filter.isBlank()) allApps
                            else allApps.filter { it.label.contains(filter, true) }
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filtered) { app ->
                            Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable { onSelect(app.packageName) }
                                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun formatWeatherAppLabel(
        context: Context,
        packageName: String,
        allApps: List<AppInfo>
): String {
    if (packageName.isNotBlank()) {
        return allApps.find { it.packageName == packageName }?.label ?: packageName
    }
    val weatherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_WEATHER)
    }
    val hasSystemWeatherApp =
            weatherIntent.resolveActivity(context.packageManager) != null
    return if (hasSystemWeatherApp) {
        context.getString(R.string.settings_weather_app_system_default)
    } else {
        context.getString(R.string.settings_weather_app_not_configured)
    }
}

private fun formatShortcutTarget(target: ShortcutTarget?, allApps: List<AppInfo>): String {
    return when (target) {
        null -> "Not set"
        is ShortcutTarget.App -> allApps.find { it.packageName == target.packageName }?.label
                        ?: target.packageName
        is ShortcutTarget.DeepLink -> "Deep link"
        is ShortcutTarget.LauncherShortcut -> {
            val appLabel =
                    allApps.find { it.packageName == target.packageName }?.label
                            ?: target.packageName
            "$appLabel - Shortcut"
        }
    }
}

