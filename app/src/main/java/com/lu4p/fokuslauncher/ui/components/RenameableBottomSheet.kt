package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lu4p.fokuslauncher.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameableBottomSheet(
        initialLabel: String,
        renameKey: Any?,
        onDismiss: () -> Unit,
        onRename: (String) -> Unit,
        editIconContentDescription: String,
        modifier: Modifier = Modifier,
        textFieldTestTag: String? = null,
        editButtonTestTag: String? = null,
        content: @Composable ColumnScope.() -> Unit,
) {
    FokusBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        var renameMode by remember(renameKey) { mutableStateOf(false) }
        var renameValue by remember(renameKey) { mutableStateOf(initialLabel) }

        SheetInlineRenameTitleRow(
                renameMode = renameMode,
                renameValue = renameValue,
                onRenameValueChange = { renameValue = it },
                idleTitle = initialLabel,
                placeholder = { Text(stringResource(R.string.app_name_placeholder)) },
                onStartRename = { renameMode = true },
                onCancelRename = { renameMode = false },
                onSave = {
                    val trimmed = renameValue.trim()
                    if (trimmed.isNotEmpty()) {
                        onRename(trimmed)
                        onDismiss()
                    }
                },
                editIconContentDescription = editIconContentDescription,
                textFieldTestTag = textFieldTestTag,
                editButtonTestTag = editButtonTestTag,
        )

        content()
    }
}
