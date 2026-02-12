package com.lu4p.fokuslauncher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.FavoriteApp

/**
 * Bottom sheet "App menu" opened directly on long-pressing a home-screen app.
 * Occupies roughly the lower third to half of the screen.
 *
 * Contents:
 *  - App name (with pencil icon to rename)
 *  - Remove from home screen
 *  - Edit home screen  → opens the edit overlay
 *  - App info           → Android system settings
 *  - Hide               → hide from launcher
 *  - Uninstall          → system uninstall dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppMenuSheet(
    fav: FavoriteApp,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onRemoveFromHome: () -> Unit,
    onEditHomeScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var renameMode by remember(fav.packageName) { mutableStateOf(false) }
    var renameValue by remember(fav.packageName) { mutableStateOf(fav.label) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // ── App name (or text field in rename mode) + pencil icon ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                if (renameMode) {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        placeholder = { Text("App name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { renameMode = false }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val trimmed = renameValue.trim()
                            if (trimmed.isNotEmpty()) {
                                onRename(trimmed)
                                onDismiss()
                            }
                        }
                    ) { Text("Save") }
                } else {
                    Text(
                        text = fav.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { renameMode = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // ── Action rows ────────────────────────────────────────
            MenuActionRow(
                icon = Icons.Default.Close,
                label = "Remove from home screen",
                onClick = {
                    onRemoveFromHome()
                    onDismiss()
                }
            )

            MenuActionRow(
                icon = Icons.Default.Home,
                label = "Edit home screen",
                onClick = {
                    onEditHomeScreen()
                    onDismiss()
                }
            )

            MenuActionRow(
                icon = Icons.Default.Info,
                label = "App info",
                onClick = {
                    onAppInfo()
                    onDismiss()
                }
            )

            MenuActionRow(
                icon = Icons.Default.VisibilityOff,
                label = "Hide",
                onClick = {
                    onHide()
                    onDismiss()
                }
            )

            MenuActionRow(
                icon = Icons.Default.Delete,
                label = "Uninstall",
                onClick = {
                    onUninstall()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
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
            tint = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
