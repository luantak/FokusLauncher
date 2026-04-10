package com.lu4p.fokuslauncher.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Translate
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import java.text.Collator
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDropdown
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import com.lu4p.fokuslauncher.ui.theme.launcherIconDp
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

private data class SubpageNavRow(
        @param:StringRes val labelRes: Int,
        val subtitle: String? = null,
        val onClick: () -> Unit,
)

private data class SwipeTargetPick(
        val pickerKey: String,
        @param:StringRes val labelRes: Int,
        val target: ShortcutTarget?,
        val onClear: () -> Unit,
)

private data class PreferredAppPickerRow(
        @param:StringRes val labelRes: Int,
        val packageName: String,
        val pickerKey: String,
        val onClear: () -> Unit,
)

private data class DeviceControlToggleRow(
        @param:StringRes val labelRes: Int,
        val subtitle: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
)

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

private fun Context.hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

private fun <T> LazyListScope.manageableAppsSection(
        headerRes: Int,
        emptyTextRes: Int,
        apps: List<T>,
        key: (T) -> Any,
        label: (T) -> String,
        subtitle: (T) -> String,
        onRowClick: (T) -> Unit,
        trailingContent: @Composable RowScope.(T) -> Unit,
) {
    item { SectionHeader(stringResource(headerRes)) }
    if (apps.isEmpty()) {
        item { EmptySettingsStateText(text = stringResource(emptyTextRes)) }
    } else {
        items(apps, key = key) { app ->
            SettingsRow(
                    label = label(app),
                    subtitle = subtitle(app),
                    subtitleStyle = MaterialTheme.typography.labelMedium,
                    onClick = { onRowClick(app) },
                    trailing = { trailingContent(app) },
            )
        }
    }
}

