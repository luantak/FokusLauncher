package com.lu4p.fokuslauncher.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.lu4p.fokuslauncher.R
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedAppItems
import com.lu4p.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.utils.BatteryOptimizationHelper
import com.lu4p.fokuslauncher.utils.LockScreenHelper
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onNavigateToHome: () -> Unit = {},
        onEditHomeScreen: () -> Unit = {},
        onEditRightShortcuts: () -> Unit = {},
        onOpenDeviceControlSettings: () -> Unit = {},
        onEditCategories: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installedFontFamilies by viewModel.installedFontFamilies.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCoarseLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCoarseLocationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Dialog states
    val showAppPickerFor = remember { mutableStateOf<String?>(null) } // swipeLeft/swipeRight
    val showResetConfirm = remember { mutableStateOf(false) }
    var showHomeWidgetsDialog by remember { mutableStateOf(false) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSystemWallpaper(it)
            onNavigateToHome()
        }
    }

    val locationPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
            ) {
                hasCoarseLocationPermission =
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                if (!hasCoarseLocationPermission &&
                                activity != null &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                )) {
                        context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
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

    Column(modifier = Modifier
        .fillMaxSize()
        .background(backgroundScrim)
        .testTag("settings_screen")
    ) {
        TopAppBar(
                title = {
                    Text(stringResource(R.string.settings_title), color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
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

            // ========== APPEARANCE ==========
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }

            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_status_bar),
                        checked = uiState.showStatusBar,
                        onCheckedChange = { viewModel.setShowStatusBar(it) }
                )
            }

            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_allow_landscape_rotation),
                        checked = uiState.allowLandscapeRotation,
                        onCheckedChange = { viewModel.setAllowLandscapeRotation(it) }
                )
            }

            item {
                AppLanguageDropdown(
                        currentTag = uiState.appLocaleTag,
                        onTagSelected = { tag -> viewModel.setAppLocaleTag(tag) }
                )
            }

            item {
                LauncherFontFamilyDropdown(
                        currentFamilyName = uiState.launcherFontFamilyName,
                        installedFamilies = installedFontFamilies,
                        onFamilySelected = { viewModel.setLauncherFontFamilyName(it) }
                )
            }

            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wallpaperPickerLauncher.launch("image/*") }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                            text = stringResource(R.string.settings_set_background_image),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBlackWallpaper()
                                    onNavigateToHome()
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                            text = stringResource(R.string.settings_set_black_wallpaper),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SettingsDivider() }

            // ========== HOME SCREEN ==========
            item { SectionHeader(stringResource(R.string.settings_section_home_screen)) }

            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { showHomeWidgetsDialog = true }
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(R.string.settings_home_widgets),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                text = homeWidgetsSummaryText(uiState),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item {
                SettingsActionRow(
                        label = stringResource(R.string.settings_accessibility),
                        subtitle = stringResource(R.string.settings_accessibility_subtitle),
                        onClick = onOpenDeviceControlSettings
                )
            }

            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable(onClick = onEditHomeScreen)
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                            text = stringResource(R.string.settings_edit_home_screen),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                }
            }

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
                                text = stringResource(R.string.settings_edit_shortcuts),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                text =
                                        pluralStringResource(
                                                R.plurals.settings_shortcuts_configured,
                                                uiState.rightSideShortcuts.size,
                                                uiState.rightSideShortcuts.size
                                        ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Home screen alignment picker
            item {
                HomeAlignmentRow(
                        currentAlignment = uiState.homeAlignment,
                        onAlignmentChanged = { viewModel.setHomeAlignment(it) }
                )
            }

            item {
                Column {
                    if (!hasCoarseLocationPermission) {
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
                                label = stringResource(R.string.settings_weather_app),
                                currentTarget = weatherAppLabel,
                                onPickApp = { showAppPickerFor.value = "weather" },
                                onClear = { viewModel.setPreferredWeatherApp("") }
                        )
                    }
                }
            }

            item {
                ShortcutTargetRow(
                        label = stringResource(R.string.settings_swipe_left),
                        currentTarget =
                                formatShortcutTarget(context, uiState.swipeLeftTarget, uiState.allApps),
                        onPickApp = { showAppPickerFor.value = "swipeLeft" },
                        onClear = { viewModel.setSwipeLeftTarget(null) }
                )
            }

            item {
                ShortcutTargetRow(
                        label = stringResource(R.string.settings_swipe_right),
                        currentTarget =
                                formatShortcutTarget(context, uiState.swipeRightTarget, uiState.allApps),
                        onPickApp = { showAppPickerFor.value = "swipeRight" },
                        onClear = { viewModel.setSwipeRightTarget(null) }
                )
            }

            item { SettingsDivider() }

            // ========== APP CATEGORIES ==========
            item { SectionHeader(stringResource(R.string.settings_section_app_categories)) }
            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable(onClick = onEditCategories)
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(R.string.settings_edit_app_categories),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                text =
                                        pluralStringResource(
                                                R.plurals.settings_categories_count,
                                                uiState.categoryDefinitions.size,
                                                uiState.categoryDefinitions.size
                                        ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item { SettingsDivider() }

            // ========== APP DRAWER ==========
            item { SectionHeader(stringResource(R.string.settings_section_app_drawer)) }

            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_sidebar_categories),
                        subtitle = stringResource(R.string.settings_drawer_sidebar_categories_subtitle),
                        checked = uiState.drawerSidebarCategories,
                        onCheckedChange = { viewModel.setDrawerSidebarCategories(it) }
                )
            }

            if (uiState.drawerSidebarCategories) {
                item {
                    DrawerCategoryRailSideRow(
                            railOnLeft = uiState.drawerCategorySidebarOnLeft,
                            onRailOnLeftChanged = { viewModel.setDrawerCategorySidebarOnLeft(it) }
                    )
                }
            }

            item {
                DrawerAppSortRow(
                        currentMode = uiState.drawerAppSortMode,
                        onModeChanged = { viewModel.setDrawerAppSortMode(it) }
                )
            }

            item { SettingsDivider() }

            // ========== HIDDEN APPS ==========
            item { SectionHeader(stringResource(R.string.settings_section_hidden_apps)) }

            if (uiState.hiddenApps.isEmpty()) {
                item {
                    Text(
                            text = stringResource(R.string.settings_no_hidden_apps),
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
            item { SectionHeader(stringResource(R.string.settings_section_renamed_apps)) }

            if (uiState.renamedApps.isEmpty()) {
                item {
                    Text(
                            text = stringResource(R.string.settings_no_renamed_apps),
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

            // ========== CONNECT ==========
            item { SectionHeader(stringResource(R.string.settings_connect_section)) }

            item {
                ExternalLinkRow(
                    icon = Icons.Filled.Star,
                    title = stringResource(R.string.settings_github_title),
                    subtitle = stringResource(R.string.settings_github_subtitle),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/luantak/FokusLauncher".toUri())
                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        )
                    }
                )
            }

            item {
                ExternalLinkRow(
                    icon = Icons.Filled.ChatBubble,
                    title = stringResource(R.string.settings_matrix_title),
                    subtitle = stringResource(R.string.settings_matrix_subtitle),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://matrix.to/#/#fokus:matrix.org".toUri())
                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        )
                    }
                )
            }

            item { SettingsDivider() }

            // ========== RESET ==========
            item { SectionHeader(stringResource(R.string.settings_section_data)) }

            item {
                val scope = rememberCoroutineScope()
                val activity = LocalActivity.current
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val shareIntent = viewModel.createLogShareIntent()
                                                if (shareIntent != null && activity != null) {
                                                    activity.startActivity(
                                                            Intent.createChooser(
                                                                    shareIntent,
                                                                    context.getString(
                                                                            R.string.settings_export_logs_share_chooser
                                                                    )
                                                            )
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                                    context,
                                                                    context.getString(
                                                                            R.string.toast_export_logs_failed
                                                                    ),
                                                                    Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(R.string.settings_export_logs_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                text = stringResource(R.string.settings_export_logs_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable(onClick = { showResetConfirm.value = true })
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
                            text = stringResource(R.string.settings_reset_all_data),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // --- Dialogs ---

    if (showResetConfirm.value) {
        AlertDialog(
                onDismissRequest = { showResetConfirm.value = false },
                title = {
                    Text(stringResource(R.string.settings_reset_confirm_title), color = MaterialTheme.colorScheme.onBackground)
                },
                text = {
                    Text(
                            stringResource(R.string.settings_reset_confirm_message),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                confirmButton = {
                    val scope = rememberCoroutineScope()
                    TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.resetAllState()
                                    showResetConfirm.value = false
                                    onNavigateBack()
                                }
                            }
                    ) {
                        Text(stringResource(R.string.action_reset), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm.value = false }) {
                        Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showHomeWidgetsDialog) {
        HomeWidgetsSettingsDialog(
                showClock = uiState.showHomeClock,
                showDate = uiState.showHomeDate,
                showWeather = uiState.showHomeWeather,
                showBattery = uiState.showHomeBattery,
                onClockChange = viewModel::setShowHomeClock,
                onDateChange = viewModel::setShowHomeDate,
                onWeatherChange = viewModel::setShowHomeWeather,
                onBatteryChange = viewModel::setShowHomeBattery,
                onDismiss = { showHomeWidgetsDialog = false }
        )
    }

    // App picker dialog (used for swipe shortcuts)
    showAppPickerFor.value?.let { pickerTarget ->
        AppPickerDialog(
                allApps = uiState.allApps,
                onSelect = { packageName ->
                    when (pickerTarget) {
                        "swipeLeft" -> viewModel.setSwipeLeftTarget(ShortcutTarget.App(packageName))
                        "swipeRight" ->
                                viewModel.setSwipeRightTarget(ShortcutTarget.App(packageName))
                        "weather" -> viewModel.setPreferredWeatherApp(packageName)
                    }
                    showAppPickerFor.value = null
                },
                onDismiss = { showAppPickerFor.value = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var accessibilityResumeTick by remember { mutableIntStateOf(0) }
    var ignoringBatteryOptimizations by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var pendingEnableLongLock by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityResumeTick++
                ignoringBatteryOptimizations =
                        BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val lockAccessibilityOn =
            remember(accessibilityResumeTick) {
                LockScreenHelper.isLockAccessibilityServiceEnabled(context)
            }

    LaunchedEffect(lockAccessibilityOn, ignoringBatteryOptimizations, uiState.longLockReturnHome) {
        if (uiState.longLockReturnHome &&
                        (!lockAccessibilityOn || !ignoringBatteryOptimizations)) {
            viewModel.setLongLockReturnHome(false)
        }
    }

    LaunchedEffect(lockAccessibilityOn, ignoringBatteryOptimizations, pendingEnableLongLock) {
        if (!pendingEnableLongLock) return@LaunchedEffect
        if (lockAccessibilityOn && ignoringBatteryOptimizations) {
            viewModel.setLongLockReturnHome(true)
        }
        pendingEnableLongLock = false
    }

    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundScrim)
                    .testTag("device_control_settings_screen")
    ) {
        TopAppBar(
                title = {
                    Text(
                            stringResource(R.string.settings_accessibility_page_title),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
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
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_accessibility_permission),
                        subtitle =
                                stringResource(
                                        if (lockAccessibilityOn) {
                                            R.string.settings_accessibility_permission_enabled
                                        } else {
                                            R.string.settings_accessibility_permission_disabled
                                        }
                                ),
                        checked = lockAccessibilityOn,
                        onCheckedChange = { LockScreenHelper.openAccessibilitySettings(context) }
                )
            }

            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_double_tap_to_lock),
                        subtitle =
                                stringResource(R.string.settings_double_tap_to_lock_subtitle),
                        checked = uiState.doubleTapEmptyLock,
                        onCheckedChange = { enabled -> viewModel.setDoubleTapEmptyLock(enabled) },
                        enabled = lockAccessibilityOn
                )
            }

            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_return_home_after_long_lock),
                        subtitle =
                                stringResource(
                                        R.string.settings_return_home_after_long_lock_subtitle
                                ),
                        checked = uiState.longLockReturnHome,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                pendingEnableLongLock = false
                                viewModel.setLongLockReturnHome(false)
                            } else if (!ignoringBatteryOptimizations) {
                                pendingEnableLongLock = true
                                BatteryOptimizationHelper.openBatteryOptimizationSettings(context)
                            } else {
                                viewModel.setLongLockReturnHome(true)
                            }
                        },
                        enabled = lockAccessibilityOn
                )
            }

            if (lockAccessibilityOn && ignoringBatteryOptimizations && uiState.longLockReturnHome) {
                item {
                    LongLockThresholdRow(
                            currentMinutes = uiState.longLockReturnHomeThresholdMinutes,
                            onMinutesSelected = viewModel::setLongLockReturnHomeThresholdMinutes
                    )
                }
            }
        }
    }
}

