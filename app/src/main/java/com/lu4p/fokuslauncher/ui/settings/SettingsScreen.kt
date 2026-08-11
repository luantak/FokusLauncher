package com.lu4p.fokuslauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SubpageChevron
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.theme.withLauncherTextGlowRecolored
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onEditHomeScreen: () -> Unit = {},
        onEditRightShortcuts: () -> Unit = {},
        onOpenDeviceControlSettings: () -> Unit = {},
        onEditCategories: () -> Unit = {},
        onDrawerDotSearchSettings: () -> Unit = {},
        onOpenHomeWidgetsSettings: () -> Unit = {},
        onOpenAppearanceSettings: () -> Unit = {},
        onOpenDrawerBehaviorSettings: () -> Unit = {},
        onOpenAppsManagementSettings: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current

    val showAppPickerFor = remember { mutableStateOf<String?>(null) }
    val showResetConfirm = remember { mutableStateOf(false) }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        SettingsHubContent(
                viewModel = viewModel,
                uiState = uiState,
                context = context,
                resources = resources,
                onOpenAppearanceSettings = onOpenAppearanceSettings,
                onOpenHomeWidgetsSettings = onOpenHomeWidgetsSettings,
                onOpenDeviceControlSettings = onOpenDeviceControlSettings,
                onEditHomeScreen = onEditHomeScreen,
                onEditRightShortcuts = onEditRightShortcuts,
                onEditCategories = onEditCategories,
                onDrawerDotSearchSettings = onDrawerDotSearchSettings,
                onOpenDrawerBehaviorSettings = onOpenDrawerBehaviorSettings,
                onOpenAppsManagementSettings = onOpenAppsManagementSettings,
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
private fun SettingsHubContent(
        viewModel: SettingsViewModel,
        uiState: SettingsUiState,
        context: Context,
        resources: Resources,
        onOpenAppearanceSettings: () -> Unit,
        onOpenHomeWidgetsSettings: () -> Unit,
        onOpenDeviceControlSettings: () -> Unit,
        onEditHomeScreen: () -> Unit,
        onEditRightShortcuts: () -> Unit,
        onEditCategories: () -> Unit,
        onDrawerDotSearchSettings: () -> Unit,
        onOpenDrawerBehaviorSettings: () -> Unit,
        onOpenAppsManagementSettings: () -> Unit,
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
                            pluralStringResource(
                                    R.plurals.settings_home_screen_apps_count,
                                    uiState.favorites.size,
                                    uiState.favorites.size,
                            ),
                            onEditHomeScreen,
                    ),
                    SubpageNavRow(
                            R.string.settings_edit_shortcuts,
                            pluralStringResource(
                                    R.plurals.settings_shortcuts_configured,
                                    uiState.rightSideShortcuts.size,
                                    uiState.rightSideShortcuts.size,
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
                                    editableCategoryCount,
                            ),
                            onEditCategories,
                    ),
                    SubpageNavRow(
                            R.string.settings_dot_search_title,
                            stringResource(R.string.settings_dot_search_subtitle),
                            onDrawerDotSearchSettings,
                    ),
                    SubpageNavRow(
                            R.string.settings_drawer_behavior_title,
                            stringResource(R.string.settings_drawer_behavior_subtitle),
                            onOpenDrawerBehaviorSettings,
                    ),
            )
    val managedAppsCount =
            uiState.hiddenApps.size +
                    uiState.renamedApps.size +
                    if (Build.VERSION.SDK_INT >= 35) uiState.archivedApps.size else 0
    val appsManagementSubtitle =
            if (managedAppsCount == 0) {
                stringResource(R.string.settings_apps_management_subtitle_empty)
            } else {
                pluralStringResource(
                        R.plurals.settings_apps_management_count,
                        managedAppsCount,
                        managedAppsCount,
                )
            }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
        item {
            SettingsRow(
                    label = stringResource(R.string.settings_look_and_feel_title),
                    subtitle = stringResource(R.string.settings_look_and_feel_subtitle),
                    verticalPadding = 14.dp,
                    onClick = onOpenAppearanceSettings,
                    trailing = { SubpageChevron() },
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
                    onAlignmentChanged = viewModel::setHomeAlignment,
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
                                    uiState.allApps,
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
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_apps)) }
        item {
            SettingsRow(
                    label = stringResource(R.string.settings_apps_management_title),
                    subtitle = appsManagementSubtitle,
                    verticalPadding = 14.dp,
                    onClick = onOpenAppsManagementSettings,
                    trailing = { SubpageChevron() },
            )
        }
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
                                Intent(Intent.ACTION_VIEW, link.url.toUri()).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                        )
                    },
            )
        }
        item { SettingsDivider() }

        item { SectionHeader(stringResource(R.string.settings_section_data)) }
        item {
            ExportLogsRow(
                    context = context,
                    createLogShareIntent = viewModel::createLogShareIntent,
            )
        }
        item {
            SettingsRow(
                    label = stringResource(R.string.settings_reset_all_data),
                    labelStyle =
                            MaterialTheme.typography.bodyLarge.withLauncherTextGlowRecolored(
                                    MaterialTheme.colorScheme.error
                            ),
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
                    Text(
                            stringResource(R.string.settings_reset_confirm_title),
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                text = {
                    Text(
                            stringResource(R.string.settings_reset_confirm_message),
                            color = MaterialTheme.colorScheme.onBackground,
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
                        Text(
                                stringResource(R.string.action_reset),
                                style =
                                        MaterialTheme.typography.labelLarge
                                                .withLauncherTextGlowRecolored(
                                                        MaterialTheme.colorScheme.error
                                                ),
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    FokusTextButton(onClick = onDismissResetConfirm) {
                        Text(
                                stringResource(R.string.action_cancel),
                                color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
        )
    }

    pickerTarget?.let { target ->
        when (target) {
            "swipeLeft",
            "swipeRight" -> {
                ShortcutActionPickerDialog(
                        allActions = uiState.allShortcutActions,
                        allApps = uiState.allApps,
                        title = stringResource(R.string.edit_shortcuts_section_all_actions),
                        onSelect = { action ->
                            onShortcutTargetSelected(target, action)
                            onDismissPicker()
                        },
                        onDismiss = onDismissPicker,
                        includeWidgetPageTarget = true,
                        profileDisplayNameOverrides = uiState.profileDisplayNameOverrides,
                )
            }
            else -> onDismissPicker()
        }
    }
}
