package com.lu4p.fokuslauncher.ui.drawer

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.core.net.toUri
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.ui.components.RenameableBottomSheet
import com.lu4p.fokuslauncher.ui.components.SheetActionRow
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel

/**
 * Bottom sheet shown on long-press of an app in the drawer.
 * Offers: Add to home, Rename, App info, Hide, Uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: AppInfo,
    categories: List<String>,
    onDismiss: () -> Unit,
    onAddToHome: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
    onSetCategory: (String) -> Unit,
    onHide: (AppInfo) -> Unit,
    isOnHomeScreen: Boolean = false
) {
    val context = LocalContext.current
    var showingCategoryPicker by remember(app.packageName, app.userHandle) {
        mutableStateOf(false)
    }

    RenameableBottomSheet(
            initialLabel = app.label,
            renameKey = app.packageName,
            onDismiss = onDismiss,
            onRename = onRename,
            editIconContentDescription = stringResource(R.string.action_rename),
            modifier = Modifier.testTag("app_action_sheet"),
            textFieldTestTag = "rename_inline_input",
            editButtonTestTag = "action_rename",
    ) {
        if (showingCategoryPicker) {
            val options =
                    remember(categories, app.category) {
                        (listOf("") + categories + app.category)
                                .map { it.trim() }
                                .distinctBy { it.lowercase() }
                                .filterNot(::isAppCategoryPickerReserved)
                    }
            options.forEach { category ->
                val selected = app.category.equals(category, ignoreCase = true)
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
                label = stringResource(R.string.action_set_category),
                onClick = { showingCategoryPicker = true },
                icon = Icons.Default.Category,
                testTag = "action_set_category",
        )

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

private fun isAppCategoryPickerReserved(category: String): Boolean =
        category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ||
                category.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) ||
                category.equals(ReservedCategoryNames.WORK, ignoreCase = true) ||
                category.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)
