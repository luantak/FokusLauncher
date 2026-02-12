package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.CategoryConstants

/**
 * Bottom sheet shown on long-press of a category chip.
 * Offers: Rename, Edit apps, Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryActionSheet(
    categoryName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onEditApps: () -> Unit,
    onDelete: () -> Unit,
    canEdit: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState()
    var renameMode by remember(categoryName) { mutableStateOf(false) }
    var renameValue by remember(categoryName) { mutableStateOf(categoryName) }
    var errorMessage by remember(categoryName) { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag("category_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Category name (or text field in rename mode)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                if (renameMode) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = renameValue,
                            onValueChange = {
                                renameValue = it
                                errorMessage = null
                            },
                            placeholder = { Text("Category name") },
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rename_inline_input")
                        )
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    TextButton(onClick = {
                        renameMode = false
                        errorMessage = null
                    }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val trimmed = renameValue.trim()
                            when {
                                trimmed.isEmpty() -> {
                                    errorMessage = "Category name cannot be empty"
                                }
                                CategoryConstants.isSystemCategory(trimmed) -> {
                                    errorMessage = "This category name is reserved"
                                }
                                trimmed == categoryName -> {
                                    renameMode = false
                                    errorMessage = null
                                }
                                else -> {
                                    onRename(trimmed)
                                    // Don't dismiss here - let the parent handle success/failure
                                }
                            }
                        }
                    ) { Text("Save") }
                } else {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Actions
            if (!renameMode) {
                if (canEdit) {
                    ActionRow(
                        icon = Icons.Default.Edit,
                        label = "Rename category",
                        onClick = { renameMode = true }
                    )
                    ActionRow(
                        icon = Icons.Default.Settings,
                        label = "Edit apps in category",
                        onClick = {
                            onEditApps()
                            onDismiss()
                        }
                    )
                    ActionRow(
                        icon = Icons.Default.Delete,
                        label = "Delete category",
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        isDestructive = true
                    )
                } else {
                    Text(
                        text = "This category cannot be modified",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )
    }
}
