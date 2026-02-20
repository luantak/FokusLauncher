package com.lu4p.fokuslauncher.ui.drawer

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lu4p.fokuslauncher.data.model.AppInfo

/**
 * Bottom sheet shown on long-press of an app in the drawer.
 * Offers: Add to home, Rename, App info, Hide, Uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onAddToHome: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
    onHide: (AppInfo) -> Unit,
    isOnHomeScreen: Boolean = false
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var renameMode by remember(app.packageName) { mutableStateOf(false) }
    var renameValue by remember(app.packageName) { mutableStateOf(app.label) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag("app_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // App name (or text field in rename mode) + pencil icon (same as homepage)
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rename_inline_input")
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
                        text = app.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { renameMode = true },
                        modifier = Modifier.testTag("action_rename")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (!isOnHomeScreen) {
                ActionRow(
                    icon = Icons.Default.Home,
                    label = "Add to home screen",
                    testTag = "action_add_to_home",
                    onClick = {
                        onAddToHome(app)
                        onDismiss()
                    }
                )
            }

            ActionRow(
                icon = Icons.Default.Info,
                label = "App info",
                testTag = "action_app_info",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${app.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )

            ActionRow(
                icon = Icons.Default.VisibilityOff,
                label = "Hide",
                testTag = "action_hide",
                onClick = {
                    onHide(app)
                    onDismiss()
                }
            )

            ActionRow(
                icon = Icons.Default.Delete,
                label = "Uninstall",
                testTag = "action_uninstall",
                onClick = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = "package:${app.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(testTag)
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
