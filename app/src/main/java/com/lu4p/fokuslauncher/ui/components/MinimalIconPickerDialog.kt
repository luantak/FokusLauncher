package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.util.resolvedCategoryDrawerIconName
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

/**
 * Searchable Material icon picker (shared by home shortcuts and drawer category icons). Shows the
 * current glyph above the search field.
 */
@Composable
fun MinimalIconPickerDialog(
        storedIconKey: String,
        title: @Composable () -> Unit,
        onSelect: (String) -> Unit,
        onDismiss: () -> Unit,
) {
    var iconSearchQuery by remember { mutableStateOf("") }
    val filteredIconNames =
            remember(iconSearchQuery) {
                val all = MinimalIcons.names
                if (iconSearchQuery.isBlank()) all
                else
                        all.filter { name ->
                            name.containsNormalizedSearch(iconSearchQuery) ||
                                    MinimalIcons.materialOutlinedSearchHaystack(name)
                                            .containsNormalizedSearch(iconSearchQuery)
                        }
            }
    val iconPickerSections =
            remember(iconSearchQuery, filteredIconNames) {
                if (iconSearchQuery.isBlank()) MinimalIcons.iconPickerSections
                else MinimalIcons.iconPickerSearchSections(filteredIconNames)
            }
    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = title,
            text = {
                Column(modifier = Modifier.wrapContentHeight()) {
                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LauncherIcon(
                                imageVector = MinimalIcons.iconFor(storedIconKey),
                                contentDescription = stringResource(R.string.icon_picker_current_icon),
                                tint = MaterialTheme.colorScheme.primary,
                                iconSize = 48.dp,
                        )
                        Text(
                                text = stringResource(R.string.icon_picker_current_icon),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                        )
                    }
                    OutlinedTextField(
                            value = iconSearchQuery,
                            onValueChange = { iconSearchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_icons)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    IconPickerSectionsLazyGrid(
                            sections = iconPickerSections,
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.height(320.dp).padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            variant = IconPickerGridVariant.Dialog,
                            isSelected = {
                                MinimalIcons.iconKeyMatchesStoredIcon(it, storedIconKey)
                            },
                            onSelect = onSelect,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(
                            stringResource(R.string.action_cancel),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
    )
}

@Composable
fun CategoryIconPickerDialog(
        category: String,
        iconOverrides: Map<String, String>,
        onSelect: (String) -> Unit,
        onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resolved = resolvedCategoryDrawerIconName(context, category, iconOverrides)
    MinimalIconPickerDialog(
            storedIconKey = resolved,
            title = {
                Text(
                        stringResource(R.string.category_icon_picker_title),
                        color = MaterialTheme.colorScheme.onBackground
                )
            },
            onSelect = onSelect,
            onDismiss = onDismiss,
    )
}