@Composable
private fun homeWidgetsSummaryText(uiState: SettingsUiState): String {
    val clock = stringResource(R.string.settings_show_home_clock)
    val date = stringResource(R.string.settings_show_home_date)
    val weather = stringResource(R.string.settings_show_home_weather)
    val battery = stringResource(R.string.settings_show_home_battery)
    val parts =
            buildList {
                if (uiState.showHomeClock) add(clock)
                if (uiState.showHomeDate) add(date)
                if (uiState.showHomeWeather) add(weather)
                if (uiState.showHomeBattery) add(battery)
            }
    return if (parts.isEmpty()) {
        stringResource(R.string.settings_home_widgets_summary_none)
    } else {
        parts.joinToString(", ")
    }
}

@Composable
private fun HomeWidgetsSettingsDialog(
        showClock: Boolean,
        showDate: Boolean,
        showWeather: Boolean,
        showBattery: Boolean,
        onClockChange: (Boolean) -> Unit,
        onDateChange: (Boolean) -> Unit,
        onWeatherChange: (Boolean) -> Unit,
        onBatteryChange: (Boolean) -> Unit,
        onDismiss: () -> Unit,
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        stringResource(R.string.settings_home_widgets_dialog_title),
                        color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeWidgetDialogSwitchRow(
                            label = stringResource(R.string.settings_show_home_clock),
                            checked = showClock,
                            onCheckedChange = onClockChange,
                    )
                    Spacer(Modifier.height(12.dp))
                    HomeWidgetDialogSwitchRow(
                            label = stringResource(R.string.settings_show_home_date),
                            checked = showDate,
                            onCheckedChange = onDateChange,
                    )
                    Spacer(Modifier.height(12.dp))
                    HomeWidgetDialogSwitchRow(
                            label = stringResource(R.string.settings_show_home_weather),
                            checked = showWeather,
                            onCheckedChange = onWeatherChange,
                    )
                    Spacer(Modifier.height(12.dp))
                    HomeWidgetDialogSwitchRow(
                            label = stringResource(R.string.settings_show_home_battery),
                            checked = showBattery,
                            onCheckedChange = onBatteryChange,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                            stringResource(R.string.action_done),
                            color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun HomeWidgetDialogSwitchRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { onCheckedChange(!checked) }
                            .padding(vertical = 4.dp)
    ) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// =========================  SUB-COMPOSABLES  =========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsToggleRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        subtitle: String? = null,
        enabled: Boolean = true
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(enabled = enabled) { onCheckedChange(!checked) }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                            if (enabled) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
        )
    }
}

