package com.lu4p.fokuslauncher.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.ui.components.RenameableBottomSheet
import com.lu4p.fokuslauncher.ui.components.SheetActionRow
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel

/**
 * Bottom sheet "App menu" opened directly on long-pressing a home-screen app.
 * Occupies roughly the lower third to half of the screen.
 *
 * Contents:
 *  - App name (with pencil icon to rename)
 *  - Remove from home screen
 *  - Home screen (edit icon) → opens the edit overlay
 *  - App info           → Android system settings
 *  - Hide               → hide from launcher
 *  - Uninstall          → system uninstall dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppMenuSheet(
    fav: FavoriteApp,
    currentCategory: String,
    categoryOptions: List<String>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onSetCategory: (String) -> Unit,
    onRemoveFromHome: () -> Unit,
    onEditHomeScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit
) {
    var showingCategoryPicker by remember(fav.packageName, fav.profileKey) { mutableStateOf(false) }
    val context = LocalContext.current
    RenameableBottomSheet(
            initialLabel = fav.label,
            renameKey = fav.packageName,
            onDismiss = onDismiss,
            onRename = onRename,
            editIconContentDescription = stringResource(R.string.action_rename),
    ) {
        if (showingCategoryPicker) {
            val options =
                    remember(categoryOptions, currentCategory) {
                        (listOf("") + categoryOptions + currentCategory)
                                .map { it.trim() }
                                .distinctBy { it.lowercase() }
                    }
            options.forEach { category ->
                val selected = currentCategory.equals(category, ignoreCase = true)
                val label =
                        if (category.isBlank()) {
                            stringResource(R.string.category_no_category)
                        } else {
                            categoryChipDisplayLabel(context, category)
                        }
                SheetActionRow(
                        label = label,
                        onClick = { onSetCategory(category) },
                        icon = if (selected) Icons.Default.Check else null,
                        iconContentDescription = label,
                        leadingContent =
                                if (selected) {
                                    null
                                } else {
                                    { Spacer(modifier = Modifier.width(24.dp)) }
                                },
                        testTag = "action_set_category_${category.ifBlank { "none" }}",
                )
            }
            return@RenameableBottomSheet
        }

        val actions: List<Triple<Int, ImageVector, () -> Unit>> =
                listOf(
                        Triple(
                                R.string.action_set_category,
                                Icons.Default.Category,
                                { showingCategoryPicker = true },
                        ),
                        Triple(R.string.action_remove_from_home, Icons.Default.Close, onRemoveFromHome),
                        Triple(R.string.settings_nav_home_screen, Icons.Outlined.Edit, onEditHomeScreen),
                        Triple(R.string.action_app_info, Icons.Default.Info, onAppInfo),
                        Triple(R.string.action_hide, Icons.Default.VisibilityOff, onHide),
                        Triple(R.string.action_uninstall, Icons.Default.Delete, onUninstall),
                )
        actions.forEach { (labelRes, icon, action) ->
            val iconCdRes =
                    when (labelRes) {
                        R.string.settings_nav_home_screen -> R.string.cd_edit_home_screen
                        else -> labelRes
                    }
            SheetActionRow(
                    label = stringResource(labelRes),
                    onClick = {
                        action()
                        if (labelRes != R.string.action_set_category) onDismiss()
                    },
                    icon = icon,
                    iconContentDescription = stringResource(iconCdRes),
            )
        }
    }
}
