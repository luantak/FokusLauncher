package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.components.SearchablePickerDialog
import com.lu4p.fokuslauncher.ui.drawer.groupShortcutActionsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedShortcutItems
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

/**
 * Same action list as home "Edit shortcuts" (open app + launcher / in-app shortcuts), for picking a
 * [com.lu4p.fokuslauncher.data.model.ShortcutTarget].
 */
@Composable
fun ShortcutActionPickerDialog(
        allActions: List<AppShortcutAction>,
        allApps: List<AppInfo>,
        title: String,
        onSelect: (AppShortcutAction) -> Unit,
        onDismiss: () -> Unit,
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        includeWidgetPageTarget: Boolean = false,
) {
    SearchablePickerDialog(
            title = title,
            onDismiss = onDismiss,
            containerColor = containerColor,
    ) { filter, onFilterChange ->
        val context = LocalContext.current
        val filtered =
                remember(filter, allActions) {
                    if (filter.isBlank()) allActions
                    else allActions.filter { it.displayLabel.containsNormalizedSearch(filter) }
                }
        val sections =
                remember(filtered, allApps, context) {
                    groupShortcutActionsIntoProfileSections(context, filtered, allApps)
                }

        OutlinedTextField(
                value = filter,
                onValueChange = onFilterChange,
                label = { Text(stringResource(R.string.search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
                modifier =
                        Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)
        ) {
            if (includeWidgetPageTarget) {
                item(key = "shortcut_action_widget_page") {
                    Text(
                            text = stringResource(R.string.widget_page_shortcut_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickableWithSystemSound {
                                                onSelect(
                                                        AppShortcutAction(
                                                                appLabel =
                                                                        context.getString(
                                                                                R.string.app_name
                                                                        ),
                                                                actionLabel =
                                                                        context.getString(
                                                                                R.string.widget_page_shortcut_label
                                                                        ),
                                                                target = ShortcutTarget.WidgetPage,
                                                        )
                                                )
                                            }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                    )
                }
            }
            profileGroupedShortcutItems(
                    sections = sections,
                    keyPrefix = "shortcut_action_pick",
                    horizontalPadding = 8.dp,
            ) { action ->
                val line =
                        when {
                            action.actionLabel == AppShortcutAction.OPEN_APP_LABEL -> action.appLabel
                            action.target is ShortcutTarget.PhoneDial -> action.appLabel
                            else -> action.displayLabel
                        }
                Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickableWithSystemSound { onSelect(action) }
                                        .padding(vertical = 10.dp, horizontal = 8.dp)
                )
            }
        }
    }
}