private val SettingsPickerCorner = RoundedCornerShape(12.dp)

@Composable
private fun settingsPickerOutlinedFieldColors() =
        OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                disabledTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                disabledBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                cursorColor = MaterialTheme.colorScheme.primary,
        )

@Composable
private fun settingsPickerMenuItemColors() =
        MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                leadingIconColor = MaterialTheme.colorScheme.onBackground,
                trailingIconColor = MaterialTheme.colorScheme.onBackground,
        )

/**
 * Endonym: name of the language written in that language (e.g. English, Polski), independent of
 * app UI locale.
 */
private fun languageAutonym(localeTag: String): String {
    val locale = Locale.forLanguageTag(localeTag)
    val raw = locale.getDisplayLanguage(locale).trim()
    if (raw.isBlank()) return localeTag
    return raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageDropdown(
        currentTag: String,
        onTagSelected: (String) -> Unit
) {
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
    val supportedLocaleTags = remember { listOf("en", "de", "pl") }
    val options =
            remember(systemDefaultLabel) {
                buildList {
                    add("" to systemDefaultLabel)
                    supportedLocaleTags.forEach { tag -> add(tag to languageAutonym(tag)) }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val selectedDisplayText =
            options.find { (tag, _) -> tag == currentTag }?.second
                    ?: if (currentTag.isBlank()) systemDefaultLabel else languageAutonym(currentTag)

    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                    modifier =
                            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                    value = selectedDisplayText,
                    onValueChange = { _ -> },
                    readOnly = true,
                    singleLine = true,
                    shape = SettingsPickerCorner,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = settingsPickerOutlinedFieldColors()
            )
            ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = SettingsPickerCorner,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border =
                            BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                            ),
            ) {
                options.forEach { (storageTag, label) ->
                    DropdownMenuItem(
                            text = {
                                Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            onClick = {
                                onTagSelected(storageTag)
                                expanded = false
                            },
                            colors = settingsPickerMenuItemColors(),
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherFontFamilyDropdown(
        currentFamilyName: String,
        installedFamilies: List<String>,
        onFamilySelected: (String) -> Unit
) {
    val systemDefault = stringResource(R.string.settings_weather_app_system_default)
    val options =
            remember(currentFamilyName, installedFamilies, systemDefault) {
                buildList {
                    add("" to systemDefault)
                    val sorted = installedFamilies.sortedWith(String.CASE_INSENSITIVE_ORDER)
                    sorted.forEach { add(it to it) }
                    val cur = currentFamilyName.trim()
                    if (cur.isNotEmpty() && sorted.none { it.equals(cur, ignoreCase = true) }) {
                        add(cur to cur)
                    }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel =
            options.find { (value, _) -> value == currentFamilyName }?.second
                    ?: currentFamilyName.ifBlank { systemDefault }

    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_font_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                    modifier =
                            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                    value = selectedLabel,
                    onValueChange = { _ -> },
                    readOnly = true,
                    singleLine = true,
                    shape = SettingsPickerCorner,
                    textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily =
                                            composeFontFamilyFromStoredName(currentFamilyName)
                            ),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = settingsPickerOutlinedFieldColors()
            )
            ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = SettingsPickerCorner,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border =
                            BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                            ),
            ) {
                options.forEach { (storageValue, label) ->
                    DropdownMenuItem(
                            text = {
                                Text(
                                        text = label,
                                        style =
                                                MaterialTheme.typography.bodyLarge.copy(
                                                        fontFamily =
                                                                composeFontFamilyFromStoredName(
                                                                        storageValue
                                                                )
                                                ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            onClick = {
                                onFamilySelected(storageValue)
                                expanded = false
                            },
                            colors = settingsPickerMenuItemColors(),
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerCategoryRailSideRow(
        railOnLeft: Boolean,
        onRailOnLeftChanged: (Boolean) -> Unit
) {
    Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_drawer_category_rail_side),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_drawer_category_rail_side_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                    selected = railOnLeft,
                    onClick = { onRailOnLeftChanged(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.settings_drawer_rail_position_left))
            }
            SegmentedButton(
                    selected = !railOnLeft,
                    onClick = { onRailOnLeftChanged(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.settings_drawer_rail_position_right))
            }
        }
    }
}

@Composable
private fun DrawerAppSortRow(
        currentMode: DrawerAppSortMode,
        onModeChanged: (DrawerAppSortMode) -> Unit
) {
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_drawer_app_sort),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_drawer_app_sort_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DrawerAppSortMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                        selected = currentMode == mode,
                        onClick = { onModeChanged(mode) },
                        shape =
                                SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = DrawerAppSortMode.entries.size
                                )
                ) {
                    Text(
                            stringResource(
                                    when (mode) {
                                        DrawerAppSortMode.ALPHABETICAL ->
                                                R.string.settings_drawer_app_sort_alphabetical
                                        DrawerAppSortMode.MOST_OPENED ->
                                                R.string.settings_drawer_app_sort_most_opened
                                    }
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LongLockThresholdRow(
        currentMinutes: Int,
        onMinutesSelected: (Int) -> Unit
) {
    val options = remember { listOf(1, 5, 15, 30) }
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel =
            pluralStringResource(
                    R.plurals.settings_long_lock_duration_minutes,
                    currentMinutes,
                    currentMinutes
            )
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_long_lock_duration),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_long_lock_duration_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                    modifier =
                            Modifier.menuAnchor(
                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                            enabled = true
                                    )
                                    .fillMaxWidth(),
                    value = selectedLabel,
                    onValueChange = { _ -> },
                    readOnly = true,
                    singleLine = true,
                    shape = SettingsPickerCorner,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = settingsPickerOutlinedFieldColors()
            )
            ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = SettingsPickerCorner,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border =
                            BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                            ),
            ) {
                options.forEach { minutes ->
                    DropdownMenuItem(
                            text = {
                                Text(
                                        text =
                                                pluralStringResource(
                                                        R.plurals.settings_long_lock_duration_minutes,
                                                        minutes,
                                                        minutes
                                                ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            onClick = {
                                onMinutesSelected(minutes)
                                expanded = false
                            },
                            colors = settingsPickerMenuItemColors(),
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAlignmentRow(
        currentAlignment: HomeAlignment,
        onAlignmentChanged: (HomeAlignment) -> Unit
) {
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.home_alignment_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.home_alignment_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            HomeAlignment.entries.forEachIndexed { index, alignment ->
                SegmentedButton(
                        selected = currentAlignment == alignment,
                        onClick = { onAlignmentChanged(alignment) },
                        shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HomeAlignment.entries.size
                        )
                ) {
                    Text(
                            stringResource(
                                    when (alignment) {
                                        HomeAlignment.LEFT -> R.string.home_alignment_left
                                        HomeAlignment.CENTER -> R.string.home_alignment_center
                                        HomeAlignment.RIGHT -> R.string.home_alignment_right
                                    }
                            )
                    )
                }
            }
        }
    }
}

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
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 0.75.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsActionRow(
        label: String,
        subtitle: String,
        onClick: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
    }
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
        TextButton(onClick = onPickApp) { Text(stringResource(R.string.action_change)) }
        IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
            Icon(
                    Icons.Default.Close,
                    stringResource(R.string.action_clear),
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
        Icon(
                Icons.Default.Visibility,
                stringResource(R.string.cd_unhide_app),
                tint = MaterialTheme.colorScheme.secondary
        )
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
        Icon(
                Icons.Default.Close,
                stringResource(R.string.cd_remove_rename),
                tint = MaterialTheme.colorScheme.secondary
        )
    }
}

// --- External link row for Connect section ---

@Composable
private fun ExternalLinkRow(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.cd_open_link),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
        )
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
                Text(stringResource(R.string.settings_app_picker_title), color = MaterialTheme.colorScheme.onBackground)
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
                                keyPrefix = "settings_app_pick",
                                horizontalPadding = 8.dp,
                        ) { app ->
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
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
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
    val hasSystemWeatherApp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val weatherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_WEATHER)
        }
        weatherIntent.resolveActivity(context.packageManager) != null
    } else {
        false
    }
    return if (hasSystemWeatherApp) {
        context.getString(R.string.settings_weather_app_system_default)
    } else {
        context.getString(R.string.settings_weather_app_not_configured)
    }
}

private fun formatShortcutTarget(
        context: Context,
        target: ShortcutTarget?,
        allApps: List<AppInfo>
): String {
    return formatShortcutTargetDisplay(
            context = context,
            target = target,
            allApps = allApps,
            notSetLabel = context.getString(R.string.shortcut_target_not_set),
            resolvedLauncherActionLabel = null
    )
}
