package com.lu4p.fokuslauncher.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.lu4p.fokuslauncher.R
import java.text.Collator
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
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.ui.drawer.GroupedAppPickerDialog
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.HomeDateFormatStyle
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.utils.LockScreenHelper
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import android.app.Activity
import androidx.compose.ui.graphics.vector.ImageVector

private data class CommunityLink(
        val icon: ImageVector,
        val titleRes: Int,
        val subtitleRes: Int,
        val url: String,
)

private val communityLinks =
        listOf(
                CommunityLink(
                        Icons.Filled.Star,
                        R.string.settings_github_title,
                        R.string.settings_github_subtitle,
                        "https://github.com/luantak/FokusLauncher",
                ),
                CommunityLink(
                        Icons.Outlined.Translate,
                        R.string.settings_weblate_title,
                        R.string.settings_weblate_subtitle,
                        "https://hosted.weblate.org/engage/fokus-launcher/",
                ),
                CommunityLink(
                        Icons.Filled.ChatBubble,
                        R.string.settings_matrix_title,
                        R.string.settings_matrix_subtitle,
                        "https://matrix.to/#/#fokus:matrix.org",
                ),
        )

@Composable
private fun rememberCoarseLocationPermission(context: Context, activity: Activity?): Pair<Boolean, () -> Unit> {
    var granted by remember {
        mutableStateOf(
                ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted =
                        ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) {
                granted =
                        ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                ) == PackageManager.PERMISSION_GRANTED
                if (!granted &&
                                activity != null &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                ) {
                    context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                    )
                }
            }
    val request = remember(launcher) { { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }
    return granted to request
}

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
        onDrawerDotSearchSettings: () -> Unit = {},
        onOpenHomeWidgetsSettings: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installedFontFamilies by viewModel.installedFontFamilies.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current

    val (hasCoarseLocationPermission, requestCoarseLocation) =
            rememberCoarseLocationPermission(context, activity)

    // Dialog states
    val showAppPickerFor = remember { mutableStateOf<String?>(null) } // swipeLeft/swipeRight/weather
    val showResetConfirm = remember { mutableStateOf(false) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSystemWallpaper(it)
            onNavigateToHome()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(backgroundScrim)
        .navigationBarsPadding()
        .testTag("settings_screen")
    ) {
        TopAppBar(
                title = {
                    Text(stringResource(R.string.settings_title), color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    FokusIconButton(onClick = onNavigateBack) {
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

        SettingsScreenContent(
                uiState = uiState,
                installedFontFamilies = installedFontFamilies,
                context = context,
                resources = resources,
                hasCoarseLocationPermission = hasCoarseLocationPermission,
                onShowStatusBarChanged = viewModel::setShowStatusBar,
                onAllowLandscapeRotationChanged = viewModel::setAllowLandscapeRotation,
                onAppLocaleTagChanged = viewModel::setAppLocaleTag,
                onLauncherFontFamilyChanged = viewModel::setLauncherFontFamilyName,
                onPickWallpaper = { wallpaperPickerLauncher.launch("image/*") },
                onSetBlackWallpaper = {
                    viewModel.setBlackWallpaper()
                    onNavigateToHome()
                },
                onOpenHomeWidgetsSettings = onOpenHomeWidgetsSettings,
                onOpenDeviceControlSettings = onOpenDeviceControlSettings,
                onEditHomeScreen = onEditHomeScreen,
                onEditRightShortcuts = onEditRightShortcuts,
                onEditCategories = onEditCategories,
                onDrawerDotSearchSettings = onDrawerDotSearchSettings,
                onRequestLocationPermission = requestCoarseLocation,
                onShowAppPicker = { showAppPickerFor.value = it },
                onShowResetConfirm = { showResetConfirm.value = true },
                onUnhideApp = viewModel::unhideApp,
                onRemoveRename = viewModel::removeRename,
                onDrawerSidebarCategoriesChanged = viewModel::setDrawerSidebarCategories,
                onDrawerCategorySidebarOnLeftChanged = viewModel::setDrawerCategorySidebarOnLeft,
                onDrawerAppSortModeChanged = viewModel::setDrawerAppSortMode,
                onHomeAlignmentChanged = viewModel::setHomeAlignment,
                onClearWeatherApp = { viewModel.setPreferredWeatherApp("") },
                onClearSwipeLeftTarget = { viewModel.setSwipeLeftTarget(null) },
                onClearSwipeRightTarget = { viewModel.setSwipeRightTarget(null) },
                createLogShareIntent = viewModel::createLogShareIntent
        )
    }

    SettingsScreenDialogs(
            uiState = uiState,
            showResetConfirm = showResetConfirm.value,
            pickerTarget = showAppPickerFor.value,
            onDismissResetConfirm = { showResetConfirm.value = false },
            onResetConfirmed = {
                viewModel.resetAllState()
                onNavigateBack()
            },
            onDismissPicker = { showAppPickerFor.value = null },
            onShortcutTargetSelected = { target, action ->
                when (target) {
                    "swipeLeft" -> viewModel.setSwipeLeftTarget(action.target)
                    "swipeRight" -> viewModel.setSwipeRightTarget(action.target)
                }
            },
            onAppPicked = { target, packageName ->
                when (target) {
                    "weather" -> viewModel.setPreferredWeatherApp(packageName)
                }
            }
    )
}

@Composable
private fun SettingsScreenContent(
        uiState: SettingsUiState,
        installedFontFamilies: List<String>,
        context: Context,
        resources: Resources,
        hasCoarseLocationPermission: Boolean,
        onShowStatusBarChanged: (Boolean) -> Unit,
        onAllowLandscapeRotationChanged: (Boolean) -> Unit,
        onAppLocaleTagChanged: (String) -> Unit,
        onLauncherFontFamilyChanged: (String) -> Unit,
        onPickWallpaper: () -> Unit,
        onSetBlackWallpaper: () -> Unit,
        onOpenHomeWidgetsSettings: () -> Unit,
        onOpenDeviceControlSettings: () -> Unit,
        onEditHomeScreen: () -> Unit,
        onEditRightShortcuts: () -> Unit,
        onEditCategories: () -> Unit,
        onDrawerDotSearchSettings: () -> Unit,
        onRequestLocationPermission: () -> Unit,
        onShowAppPicker: (String) -> Unit,
        onShowResetConfirm: () -> Unit,
        onUnhideApp: (String, String) -> Unit,
        onRemoveRename: (String, String) -> Unit,
        onDrawerSidebarCategoriesChanged: (Boolean) -> Unit,
        onDrawerCategorySidebarOnLeftChanged: (Boolean) -> Unit,
        onDrawerAppSortModeChanged: (DrawerAppSortMode) -> Unit,
        onHomeAlignmentChanged: (HomeAlignment) -> Unit,
        onClearWeatherApp: () -> Unit,
        onClearSwipeLeftTarget: () -> Unit,
        onClearSwipeRightTarget: () -> Unit,
        createLogShareIntent: suspend () -> Intent?
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
        items(
                listOf(
                        Triple(
                                R.string.settings_show_status_bar,
                                uiState.showStatusBar,
                                onShowStatusBarChanged,
                        ),
                        Triple(
                                R.string.settings_allow_landscape_rotation,
                                uiState.allowLandscapeRotation,
                                onAllowLandscapeRotationChanged,
                        ),
                ),
                key = { it.first },
        ) { (labelRes, checked, onChange) ->
            SettingsToggleRow(
                    label = stringResource(labelRes),
                    checked = checked,
                    onCheckedChange = onChange,
            )
        }
        item {
            AppLanguageDropdown(
                    currentTag = uiState.appLocaleTag,
                    onTagSelected = onAppLocaleTagChanged
            )
        }
        item {
            LauncherFontFamilyDropdown(
                    currentFamilyName = uiState.launcherFontFamilyName,
                    installedFamilies = installedFontFamilies,
                    onFamilySelected = onLauncherFontFamilyChanged
            )
        }
        item {
            SimpleSettingsRow(
                    label = stringResource(R.string.settings_set_background_image),
                    onClick = onPickWallpaper
            )
        }
        item {
            SimpleSettingsRow(
                    label = stringResource(R.string.settings_set_black_wallpaper),
                    onClick = onSetBlackWallpaper
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_home_screen)) }
        item {
            SettingsSubpageNavigationRow(
                    label = stringResource(R.string.settings_home_widgets),
                    subtitle = stringResource(R.string.settings_home_widgets_subtitle),
                    onClick = onOpenHomeWidgetsSettings
            )
        }
        item {
            SettingsActionRow(
                    label = stringResource(R.string.settings_accessibility),
                    subtitle = stringResource(R.string.settings_accessibility_subtitle),
                    onClick = onOpenDeviceControlSettings
            )
        }
        item {
            SettingsSubpageNavigationRow(
                    label = stringResource(R.string.settings_edit_home_screen),
                    onClick = onEditHomeScreen
            )
        }
        item {
            SettingsSubpageNavigationRow(
                    label = stringResource(R.string.settings_edit_shortcuts),
                    subtitle =
                            pluralStringResource(
                                    R.plurals.settings_shortcuts_configured,
                                    uiState.rightSideShortcuts.size,
                                    uiState.rightSideShortcuts.size
                            ),
                    onClick = onEditRightShortcuts
            )
        }
        item {
            HomeAlignmentRow(
                    currentAlignment = uiState.homeAlignment,
                    onAlignmentChanged = onHomeAlignmentChanged
            )
        }
        item {
            Column {
                if (!hasCoarseLocationPermission) {
                    LocationWeatherRow(onEnableClick = onRequestLocationPermission)
                } else {
                    val weatherAppLabel =
                            formatWeatherAppLabel(
                                    context,
                                    resources,
                                    uiState.preferredWeatherAppPackage,
                                    uiState.allApps
                            )
                    ShortcutTargetRow(
                            label = stringResource(R.string.settings_weather_app),
                            currentTarget = weatherAppLabel,
                            onPickApp = { onShowAppPicker("weather") },
                            onClear = onClearWeatherApp
                    )
                }
            }
        }
        item {
            ShortcutTargetRow(
                    label = stringResource(R.string.settings_swipe_left),
                    currentTarget =
                            formatShortcutTarget(
                                    context,
                                    resources,
                                    uiState.swipeLeftTarget,
                                    uiState.allApps
                            ),
                    onPickApp = { onShowAppPicker("swipeLeft") },
                    onClear = onClearSwipeLeftTarget
            )
        }
        item {
            ShortcutTargetRow(
                    label = stringResource(R.string.settings_swipe_right),
                    currentTarget =
                            formatShortcutTarget(
                                    context,
                                    resources,
                                    uiState.swipeRightTarget,
                                    uiState.allApps
                            ),
                    onPickApp = { onShowAppPicker("swipeRight") },
                    onClear = onClearSwipeRightTarget
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_app_drawer)) }
        item {
            SettingsSubpageNavigationRow(
                    label = stringResource(R.string.settings_edit_app_categories),
                    subtitle =
                            pluralStringResource(
                                    R.plurals.settings_categories_count,
                                    uiState.categoryDefinitions.size,
                                    uiState.categoryDefinitions.size
                            ),
                    onClick = onEditCategories
            )
        }
        item {
            SettingsToggleRow(
                    label = stringResource(R.string.settings_drawer_sidebar_categories),
                    subtitle = stringResource(R.string.settings_drawer_sidebar_categories_subtitle),
                    checked = uiState.drawerSidebarCategories,
                    onCheckedChange = onDrawerSidebarCategoriesChanged
            )
        }
        if (uiState.drawerSidebarCategories) {
            item {
                DrawerCategoryRailSideRow(
                        railOnLeft = uiState.drawerCategorySidebarOnLeft,
                        onRailOnLeftChanged = onDrawerCategorySidebarOnLeftChanged
                )
            }
        }
        item {
            DrawerAppSortRow(
                    currentMode = uiState.drawerAppSortMode,
                    showCustomSortOption = uiState.drawerSidebarCategories,
                    onModeChanged = onDrawerAppSortModeChanged
            )
        }
        item {
            SettingsSubpageNavigationRow(
                    label = stringResource(R.string.settings_dot_search_title),
                    subtitle = stringResource(R.string.settings_dot_search_subtitle),
                    onClick = onDrawerDotSearchSettings
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_hidden_apps)) }
        if (uiState.hiddenApps.isEmpty()) {
            item {
                EmptySettingsStateText(text = stringResource(R.string.settings_no_hidden_apps))
            }
        } else {
            items(uiState.hiddenApps) { hiddenApp ->
                HiddenAppRow(
                        app = hiddenApp,
                        onUnhide = { onUnhideApp(hiddenApp.packageName, hiddenApp.profileKey) }
                )
            }
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_renamed_apps)) }
        if (uiState.renamedApps.isEmpty()) {
            item {
                EmptySettingsStateText(text = stringResource(R.string.settings_no_renamed_apps))
            }
        } else {
            items(uiState.renamedApps) { renamedApp ->
                RenamedAppRow(
                        packageName = renamedApp.packageName,
                        profileLabel = renamedApp.profileLabel,
                        customName = renamedApp.customName,
                        onRemoveRename = {
                            onRemoveRename(renamedApp.packageName, renamedApp.profileKey)
                        }
                )
            }
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_connect_section)) }
        items(communityLinks, key = { it.url }) { link ->
            ExternalLinkRow(
                    icon = link.icon,
                    title = stringResource(link.titleRes),
                    subtitle = stringResource(link.subtitleRes),
                    onClick = {
                        context.startActivity(
                                Intent(Intent.ACTION_VIEW, link.url.toUri())
                                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        )
                    },
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_data)) }
        item {
            ExportLogsRow(
                    context = context,
                    createLogShareIntent = createLogShareIntent
            )
        }
        item {
            ResetAllDataRow(onClick = onShowResetConfirm)
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SettingsScreenDialogs(
        uiState: SettingsUiState,
        showResetConfirm: Boolean,
        pickerTarget: String?,
        onDismissResetConfirm: () -> Unit,
        onResetConfirmed: suspend () -> Unit,
        onDismissPicker: () -> Unit,
        onShortcutTargetSelected: (String, AppShortcutAction) -> Unit,
        onAppPicked: (String, String) -> Unit
) {
    if (showResetConfirm) {
        AlertDialog(
                onDismissRequest = onDismissResetConfirm,
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
                    FokusTextButton(
                            onClick = {
                                scope.launch {
                                    onResetConfirmed()
                                    onDismissResetConfirm()
                                }
                            }
                    ) {
                        Text(stringResource(R.string.action_reset), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    FokusTextButton(onClick = onDismissResetConfirm) {
                        Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    pickerTarget?.let { target ->
        when (target) {
            "swipeLeft", "swipeRight" -> {
                ShortcutActionPickerDialog(
                        allActions = uiState.allShortcutActions,
                        allApps = uiState.allApps,
                        title = stringResource(R.string.edit_shortcuts_section_all_actions),
                        onSelect = { action ->
                            onShortcutTargetSelected(target, action)
                            onDismissPicker()
                        },
                        onDismiss = onDismissPicker
                )
            }
            else -> {
                GroupedAppPickerDialog(
                        apps = uiState.allApps,
                        title = stringResource(R.string.settings_app_picker_title),
                        keyPrefix = "settings_app_pick",
                        onSelect = { app ->
                            onAppPicked(target, app.packageName)
                            onDismissPicker()
                        },
                        onDismiss = onDismissPicker,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWidgetsSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val showAppPickerFor = remember { mutableStateOf<String?>(null) }

    val (hasCoarseLocationPermission, requestCoarseLocation) =
            rememberCoarseLocationPermission(context, activity)

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("home_widgets_settings_screen")
    ) {
        TopAppBar(
                title = {
                    Text(
                            stringResource(R.string.settings_home_widgets),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    FokusIconButton(onClick = onNavigateBack) {
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
                        label = stringResource(R.string.settings_show_home_clock),
                        checked = uiState.showHomeClock,
                        onCheckedChange = { viewModel.setShowHomeClock(it) }
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_date),
                        checked = uiState.showHomeDate,
                        onCheckedChange = { viewModel.setShowHomeDate(it) }
                )
            }
            item {
                HomeDateFormatDropdown(
                        currentStyle = uiState.homeDateFormatStyle,
                        enabled = uiState.showHomeDate,
                        onStyleSelected = { viewModel.setHomeDateFormatStyle(it) }
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_weather),
                        checked = uiState.showHomeWeather,
                        onCheckedChange = { viewModel.setShowHomeWeather(it) }
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_battery),
                        checked = uiState.showHomeBattery,
                        onCheckedChange = { viewModel.setShowHomeBattery(it) }
                )
            }
            item { SettingsDivider() }
            item {
                Column {
                    if (!hasCoarseLocationPermission) {
                        LocationWeatherRow(onEnableClick = requestCoarseLocation)
                    } else {
                        val weatherAppLabel =
                                formatWeatherAppLabel(
                                        context,
                                        resources,
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
                val clockLabel =
                        formatWidgetAppOverrideLabel(
                                resources,
                                uiState.preferredClockAppPackage,
                                uiState.allApps
                        )
                ShortcutTargetRow(
                        label = stringResource(R.string.settings_widget_clock_app),
                        currentTarget = clockLabel,
                        onPickApp = { showAppPickerFor.value = "clock" },
                        onClear = { viewModel.setPreferredClockApp("") }
                )
            }
            item {
                val calendarLabel =
                        formatWidgetAppOverrideLabel(
                                resources,
                                uiState.preferredCalendarAppPackage,
                                uiState.allApps
                        )
                ShortcutTargetRow(
                        label = stringResource(R.string.settings_widget_calendar_app),
                        currentTarget = calendarLabel,
                        onPickApp = { showAppPickerFor.value = "calendar" },
                        onClear = { viewModel.setPreferredCalendarApp("") }
                )
            }
        }
    }

    showAppPickerFor.value?.let { pickerTarget ->
        GroupedAppPickerDialog(
                apps = uiState.allApps,
                title = stringResource(R.string.settings_app_picker_title),
                keyPrefix = "settings_app_pick",
                onSelect = { app ->
                    when (pickerTarget) {
                        "weather" -> viewModel.setPreferredWeatherApp(app.packageName)
                        "clock" -> viewModel.setPreferredClockApp(app.packageName)
                        "calendar" -> viewModel.setPreferredCalendarApp(app.packageName)
                    }
                    showAppPickerFor.value = null
                },
                onDismiss = { showAppPickerFor.value = null },
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityResumeTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val lockAccessibilityOn =
            remember(accessibilityResumeTick) {
                LockScreenHelper.isLockAccessibilityServiceEnabled(context)
            }

    LaunchedEffect(lockAccessibilityOn, uiState.longLockReturnHome) {
        if (uiState.longLockReturnHome && !lockAccessibilityOn) {
            viewModel.setLongLockReturnHome(false)
        }
    }

    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
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
                    FokusIconButton(onClick = onNavigateBack) {
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
                        onCheckedChange = { enabled -> viewModel.setLongLockReturnHome(enabled) },
                        enabled = lockAccessibilityOn
                )
            }

            if (lockAccessibilityOn && uiState.longLockReturnHome) {
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
                            .clickableWithSystemSound(enabled = enabled) { onCheckedChange(!checked) }
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
                onCheckedChange = rememberBooleanChangeWithSystemSound(onCheckedChange),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsReadOnlyExposedDropdown(
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        menuContent: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
                modifier =
                        textFieldModifier
                                .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = fieldEnabled
                                )
                                .fillMaxWidth(),
                value = selectedDisplayText,
                onValueChange = { _ -> },
                readOnly = true,
                enabled = fieldEnabled,
                singleLine = true,
                shape = SettingsPickerCorner,
                textStyle = textStyle,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                },
                colors = settingsPickerOutlinedFieldColors()
        )
        ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onExpandedChange(false) },
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
            menuContent()
        }
    }
}

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

@Composable
private fun homeDateFormatStyleLabel(style: HomeDateFormatStyle): String =
        when (style) {
            HomeDateFormatStyle.SYSTEM_DEFAULT ->
                    stringResource(R.string.settings_home_date_format_system)
            HomeDateFormatStyle.US_SLASHES ->
                    stringResource(R.string.settings_home_date_format_us_slashes)
            HomeDateFormatStyle.EU_SLASHES ->
                    stringResource(R.string.settings_home_date_format_eu_slashes)
            HomeDateFormatStyle.EU_DOTS ->
                    stringResource(R.string.settings_home_date_format_eu_dots)
            HomeDateFormatStyle.WEEKDAY_MONTH_ABBR ->
                    stringResource(R.string.settings_home_date_format_weekday_month)
            HomeDateFormatStyle.MONTH_LONG ->
                    stringResource(R.string.settings_home_date_format_month_long)
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDateFormatDropdown(
        currentStyle: HomeDateFormatStyle,
        enabled: Boolean,
        onStyleSelected: (HomeDateFormatStyle) -> Unit
) {
    val options =
            remember {
                listOf(
                        HomeDateFormatStyle.SYSTEM_DEFAULT,
                        HomeDateFormatStyle.US_SLASHES,
                        HomeDateFormatStyle.EU_SLASHES,
                        HomeDateFormatStyle.EU_DOTS,
                        HomeDateFormatStyle.WEEKDAY_MONTH_ABBR,
                        HomeDateFormatStyle.MONTH_LONG
                )
            }
    var expanded by remember { mutableStateOf(false) }
    val onDateFormatExpandedChange =
            rememberBooleanChangeWithSystemSound { newExpanded ->
                if (enabled) expanded = newExpanded
            }
    val selectedLabel = homeDateFormatStyleLabel(currentStyle)
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_home_date_format),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        SettingsReadOnlyExposedDropdown(
                expanded = expanded,
                onExpandedChange = onDateFormatExpandedChange,
                selectedDisplayText = selectedLabel,
                fieldEnabled = enabled,
                menuExpanded = expanded && enabled,
        ) {
            options.forEach { style ->
                DropdownMenuItem(
                        text = {
                            Text(
                                    text = homeDateFormatStyleLabel(style),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        onClick =
                                rememberClickWithSystemSound {
                                    onStyleSelected(style)
                                    expanded = false
                                },
                        colors = settingsPickerMenuItemColors(),
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageDropdown(
        currentTag: String,
        onTagSelected: (String) -> Unit
) {
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
    val supportedLocaleTags = remember { listOf("en", "de", "pl", "ru", "zh-CN", "tr", "da") }
    val options =
            remember(systemDefaultLabel) {
                val collator = Collator.getInstance(Locale.ROOT).apply { strength = Collator.PRIMARY }
                buildList {
                    add("" to systemDefaultLabel)
                    supportedLocaleTags
                            .map { tag -> tag to languageAutonym(tag) }
                            .sortedWith { a, b -> collator.compare(a.second, b.second) }
                            .forEach { add(it) }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val onLanguageExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
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
        SettingsReadOnlyExposedDropdown(
                expanded = expanded,
                onExpandedChange = onLanguageExpandedChange,
                selectedDisplayText = selectedDisplayText,
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
                        onClick =
                                rememberClickWithSystemSound {
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
    val onFontExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
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
        SettingsReadOnlyExposedDropdown(
                expanded = expanded,
                onExpandedChange = onFontExpandedChange,
                selectedDisplayText = selectedLabel,
                textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = composeFontFamilyFromStoredName(currentFamilyName)
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
                        onClick =
                                rememberClickWithSystemSound {
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

@Composable
private fun SettingsLabeledSegmentedSection(
        title: String,
        subtitle: String?,
        content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerCategoryRailSideRow(
        railOnLeft: Boolean,
        onRailOnLeftChanged: (Boolean) -> Unit
) {
    SettingsLabeledSegmentedSection(
            title = stringResource(R.string.settings_drawer_category_rail_side),
            subtitle = stringResource(R.string.settings_drawer_category_rail_side_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                    selected = railOnLeft,
                    onClick =
                            rememberClickWithSystemSound { onRailOnLeftChanged(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.settings_drawer_rail_position_left))
            }
            SegmentedButton(
                    selected = !railOnLeft,
                    onClick =
                            rememberClickWithSystemSound { onRailOnLeftChanged(false) },
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
        showCustomSortOption: Boolean,
        onModeChanged: (DrawerAppSortMode) -> Unit
) {
    val modes =
            remember(showCustomSortOption) {
                if (showCustomSortOption) DrawerAppSortMode.entries.toList()
                else DrawerAppSortMode.entries.filterNot { it == DrawerAppSortMode.CUSTOM }
            }
    val coercedMode =
            remember(currentMode, showCustomSortOption) {
                if (!showCustomSortOption && currentMode == DrawerAppSortMode.CUSTOM) {
                    DrawerAppSortMode.ALPHABETICAL
                } else {
                    currentMode
                }
            }
    SettingsLabeledSegmentedSection(
            title = stringResource(R.string.settings_drawer_app_sort),
            subtitle = stringResource(R.string.settings_drawer_app_sort_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                        selected = coercedMode == mode,
                        onClick = rememberClickWithSystemSound { onModeChanged(mode) },
                        shape =
                                SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = modes.size
                                )
                ) {
                    Text(
                            stringResource(
                                    when (mode) {
                                        DrawerAppSortMode.ALPHABETICAL ->
                                                R.string.settings_drawer_app_sort_alphabetical
                                        DrawerAppSortMode.MOST_OPENED ->
                                                R.string.settings_drawer_app_sort_most_opened
                                        DrawerAppSortMode.CUSTOM ->
                                                R.string.settings_drawer_app_sort_custom
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
    val onLongLockExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
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
        SettingsReadOnlyExposedDropdown(
                expanded = expanded,
                onExpandedChange = onLongLockExpandedChange,
                selectedDisplayText = selectedLabel,
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
                        onClick =
                                rememberClickWithSystemSound {
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

@Composable
private fun HomeAlignmentRow(
        currentAlignment: HomeAlignment,
        onAlignmentChanged: (HomeAlignment) -> Unit
) {
    SettingsLabeledSegmentedSection(
            title = stringResource(R.string.home_alignment_title),
            subtitle = stringResource(R.string.home_alignment_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            HomeAlignment.entries.forEachIndexed { index, alignment ->
                SegmentedButton(
                        selected = currentAlignment == alignment,
                        onClick =
                                rememberClickWithSystemSound {
                                    onAlignmentChanged(alignment)
                                },
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
    SettingsSubpageNavigationRow(label = label, subtitle = subtitle, onClick = onClick)
}

@Composable
private fun SettingsSubpageNavigationRow(
        label: String,
        subtitle: String? = null,
        onClick: () -> Unit,
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(onClick = onClick)
                            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
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
        Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = stringResource(R.string.cd_open_subpage),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SimpleSettingsRow(
        label: String,
        onClick: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(onClick = onClick)
                            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptySettingsStateText(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun ExportLogsRow(
        context: Context,
        createLogShareIntent: suspend () -> Intent?
) {
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val shareChooserTitle =
            stringResource(R.string.settings_export_logs_share_chooser)
    val exportLogsFailedToast = stringResource(R.string.toast_export_logs_failed)
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound {
                                scope.launch {
                                    val shareIntent = createLogShareIntent()
                                    if (shareIntent != null && activity != null) {
                                        activity.startActivity(
                                                Intent.createChooser(
                                                        shareIntent,
                                                        shareChooserTitle
                                                )
                                        )
                                    } else {
                                        Toast.makeText(
                                                        context,
                                                        exportLogsFailedToast,
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

@Composable
private fun ResetAllDataRow(onClick: () -> Unit) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(onClick = onClick)
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

// --- Location for weather row (shown only when permission disabled) ---

@Composable
private fun LocationWeatherRow(onEnableClick: () -> Unit) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(onClick = onEnableClick)
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
        FokusTextButton(onClick = onEnableClick) {
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
        FokusTextButton(onClick = onPickApp) { Text(stringResource(R.string.action_change)) }
        FokusIconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
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
                            .clickableWithSystemSound(onClick = onUnhide)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            val secondary =
                    app.profileLabel?.let { "$it • ${app.packageName}" } ?: app.packageName
            Text(
                    secondary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
                Icons.Default.Visibility,
                stringResource(R.string.cd_unhide_app),
                tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun RenamedAppRow(
        packageName: String,
        profileLabel: String?,
        customName: String,
        onRemoveRename: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(onClick = onRemoveRename)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    customName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    profileLabel?.let { "$it • $packageName" } ?: packageName,
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
                            .clickableWithSystemSound(onClick = onClick)
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

private fun formatWidgetAppOverrideLabel(
        resources: Resources,
        packageName: String,
        allApps: List<AppInfo>
): String {
    if (packageName.isNotBlank()) {
        return allApps.find { it.packageName == packageName }?.label ?: packageName
    }
    return resources.getString(R.string.settings_weather_app_system_default)
}

private fun formatWeatherAppLabel(
        context: Context,
        resources: Resources,
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
        resources.getString(R.string.settings_weather_app_system_default)
    } else {
        resources.getString(R.string.settings_weather_app_not_configured)
    }
}

private fun formatShortcutTarget(
        context: Context,
        resources: Resources,
        target: ShortcutTarget?,
        allApps: List<AppInfo>
): String {
    return formatShortcutTargetDisplay(
            context = context,
            target = target,
            allApps = allApps,
            notSetLabel = resources.getString(R.string.shortcut_target_not_set),
            resolvedLauncherActionLabel = null
    )
}
