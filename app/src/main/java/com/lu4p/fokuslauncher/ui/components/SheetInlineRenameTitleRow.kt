package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R

/**
 * Shared title row for bottom sheets: view mode with edit affordance, or inline rename with
 * Cancel/Save.
 */
@Composable
fun SheetInlineRenameTitleRow(
        renameMode: Boolean,
        renameValue: String,
        onRenameValueChange: (String) -> Unit,
        idleTitle: String,
        placeholder: @Composable () -> Unit,
        onStartRename: () -> Unit,
        onCancelRename: () -> Unit,
        onSave: () -> Unit,
        saveEnabled: Boolean = true,
        showEditButton: Boolean = true,
        editIconContentDescription: String,
        modifier: Modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        textFieldModifier: Modifier = Modifier,
        textFieldTestTag: String? = null,
        editButtonTestTag: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (renameMode) {
            OutlinedTextField(
                    value = renameValue,
                    onValueChange = onRenameValueChange,
                    placeholder = placeholder,
                    singleLine = true,
                    modifier =
                            textFieldModifier
                                    .weight(1f)
                                    .let { base ->
                                        if (textFieldTestTag != null) base.testTag(textFieldTestTag)
                                        else base
                                    },
            )
            FokusTextButton(onClick = onCancelRename) {
                Text(stringResource(R.string.action_cancel))
            }
            FokusTextButton(enabled = saveEnabled, onClick = onSave) {
                Text(stringResource(R.string.action_save))
            }
        } else {
            Text(
                    text = idleTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
            )
            if (showEditButton) {
                FokusIconButton(
                        onClick = onStartRename,
                        modifier =
                                if (editButtonTestTag != null)
                                        Modifier.testTag(editButtonTestTag)
                                else Modifier,
                ) {
                    LauncherIcon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = editIconContentDescription,
                            tint = MaterialTheme.colorScheme.onBackground,
                            iconSize = 24.dp,
                    )
                }
            }
        }
    }
}