@Composable
private fun rememberCoarseLocationPermission(context: Context, activity: Activity?): Pair<Boolean, () -> Unit> {
    var granted by remember { mutableStateOf(context.hasCoarseLocationPermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    OnResumeEffect(lifecycleOwner) { granted = context.hasCoarseLocationPermission() }
    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) {
                granted = context.hasCoarseLocationPermission()
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

    // Dialog states
    val showAppPickerFor = remember { mutableStateOf<String?>(null) } // swipeLeft/swipeRight
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
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        SettingsScreenContent(
                viewModel = viewModel,
                uiState = uiState,
                installedFontFamilies = installedFontFamilies,
                context = context,
                resources = resources,
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
                onShowAppPicker = { showAppPickerFor.value = it },
                onShowResetConfirm = { showResetConfirm.value = true },
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
    )
}

@Composable
private fun SettingsScreenContent(
        viewModel: SettingsViewModel,
        uiState: SettingsUiState,
        installedFontFamilies: List<String>,
        context: Context,
        resources: Resources,
        onPickWallpaper: () -> Unit,
        onSetBlackWallpaper: () -> Unit,
        onOpenHomeWidgetsSettings: () -> Unit,
        onOpenDeviceControlSettings: () -> Unit,
        onEditHomeScreen: () -> Unit,
        onEditRightShortcuts: () -> Unit,
        onEditCategories: () -> Unit,
        onDrawerDotSearchSettings: () -> Unit,
        onShowAppPicker: (String) -> Unit,
        onShowResetConfirm: () -> Unit,
) {
    val homeScreenSubpageRows =
            listOf(
                    SubpageNavRow(
                            R.string.settings_home_widgets,
                            stringResource(R.string.settings_home_widgets_subtitle),
                            onOpenHomeWidgetsSettings,
                    ),
                    SubpageNavRow(
                            R.string.settings_accessibility,
                            stringResource(R.string.settings_accessibility_subtitle),
                            onOpenDeviceControlSettings,
                    ),
                    SubpageNavRow(
                            R.string.settings_edit_home_screen,
                            onClick = onEditHomeScreen,
                    ),
                    SubpageNavRow(
                            R.string.settings_edit_shortcuts,
                            pluralStringResource(
                                    R.plurals.settings_shortcuts_configured,
                                    uiState.rightSideShortcuts.size,
                                    uiState.rightSideShortcuts.size
                            ),
                            onEditRightShortcuts,
                    ),
            )
    val editableCategoryCount =
            remember(uiState.allApps, uiState.categoryDefinitions) {
                editableCategoriesForSettings(uiState).size
            }
    val drawerSubpageRows =
            listOf(
                    SubpageNavRow(
                            R.string.settings_edit_app_categories,
                            pluralStringResource(
                                    R.plurals.settings_categories_count,
                                    editableCategoryCount,
                                    editableCategoryCount
                            ),
                            onEditCategories,
                    ),
                    SubpageNavRow(
                            R.string.settings_dot_search_title,
                            stringResource(R.string.settings_dot_search_subtitle),
                            onDrawerDotSearchSettings,
                    ),
            )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
        items(
                listOf(
                        Triple(
                                R.string.settings_show_status_bar,
                                uiState.showStatusBar,
                                viewModel::setShowStatusBar,
                        ),
                        Triple(
                                R.string.settings_allow_landscape_rotation,
                                uiState.allowLandscapeRotation,
                                viewModel::setAllowLandscapeRotation,
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
                    onTagSelected = viewModel::setAppLocaleTag
            )
        }
        item {
            LauncherFontFamilyDropdown(
                    currentFamilyName = uiState.launcherFontFamilyName,
                    installedFamilies = installedFontFamilies,
                    onFamilySelected = viewModel::setLauncherFontFamilyName
            )
        }
        item {
            LauncherFontSizeSlider(
                    currentScale = uiState.launcherFontScale,
                    onScaleChange = viewModel::setLauncherFontScale,
            )
        }
        item {
            SettingsRow(
                    label = stringResource(R.string.settings_set_background_image),
                    verticalPadding = 14.dp,
                    onClick = onPickWallpaper,
            )
        }
        item {
            SettingsRow(
                    label = stringResource(R.string.settings_set_black_wallpaper),
                    verticalPadding = 14.dp,
                    onClick = onSetBlackWallpaper,
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_home_screen)) }
        items(
                homeScreenSubpageRows,
                key = { it.labelRes },
        ) { row ->
            SettingsRow(
                    label = stringResource(row.labelRes),
                    subtitle = row.subtitle,
                    verticalPadding = 14.dp,
                    onClick = row.onClick,
                    trailing = { SubpageChevron() },
            )
        }
        item {
            HomeAlignmentRow(
                    currentAlignment = uiState.homeAlignment,
                    onAlignmentChanged = viewModel::setHomeAlignment
            )
        }
        items(
                listOf(
                        SwipeTargetPick(
                                "swipeLeft",
                                R.string.settings_swipe_left,
                                uiState.swipeLeftTarget,
                        ) { viewModel.setSwipeLeftTarget(null) },
                        SwipeTargetPick(
                                "swipeRight",
                                R.string.settings_swipe_right,
                                uiState.swipeRightTarget,
                        ) { viewModel.setSwipeRightTarget(null) },
                ),
                key = { it.pickerKey },
        ) { row ->
            ShortcutTargetRow(
                    label = stringResource(row.labelRes),
                    currentTarget =
                            formatShortcutTarget(
                                    context,
                                    resources,
                                    row.target,
                                    uiState.allApps
                            ),
                    onPickApp = { onShowAppPicker(row.pickerKey) },
                    onClear = row.onClear,
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_app_drawer)) }
        items(
                drawerSubpageRows,
                key = { it.labelRes },
        ) { row ->
            SettingsRow(
                    label = stringResource(row.labelRes),
                    subtitle = row.subtitle,
                    verticalPadding = 14.dp,
                    onClick = row.onClick,
                    trailing = { SubpageChevron() },
            )
        }
        item {
            SettingsToggleRow(
                    label = stringResource(R.string.settings_drawer_sidebar_categories),
                    subtitle = stringResource(R.string.settings_drawer_sidebar_categories_subtitle),
                    checked = uiState.drawerSidebarCategories,
                    onCheckedChange = viewModel::setDrawerSidebarCategories
            )
        }
        item {
            SettingsToggleRow(
                    label = stringResource(R.string.settings_drawer_search_auto_launch),
                    subtitle = stringResource(R.string.settings_drawer_search_auto_launch_subtitle),
                    checked = uiState.drawerSearchAutoLaunch,
                    onCheckedChange = viewModel::setDrawerSearchAutoLaunch
            )
        }
        if (uiState.drawerSidebarCategories) {
            item {
                DrawerCategoryRailSideRow(
                        railOnLeft = uiState.drawerCategorySidebarOnLeft,
                        onRailOnLeftChanged = viewModel::setDrawerCategorySidebarOnLeft
                )
            }
        }
        item {
            DrawerAppSortRow(
                    currentMode = uiState.drawerAppSortMode,
                    showCustomSortOption = uiState.drawerSidebarCategories,
                    onModeChanged = viewModel::setDrawerAppSortMode
            )
        }
        item { SettingsDivider() }

        manageableAppsSection(
                headerRes = R.string.settings_section_hidden_apps,
                emptyTextRes = R.string.settings_no_hidden_apps,
                apps = uiState.hiddenApps,
                key = { "${it.packageName}|${it.profileKey}" },
                label = { it.label },
                subtitle = { app ->
                    app.profileLabel?.let { pl -> "$pl • ${app.packageName}" } ?: app.packageName
                },
                onRowClick = { viewModel.unhideApp(it.packageName, it.profileKey) },
                trailingContent = {
                    Spacer(Modifier.width(8.dp))
                    LauncherIcon(
                            Icons.Default.Visibility,
                            stringResource(R.string.cd_unhide_app),
                            tint = MaterialTheme.colorScheme.secondary,
                            iconSize = 24.dp,
                    )
                },
        )
        item { SettingsDivider() }

        manageableAppsSection(
                headerRes = R.string.settings_section_renamed_apps,
                emptyTextRes = R.string.settings_no_renamed_apps,
                apps = uiState.renamedApps,
                key = { "${it.packageName}|${it.profileKey}" },
                label = { it.customName },
                subtitle = { app ->
                    app.profileLabel?.let { pl -> "$pl • ${app.packageName}" } ?: app.packageName
                },
                onRowClick = { viewModel.removeRename(it.packageName, it.profileKey) },
                trailingContent = {
                    Spacer(Modifier.width(8.dp))
                    LauncherIcon(
                            Icons.Default.Close,
                            stringResource(R.string.cd_remove_rename),
                            tint = MaterialTheme.colorScheme.secondary,
                            iconSize = 24.dp,
                    )
                },
        )
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_connect_section)) }
        items(communityLinks, key = { it.url }) { link ->
            SettingsRow(
                    label = stringResource(link.titleRes),
                    subtitle = stringResource(link.subtitleRes),
                    verticalPadding = 14.dp,
                    leading = {
                        LauncherIcon(
                                imageVector = link.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                iconSize = 24.dp,
                        )
                    },
                    trailing = {
                        LauncherIcon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.cd_open_link),
                                tint = MaterialTheme.colorScheme.secondary,
                                iconSize = 18.dp,
                        )
                    },
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
                    createLogShareIntent = viewModel::createLogShareIntent
            )
        }
        item {
            SettingsRow(
                    label = stringResource(R.string.settings_reset_all_data),
                    labelColor = MaterialTheme.colorScheme.error,
                    verticalPadding = 14.dp,
                    leading = {
                        LauncherIcon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                iconSize = 24.dp,
                        )
                    },
                    onClick = onShowResetConfirm,
            )
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
) {
    if (showResetConfirm) {
        FokusAlertDialog(
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
            else -> onDismissPicker()
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
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_home_widgets),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                    listOf(
                            Triple(R.string.settings_show_home_clock, uiState.showHomeClock, viewModel::setShowHomeClock),
                            Triple(R.string.settings_show_home_date, uiState.showHomeDate, viewModel::setShowHomeDate),
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
                HomeDateFormatDropdown(
                        currentStyle = uiState.homeDateFormatStyle,
                        enabled = uiState.showHomeDate,
                        onStyleSelected = viewModel::setHomeDateFormatStyle,
                )
            }
            items(
                    listOf(
                            Triple(R.string.settings_show_home_weather, uiState.showHomeWeather, viewModel::setShowHomeWeather),
                            Triple(R.string.settings_show_home_battery, uiState.showHomeBattery, viewModel::setShowHomeBattery),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item { SettingsDivider() }
            item {
                WeatherAppSettingRow(
                        hasCoarseLocationPermission = hasCoarseLocationPermission,
                        onRequestLocationPermission = requestCoarseLocation,
                        context = context,
                        resources = resources,
                        preferredWeatherAppPackage = uiState.preferredWeatherAppPackage,
                        allApps = uiState.allApps,
                        onPickApp = { showAppPickerFor.value = "weather" },
                        onClear = { viewModel.setPreferredWeatherApp("") },
                )
            }
            items(
                    listOf(
                            PreferredAppPickerRow(
                                    R.string.settings_widget_clock_app,
                                    uiState.preferredClockAppPackage,
                                    "clock",
                            ) { viewModel.setPreferredClockApp("") },
                            PreferredAppPickerRow(
                                    R.string.settings_widget_calendar_app,
                                    uiState.preferredCalendarAppPackage,
                                    "calendar",
                            ) { viewModel.setPreferredCalendarApp("") },
                    ),
                    key = { it.labelRes },
            ) { row ->
                ShortcutTargetRow(
                        label = stringResource(row.labelRes),
                        currentTarget =
                                formatPreferredAppLabel(
                                        context,
                                        resources,
                                        row.packageName,
                                        uiState.allApps,
                                        ::formatWidgetAppEmptyLabel,
                                ),
                        onPickApp = { showAppPickerFor.value = row.pickerKey },
                        onClear = row.onClear,
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
    var accessibilityResumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    OnResumeEffect(lifecycleOwner) { accessibilityResumeTick++ }

    val lockAccessibilityOn =
            remember(accessibilityResumeTick) {
                LockScreenHelper.isLockAccessibilityServiceEnabled(context)
            }

    LaunchedEffect(lockAccessibilityOn, uiState.longLockReturnHome) {
        if (uiState.longLockReturnHome && !lockAccessibilityOn) {
            viewModel.setLongLockReturnHome(false)
        }
    }

    val deviceControlToggleRows =
            listOf(
                    DeviceControlToggleRow(
                            R.string.settings_double_tap_to_lock,
                            stringResource(R.string.settings_double_tap_to_lock_subtitle),
                            uiState.doubleTapEmptyLock,
                            viewModel::setDoubleTapEmptyLock,
                    ),
                    DeviceControlToggleRow(
                            R.string.settings_return_home_after_long_lock,
                            stringResource(R.string.settings_return_home_after_long_lock_subtitle),
                            uiState.longLockReturnHome,
                            viewModel::setLongLockReturnHome,
                    ),
            )

    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
                    .testTag("device_control_settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_accessibility_page_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
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

            items(
                    deviceControlToggleRows,
                    key = { it.labelRes },
            ) { row ->
                SettingsToggleRow(
                        label = stringResource(row.labelRes),
                        subtitle = row.subtitle,
                        checked = row.checked,
                        onCheckedChange = row.onCheckedChange,
                        enabled = lockAccessibilityOn,
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
private fun HomeDateFormatDropdown(
        currentStyle: HomeDateFormatStyle,
        enabled: Boolean,
        onStyleSelected: (HomeDateFormatStyle) -> Unit
) {
    val options = remember { HomeDateFormatStyle.entries }
    var expanded by remember { mutableStateOf(false) }
    val onDateFormatExpandedChange =
            rememberBooleanChangeWithSystemSound { newExpanded ->
                if (enabled) expanded = newExpanded
            }
    SettingsDropdown(
            title = stringResource(R.string.settings_home_date_format),
            options = options,
            expanded = expanded,
            onExpandedChange = onDateFormatExpandedChange,
            selectedDisplayText = stringResource(currentStyle.labelRes),
            fieldEnabled = enabled,
            menuExpanded = expanded && enabled,
            itemContent = { style ->
                Text(
                        text = stringResource(style.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = onStyleSelected,
    )
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
    SettingsDropdown(
            title = stringResource(R.string.settings_language_label),
            options = options,
            expanded = expanded,
            onExpandedChange = onLanguageExpandedChange,
            selectedDisplayText = selectedDisplayText,
            itemContent = { (_, label) ->
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = { (storageTag, _) -> onTagSelected(storageTag) },
    )
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
    SettingsDropdown(
            title = stringResource(R.string.settings_font_label),
            options = options,
            expanded = expanded,
            onExpandedChange = onFontExpandedChange,
            selectedDisplayText = selectedLabel,
            textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = composeFontFamilyFromStoredName(currentFamilyName)
                    ),
            itemContent = { (storageValue, label) ->
                Text(
                        text = label,
                        style =
                                MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily =
                                                composeFontFamilyFromStoredName(storageValue)
                                ),
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = { (storageValue, _) -> onFamilySelected(storageValue) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherFontSizeSlider(
        currentScale: Float,
        onScaleChange: (Float) -> Unit,
) {
    val synced = LauncherFontScale.snapToStep(currentScale)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                    text = stringResource(R.string.settings_font_size_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
            )
            Text(
                    text = String.format(Locale.US, "%.1fx", pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_font_size_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = LauncherFontScale.snapToStep(raw)
                },
                onValueChangeFinished = {
                    val v = LauncherFontScale.snapToStep(pending)
                    if (v != synced) {
                        onScaleChange(v)
                    }
                },
                valueRange = LauncherFontScale.MIN..LauncherFontScale.MAX,
                steps = LauncherFontScale.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
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

@OptIn(ExperimentalMaterial3Api::class)
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
            remember(currentMode, showCustomSortOption, modes) {
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
        SingleChoiceSegmentedButtonRow(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(IntrinsicSize.Max)
        ) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                        selected = coercedMode == mode,
                        onClick = rememberClickWithSystemSound { onModeChanged(mode) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape =
                                SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = modes.size
                                )
                ) {
                    Box(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                    ) {
                        Text(
                                text = stringResource(mode.labelRes),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
    SettingsDropdown(
            title = stringResource(R.string.settings_long_lock_duration),
            subtitle = stringResource(R.string.settings_long_lock_duration_subtitle),
            options = options,
            expanded = expanded,
            onExpandedChange = onLongLockExpandedChange,
            selectedDisplayText = selectedLabel,
            itemContent = { minutes ->
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
            onItemSelected = onMinutesSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    Text(stringResource(alignment.labelRes))
                }
            }
        }
    }
}

@Composable
private fun SubpageChevron() {
    LauncherIcon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = stringResource(R.string.cd_open_subpage),
            tint = MaterialTheme.colorScheme.secondary,
            iconSize = 22.dp,
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
            text = title,
            style =
                    MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                    ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp),
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
    val shareChooserTitle = stringResource(R.string.settings_export_logs_share_chooser)
    val exportLogsFailedToast = stringResource(R.string.toast_export_logs_failed)
    SettingsRow(
            label = stringResource(R.string.settings_export_logs_title),
            subtitle = stringResource(R.string.settings_export_logs_subtitle),
            verticalPadding = 14.dp,
            onClick = {
                scope.launch {
                    val shareIntent = createLogShareIntent()
                    if (shareIntent != null && activity != null) {
                        activity.startActivity(
                                Intent.createChooser(shareIntent, shareChooserTitle)
                        )
                    } else {
                        Toast.makeText(context, exportLogsFailedToast, Toast.LENGTH_SHORT).show()
                    }
                }
            },
    )
}

// --- Weather app (location gate + shortcut row) ---

@Composable
private fun WeatherAppSettingRow(
        hasCoarseLocationPermission: Boolean,
        onRequestLocationPermission: () -> Unit,
        context: Context,
        resources: Resources,
        preferredWeatherAppPackage: String,
        allApps: List<AppInfo>,
        onPickApp: () -> Unit,
        onClear: () -> Unit,
) {
    Column {
        if (!hasCoarseLocationPermission) {
            SettingsRow(
                    label = stringResource(R.string.settings_weather_location_disabled),
                    subtitle = stringResource(R.string.settings_weather_location_disabled_subtitle),
                    onClick = onRequestLocationPermission,
                    leading = {
                        LauncherIcon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                iconSize = 24.dp,
                        )
                    },
                    trailing = {
                        FokusTextButton(onClick = onRequestLocationPermission) {
                            Text(stringResource(R.string.settings_weather_location_enable_button))
                        }
                    },
            )
        } else {
            val weatherAppLabel =
                    formatPreferredAppLabel(
                            context,
                            resources,
                            preferredWeatherAppPackage,
                            allApps,
                            ::formatWeatherAppEmptyLabel,
                    )
            ShortcutTargetRow(
                    label = stringResource(R.string.settings_weather_app),
                    currentTarget = weatherAppLabel,
                    onPickApp = onPickApp,
                    onClear = onClear,
            )
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
            verticalAlignment = Alignment.Top,
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
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
        ) {
            FokusIconButton(
                    onClick = onPickApp,
                    modifier = Modifier.size(36.dp.launcherIconDp()),
            ) {
                LauncherIcon(
                        Icons.Outlined.Edit,
                        stringResource(R.string.action_change),
                        tint = MaterialTheme.colorScheme.primary,
                        iconSize = 20.dp,
                )
            }
            FokusIconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp.launcherIconDp()),
            ) {
                LauncherIcon(
                        Icons.Default.Close,
                        stringResource(R.string.action_clear),
                        tint = MaterialTheme.colorScheme.error,
                        iconSize = 18.dp,
                )
            }
        }
    }
}

// =====================  DIALOGS  =====================

private fun formatPreferredAppLabel(
        context: Context,
        resources: Resources,
        packageName: String,
        allApps: List<AppInfo>,
        emptyLabel: (Context, Resources) -> String,
): String {
    if (packageName.isNotBlank()) {
        return allApps.find { it.packageName == packageName }?.label ?: packageName
    }
    return emptyLabel(context, resources)
}

private fun formatWidgetAppEmptyLabel(context: Context, resources: Resources): String =
        resources.getString(R.string.settings_weather_app_system_default)

private fun formatWeatherAppEmptyLabel(context: Context, resources: Resources): String {
    val hasSystemWeatherApp =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent(Intent.ACTION_MAIN)
                        .apply { addCategory(Intent.CATEGORY_APP_WEATHER) }
                        .resolveActivity(context.packageManager) != null
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
