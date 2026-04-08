package com.lu4p.fokuslauncher.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.ui.components.RenameableBottomSheet
import com.lu4p.fokuslauncher.ui.components.SheetActionRow

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
    RenameableBottomSheet(
            initialLabel = fav.label,
            renameKey = fav.packageName,
            onDismiss = onDismiss,
            onRename = onRename,
            editIconContentDescription = stringResource(R.string.action_rename),
    ) {
        SheetActionRow(
                label = stringResource(R.string.action_remove_from_home),
                onClick = {
                    onRemoveFromHome()
                    onDismiss()
                },
                icon = Icons.Default.Close,
        )

        SheetActionRow(
                label = stringResource(R.string.settings_edit_home_screen),
                onClick = {
                    onEditHomeScreen()
                    onDismiss()
                },
                icon = Icons.Default.Home,
        )

        SheetActionRow(
                label = stringResource(R.string.action_app_info),
                onClick = {
                    onAppInfo()
                    onDismiss()
                },
                icon = Icons.Default.Info,
        )

        SheetActionRow(
                label = stringResource(R.string.action_hide),
                onClick = {
                    onHide()
                    onDismiss()
                },
                icon = Icons.Default.VisibilityOff,
        )

        SheetActionRow(
                label = stringResource(R.string.action_uninstall),
                onClick = {
                    onUninstall()
                    onDismiss()
                },
                icon = Icons.Default.Delete,
        )
    }
}
