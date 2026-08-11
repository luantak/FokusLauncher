package com.lu4p.fokuslauncher.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsManagementSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("apps_management_settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_apps_management_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // System app archiving APIs (ApplicationInfo.isArchived, etc.) require API 35+.
            if (Build.VERSION.SDK_INT >= 35) {
                manageableAppsSection(
                        headerRes = R.string.settings_section_archived_apps,
                        emptyTextRes = R.string.settings_no_archived_apps,
                        apps = uiState.archivedApps,
                        key = { "archived_${it.stableKey}" },
                        label = { it.app.label },
                        subtitle = { archived ->
                            archived.profileLabel?.let { pl -> "$pl • ${archived.app.packageName}" }
                                    ?: archived.app.packageName
                        },
                        onRowClick = viewModel::restoreArchivedApp,
                        trailingContent = {
                            Spacer(Modifier.width(8.dp))
                            LauncherIcon(
                                    Icons.Default.Restore,
                                    stringResource(R.string.cd_restore_archived_app),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    iconSize = 24.dp,
                            )
                        },
                )
                item { SettingsDivider() }
            }

            manageableAppsSection(
                    headerRes = R.string.settings_section_hidden_apps,
                    emptyTextRes = R.string.settings_no_hidden_apps,
                    apps = uiState.hiddenApps,
                    key = { "hidden_${it.stableKey}" },
                    label = { it.label },
                    subtitle = { app ->
                        app.profileLabel?.let { pl -> "$pl • ${app.packageName}" } ?: app.packageName
                    },
                    onRowClick = {
                        viewModel.unhideApp(it.packageName, it.profileKey, it.launcherShortcutId)
                    },
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
                    key = { "renamed_${it.stableKey}" },
                    label = { it.customName },
                    subtitle = { app ->
                        app.profileLabel?.let { pl -> "$pl • ${app.packageName}" } ?: app.packageName
                    },
                    onRowClick = {
                        viewModel.removeRename(it.packageName, it.profileKey, it.launcherShortcutId)
                    },
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
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
