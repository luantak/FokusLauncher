package com.lu4p.fokuslauncher.ui.drawer

import android.content.Intent
import android.provider.Settings
import com.lu4p.fokuslauncher.ui.components.SheetActionRow
import com.lu4p.fokuslauncher.ui.components.SheetInlineRenameTitleRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
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
            SheetInlineRenameTitleRow(
                    renameMode = renameMode,
                    renameValue = renameValue,
                    onRenameValueChange = { renameValue = it },
                    idleTitle = app.label,
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
                    editIconContentDescription = stringResource(R.string.action_rename),
                    textFieldTestTag = "rename_inline_input",
                    editButtonTestTag = "action_rename",
            )

            if (!isOnHomeScreen) {
                SheetActionRow(
                    label = stringResource(R.string.action_add_to_home),
                    onClick = {
                        onAddToHome(app)
                        onDismiss()
                    },
                    icon = Icons.Default.Home,
                    testTag = "action_add_to_home",
                )
            }

            SheetActionRow(
                label = stringResource(R.string.action_app_info),
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${app.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
                icon = Icons.Default.Info,
                testTag = "action_app_info",
            )

            SheetActionRow(
                label = stringResource(R.string.action_hide),
                onClick = {
                    onHide(app)
                    onDismiss()
                },
                icon = Icons.Default.VisibilityOff,
                testTag = "action_hide",
            )

            SheetActionRow(
                label = stringResource(R.string.action_uninstall),
                onClick = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = "package:${app.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
                icon = Icons.Default.Delete,
                testTag = "action_uninstall",
            )
        }
    }
}
