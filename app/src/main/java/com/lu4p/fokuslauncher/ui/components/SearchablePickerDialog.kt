package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.lu4p.fokuslauncher.R

/**
 * Alert dialog with owned search/filter state; [content] builds the body (typically a search field
 * plus list) from the current filter and update callback.
 */
@Composable
fun SearchablePickerDialog(
        title: String,
        onDismiss: () -> Unit,
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        content: @Composable ColumnScope.(filter: String, onFilterChange: (String) -> Unit) -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
            text = { Column { content(filter) { filter = it } } },
            confirmButton = {},
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = containerColor,
    )
}
