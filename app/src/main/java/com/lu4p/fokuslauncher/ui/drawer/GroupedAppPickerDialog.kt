package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

/**
 * Searchable app list grouped by work profile / owner, used by weather picker, settings,
 * and dot-search flows.
 */
@Composable
fun GroupedAppPickerDialog(
        apps: List<AppInfo>,
        title: String,
        keyPrefix: String,
        onSelect: (AppInfo) -> Unit,
        onDismiss: () -> Unit,
        searchLabel: (@Composable () -> Unit)? = { Text(stringResource(R.string.search)) },
        emptyStateText: String? = null,
        useSystemSoundOnItemClick: Boolean = true,
) {
    var filter by remember { mutableStateOf("") }
    val context = LocalContext.current
    val filtered =
            remember(filter, apps) {
                if (filter.isBlank()) apps
                else apps.filter { it.label.containsNormalizedSearch(filter) }
            }
    val filteredSections =
            remember(filtered, context) {
                groupAppsIntoProfileSections(
                        context,
                        filtered,
                        ::sortAppsAlphabeticallyByProfileSection
                )
            }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    if (emptyStateText != null && apps.isEmpty()) {
                        Text(
                                emptyStateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        OutlinedTextField(
                                value = filter,
                                onValueChange = { filter = it },
                                label = searchLabel,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            profileGroupedAppItems(
                                    sections = filteredSections,
                                    keyPrefix = keyPrefix,
                                    horizontalPadding = 8.dp,
                            ) { app ->
                                val labelStyle = MaterialTheme.typography.bodyLarge
                                val labelColor = MaterialTheme.colorScheme.onBackground
                                val rowModifier =
                                        Modifier.fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                Text(
                                        text = app.label,
                                        style = labelStyle,
                                        color = labelColor,
                                        modifier =
                                                if (useSystemSoundOnItemClick) {
                                                    rowModifier.clickableWithSystemSound {
                                                        onSelect(app)
                                                    }
                                                } else {
                                                    rowModifier.clickable { onSelect(app) }
                                                }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
