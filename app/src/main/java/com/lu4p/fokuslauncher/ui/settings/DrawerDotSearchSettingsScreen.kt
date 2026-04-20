package com.lu4p.fokuslauncher.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.theme.launcherIconDp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.drawer.GroupedAppPickerDialog
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.theme.withLauncherTextGlowRecolored
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound

/** Token stored in URL templates; validated by [DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate]. */
private const val DotSearchUrlQueryPlaceholder = "%q"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerDotSearchSettingsScreen(
        viewModel: DrawerDotSearchSettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultSystemLabel = stringResource(R.string.settings_dot_search_default_system)
    val toastInvalidAlias = stringResource(R.string.settings_dot_search_invalid_alias)
    val toastAliasTaken = stringResource(R.string.settings_dot_search_alias_taken)
    var showAddShortcutChoice by remember { mutableStateOf(false) }
    var showDefaultPicker by remember { mutableStateOf(false) }
    var showDefaultUrlTemplate by remember { mutableStateOf(false) }
    var showAliasAppPicker by remember { mutableStateOf(false) }
    var showAliasCharForUrlTemplate by remember { mutableStateOf(false) }
    var pendingAliasApp by remember { mutableStateOf<AppInfo?>(null) }
    var pendingUrlAliasChar by remember { mutableStateOf<Char?>(null) }

    val defaultSummary =
            remember(uiState.defaultTarget, uiState.allApps, defaultSystemLabel) {
                val pref = uiState.defaultTarget
                if (pref.target == null) {
                    defaultSystemLabel
                } else {
                    formatShortcutTargetDisplay(
                            context = context,
                            target = pref.target,
                            allApps = uiState.allApps,
                            notSetLabel =
                                    (pref.target as? ShortcutTarget.App)?.packageName ?: "",
                            profileKey = pref.profileKey
                    )
                }
            }

    val urlTemplateError =
            stringResource(
                    R.string.settings_dot_search_url_template_error,
                    DotSearchUrlQueryPlaceholder
            )
    val urlTemplateHelp =
            stringResource(
                    R.string.settings_dot_search_url_template_help,
                    DotSearchUrlQueryPlaceholder
            )

    Scaffold(
            containerColor = backgroundScrim,
            topBar = {
                FokusSettingsTopBar(
                        titleText = stringResource(R.string.settings_dot_search_screen_title),
                        onNavigateBack = onNavigateBack,
                        containerColor = Color.Transparent,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                        onClick =
                                rememberClickWithSystemSound {
                                    showAddShortcutChoice = true
                                }
                ) {
                    LauncherIcon(
                            Icons.Default.Add,
                            stringResource(R.string.settings_dot_search_add_alias),
                            iconSize = 24.dp,
                    )
                }
            }
    ) { padding ->
        LazyColumn(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp)
        ) {
            item {
                Text(
                        text =
                                stringResource(
                                        R.string.settings_dot_search_explanation,
                                        DotSearchUrlQueryPlaceholder
                                ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            item {
                DotSearchTargetSettingsRow(
                        label = stringResource(R.string.settings_dot_search_default_label),
                        summary = defaultSummary,
                        onPickApp = { showDefaultPicker = true },
                        onPickUrlTemplate = { showDefaultUrlTemplate = true },
                        onClear =
                                if (uiState.defaultTarget.target != null) {
                                    { viewModel.clearDefaultTarget() }
                                } else null
                )
            }
            item {
                Text(
                        text = stringResource(R.string.settings_dot_search_aliases_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            val sortedAliases = uiState.aliases.entries.sortedBy { it.key }
            items(sortedAliases, key = { it.key }) { entry ->
                val label =
                        formatShortcutTargetDisplay(
                                context = context,
                                target = entry.value.target,
                                allApps = uiState.allApps,
                                notSetLabel = "",
                                profileKey = entry.value.profileKey
                        )
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                ".${entry.key} …",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    FokusIconButton(onClick = { viewModel.removeAlias(entry.key) }) {
                        LauncherIcon(
                                Icons.Default.Close,
                                stringResource(R.string.cd_remove_alias),
                                tint = MaterialTheme.colorScheme.error,
                                iconSize = 24.dp,
                        )
                    }
                }
            }
        }
    }

    if (showAddShortcutChoice) {
        FokusAlertDialog(
                onDismissRequest = { showAddShortcutChoice = false },
                title = {
                    Text(
                            stringResource(R.string.settings_dot_search_add_shortcut_title),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    Column {
                        FokusTextButton(
                                onClick = {
                                    showAddShortcutChoice = false
                                    showAliasAppPicker = true
                                }
                        ) {
                            Text(stringResource(R.string.settings_dot_search_add_shortcut_app))
                        }
                        FokusTextButton(
                                onClick = {
                                    showAddShortcutChoice = false
                                    showAliasCharForUrlTemplate = true
                                }
                        ) {
                            Text(stringResource(R.string.settings_dot_search_add_shortcut_url))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    FokusTextButton(onClick = { showAddShortcutChoice = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
        )
    }

    if (showDefaultPicker) {
        GroupedAppPickerDialog(
                apps = uiState.webSearchCapableApps,
                title = stringResource(R.string.settings_dot_search_pick_default_app),
                keyPrefix = "dot_search_pick",
                onSelect = { app ->
                    viewModel.setDefaultFromApp(app)
                    showDefaultPicker = false
                },
                onDismiss = { showDefaultPicker = false },
                searchLabel = null,
                emptyStateText = stringResource(R.string.settings_dot_search_no_web_search_apps),
                useSystemSoundOnItemClick = false,
        )
    }

    if (showDefaultUrlTemplate) {
        DotSearchUrlTemplateDialog(
                title = stringResource(R.string.settings_dot_search_url_template_dialog_title),
                helpText = urlTemplateHelp,
                validationErrorText = urlTemplateError,
                onConfirm = { url ->
                    viewModel.setDefaultFromUrlTemplate(url)
                },
                onDismiss = { showDefaultUrlTemplate = false }
        )
    }

    if (showAliasAppPicker) {
        GroupedAppPickerDialog(
                apps = uiState.webSearchCapableApps,
                title = stringResource(R.string.settings_dot_search_pick_alias_app),
                keyPrefix = "dot_search_pick_alias",
                onSelect = { app ->
                    pendingAliasApp = app
                    showAliasAppPicker = false
                },
                onDismiss = { showAliasAppPicker = false },
                searchLabel = null,
                emptyStateText = stringResource(R.string.settings_dot_search_no_web_search_apps),
                useSystemSoundOnItemClick = false,
        )
    }

    pendingAliasApp?.let { app ->
        AliasCharDialog(
                onConfirm = { raw ->
                    parseDotSearchAliasCharOrToast(
                                    context,
                                    raw,
                                    toastInvalidAlias,
                                    toastAliasTaken,
                                    viewModel,
                            )
                            ?.let { c ->
                                viewModel.setAlias(c, app)
                                pendingAliasApp = null
                            }
                },
                onDismiss = { pendingAliasApp = null }
        )
    }

    if (showAliasCharForUrlTemplate) {
        AliasCharDialog(
                onConfirm = { raw ->
                    parseDotSearchAliasCharOrToast(
                                    context,
                                    raw,
                                    toastInvalidAlias,
                                    toastAliasTaken,
                                    viewModel,
                            )
                            ?.let { c ->
                                pendingUrlAliasChar = c
                                showAliasCharForUrlTemplate = false
                            }
                },
                onDismiss = { showAliasCharForUrlTemplate = false }
        )
    }

    pendingUrlAliasChar?.let { ch ->
        DotSearchUrlTemplateDialog(
                title = stringResource(R.string.settings_dot_search_url_template_for_alias, ch),
                helpText = urlTemplateHelp,
                validationErrorText = urlTemplateError,
                onConfirm = { url ->
                    viewModel.setAliasFromUrlTemplate(ch, url)
                },
                onDismiss = { pendingUrlAliasChar = null }
        )
    }
}

private fun parseDotSearchAliasCharOrToast(
        context: Context,
        raw: String,
        toastInvalidAlias: String,
        toastAliasTaken: String,
        viewModel: DrawerDotSearchSettingsViewModel,
): Char? {
    val c = raw.trim().firstOrNull()?.lowercaseChar()
    return when {
        c == null || !DrawerDotSearchSettingsViewModel.isValidAliasChar(c) -> {
            Toast.makeText(context, toastInvalidAlias, Toast.LENGTH_SHORT).show()
            null
        }
        viewModel.aliasCharTaken(c) -> {
            Toast.makeText(context, toastAliasTaken, Toast.LENGTH_SHORT).show()
            null
        }
        else -> c
    }
}

@Composable
private fun DotSearchTargetSettingsRow(
        label: String,
        summary: String,
        onPickApp: () -> Unit,
        onPickUrlTemplate: () -> Unit,
        onClear: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
        Text(
                summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
            FokusTextButton(onClick = onPickUrlTemplate) {
                Text(stringResource(R.string.settings_dot_search_url_template))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onClear != null) {
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
}

@Composable
private fun DotSearchUrlTemplateDialog(
        title: String,
        helpText: String,
        validationErrorText: String,
        onConfirm: (String) -> Unit,
        onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                            text = helpText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                error = null
                            },
                            minLines = 2,
                            maxLines = 4,
                            isError = error != null,
                            supportingText = {
                                error?.let { err ->
                                    Text(
                                            err,
                                            style =
                                                    MaterialTheme.typography.bodySmall
                                                            .withLauncherTextGlowRecolored(
                                                                    MaterialTheme.colorScheme
                                                                            .error
                                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FokusTextButton(
                        onClick = {
                            if (!DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate(value)) {
                                error = validationErrorText
                            } else {
                                onConfirm(value.trim())
                                onDismiss()
                            }
                        }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = { FokusTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun AliasCharDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        stringResource(R.string.settings_dot_search_alias_dialog_title),
                        color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                OutlinedTextField(
                        value = value,
                        onValueChange = { v ->
                            value =
                                    v.filter { ch -> ch in 'a'..'z' }
                                            .take(1)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                FokusTextButton(onClick = { onConfirm(value) }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                FokusTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
    )
}
