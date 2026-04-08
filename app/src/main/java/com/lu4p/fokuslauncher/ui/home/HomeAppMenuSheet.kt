package com.lu4p.fokuslauncher.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
        val actions: List<Triple<Int, ImageVector, () -> Unit>> =
                listOf(
                        Triple(R.string.action_remove_from_home, Icons.Default.Close, onRemoveFromHome),
                        Triple(R.string.settings_edit_home_screen, Icons.Default.Home, onEditHomeScreen),
                        Triple(R.string.action_app_info, Icons.Default.Info, onAppInfo),
                        Triple(R.string.action_hide, Icons.Default.VisibilityOff, onHide),
                        Triple(R.string.action_uninstall, Icons.Default.Delete, onUninstall),
                )
        actions.forEach { (labelRes, icon, action) ->
            SheetActionRow(
                    label = stringResource(labelRes),
                    onClick = {
                        action()
                        onDismiss()
                    },
                    icon = icon,
            )
        }
    }
}
