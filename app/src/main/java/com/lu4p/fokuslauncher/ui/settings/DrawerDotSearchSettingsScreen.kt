package com.lu4p.fokuslauncher.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedAppItems
import com.lu4p.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

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
    var showAddShortcutChoice by remember { mutableStateOf(false) }
    var showDefaultPicker by remember { mutableStateOf(false) }
    var showDefaultUrlTemplate by remember { mutableStateOf(false) }
    var showAliasAppPicker by remember { mutableStateOf(false) }
    var showAliasCharForUrlTemplate by remember { mutableStateOf(false) }
    var pendingAliasApp by remember { mutableStateOf<AppInfo?>(null) }
    var pendingUrlAliasChar by remember { mutableStateOf<Char?>(null) }

    val defaultSummary =
            remember(uiState.defaultTarget, uiState.allApps, context) {
                val pref = uiState.defaultTarget
                if (pref.target == null) {
                    context.getString(R.string.settings_dot_search_default_system)
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
                TopAppBar(
                        title = {
                            Text(
                                    stringResource(R.string.settings_dot_search_screen_title),
                                    color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        stringResource(R.string.action_back),
                                        tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddShortcutChoice = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.settings_dot_search_add_alias))
                }
            }
    ) { padding ->
        LazyColumn(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
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
            items(sortedAliases, key = { it.key }) { (ch, pref) ->
                val label =
                        formatShortcutTargetDisplay(
                                context = context,
                                target = pref.target,
                                allApps = uiState.allApps,
                                notSetLabel = "",
                                profileKey = pref.profileKey
                        )
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                ".${ch} …",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = { viewModel.removeAlias(ch) }) {
                        Icon(
                                Icons.Default.Close,
                                stringResource(R.string.cd_remove_alias),
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showAddShortcutChoice) {
        AlertDialog(
                onDismissRequest = { showAddShortcutChoice = false },
                title = {
                    Text(
                            stringResource(R.string.settings_dot_search_add_shortcut_title),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    Column {
                        TextButton(
                                onClick = {
                                    showAddShortcutChoice = false
                                    showAliasAppPicker = true
                                }
                        ) {
                            Text(stringResource(R.string.settings_dot_search_add_shortcut_app))
                        }
                        TextButton(
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
                    TextButton(onClick = { showAddShortcutChoice = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showDefaultPicker) {
        DotSearchAppPickerDialog(
                apps = uiState.webSearchCapableApps,
                title = stringResource(R.string.settings_dot_search_pick_default_app),
                onSelect = { app ->
                    viewModel.setDefaultFromApp(app)
                    showDefaultPicker = false
                },
                onDismiss = { showDefaultPicker = false }
        )
    }

    if (showDefaultUrlTemplate) {
        DotSearchUrlTemplateDialog(
                title = stringResource(R.string.settings_dot_search_url_template_dialog_title),
                helpText = urlTemplateHelp,
                validationErrorText = urlTemplateError,
                onConfirm = { url ->
                    viewModel.setDefaultFromUrlTemplate(url)
                    showDefaultUrlTemplate = false
                },
                onDismiss = { showDefaultUrlTemplate = false }
        )
    }

    if (showAliasAppPicker) {
        DotSearchAppPickerDialog(
                apps = uiState.webSearchCapableApps,
                title = stringResource(R.string.settings_dot_search_pick_alias_app),
                onSelect = { app ->
                    showAliasAppPicker = false
                    pendingAliasApp = app
                },
                onDismiss = { showAliasAppPicker = false }
        )
    }

    pendingAliasApp?.let { app ->
        AliasCharDialog(
                onConfirm = { raw ->
                    val c = raw.trim().firstOrNull()?.lowercaseChar()
                    when {
                        c == null ->
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_dot_search_invalid_alias),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        !DrawerDotSearchSettingsViewModel.isValidAliasChar(c) ->
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_dot_search_invalid_alias),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        viewModel.aliasCharTaken(c) ->
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_dot_search_alias_taken),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        else -> {
                            viewModel.setAlias(c, app)
                            pendingAliasApp = null
                        }
                    }
                },
                onDismiss = { pendingAliasApp = null }
        )
    }

    if (showAliasCharForUrlTemplate) {
        AliasCharDialog(
                onConfirm = { raw ->
                    val c = raw.trim().firstOrNull()?.lowercaseChar()
                    when {
                        c == null ->
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_dot_search_invalid_alias),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        !DrawerDotSearchSettingsViewModel.isValidAliasChar(c) ->
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_dot_search_invalid_alias),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        viewModel.aliasCharTaken(c) ->
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_dot_search_alias_taken),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        else -> {
                            showAliasCharForUrlTemplate = false
                            pendingUrlAliasChar = c
                        }
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
                    pendingUrlAliasChar = null
                },
                onDismiss = { pendingUrlAliasChar = null }
        )
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
            TextButton(onClick = onPickApp) { Text(stringResource(R.string.action_change)) }
            TextButton(onClick = onPickUrlTemplate) {
                Text(stringResource(R.string.settings_dot_search_url_template))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onClear != null) {
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
    }
}

@Composable
private fun DotSearchAppPickerDialog(
        apps: List<AppInfo>,
        title: String,
        onSelect: (AppInfo) -> Unit,
        onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val context = LocalContext.current
    val emptyLabel = stringResource(R.string.settings_dot_search_no_web_search_apps)
    val filtered =
            remember(filter, apps) {
                if (filter.isBlank()) apps
                else apps.filter { it.label.containsNormalizedSearch(filter) }
            }
    val filteredSections =
            remember(filtered, context) {
                groupAppsIntoProfileSections(context, filtered, ::sortAppsAlphabeticallyByProfileSection)
            }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    if (apps.isEmpty()) {
                        Text(
                                emptyLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                    OutlinedTextField(
                            value = filter,
                            onValueChange = { filter = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        profileGroupedAppItems(
                                sections = filteredSections,
                                keyPrefix = "dot_search_pick",
                                horizontalPadding = 8.dp,
                        ) { app ->
                            Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable { onSelect(app) }
                                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
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
    AlertDialog(
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
                                val e = error
                                if (e != null) {
                                    Text(e, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            if (!DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate(value)) {
                                error = validationErrorText
                            } else {
                                onConfirm(value.trim())
                            }
                        }
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun AliasCharDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
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
                TextButton(onClick = { onConfirm(value) }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
