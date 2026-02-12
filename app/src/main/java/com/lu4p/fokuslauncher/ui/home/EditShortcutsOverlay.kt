package com.lu4p.fokuslauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.components.MinimalIcons

@Composable
fun EditShortcutsOverlay(viewModel: HomeViewModel) {
    val editShortcuts by viewModel.editRightShortcuts.collectAsStateWithLifecycle()
    val allActions by viewModel.allShortcutActions.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var iconPickerForIndex by remember { mutableStateOf<Int?>(null) }

    val selectedIds = remember(editShortcuts) {
        editShortcuts.map { ShortcutTarget.encode(it.target) }.toSet()
    }
    val uncheckedActions = remember(allActions, selectedIds, searchQuery) {
        allActions
            .filter { it.id !in selectedIds }
            .let { list ->
                if (searchQuery.isBlank()) list
                else list.filter { it.displayLabel.contains(searchQuery, ignoreCase = true) }
            }
    }
    val uncheckedActionsGrouped = remember(uncheckedActions) {
        uncheckedActions
            .groupBy { it.appLabel }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    BackHandler { viewModel.saveEditedRightShortcuts() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit shortcuts",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { viewModel.saveEditedRightShortcuts() }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps and actions") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        ReorderableShortcutList(
            editShortcuts = editShortcuts,
            uncheckedActionsGrouped = uncheckedActionsGrouped,
            onToggleChecked = { target ->
                viewModel.toggleRightShortcut(
                    AppShortcutAction(
                        appLabel = viewModel.formatShortcutTarget(target),
                        actionLabel = AppShortcutAction.OPEN_APP_LABEL,
                        target = target
                    )
                )
            },
            onToggleUnchecked = { action -> viewModel.toggleRightShortcut(action) },
            onReorder = { from, to -> viewModel.reorderRightShortcut(from, to) },
            onOpenIconPicker = { index -> iconPickerForIndex = index },
            formatCheckedLabel = { target -> viewModel.formatShortcutTarget(target) }
        )
    }

    if (iconPickerForIndex != null) {
        IconPickerDialog(
            currentIconName = editShortcuts.getOrNull(iconPickerForIndex!!)?.iconName ?: "circle",
            onSelect = { name ->
                viewModel.updateShortcutIcon(iconPickerForIndex!!, name)
                iconPickerForIndex = null
            },
            onDismiss = { iconPickerForIndex = null }
        )
    }
}

@Composable
private fun ReorderableShortcutList(
    editShortcuts: List<HomeShortcut>,
    uncheckedActionsGrouped: List<Pair<String, List<AppShortcutAction>>>,
    onToggleChecked: (ShortcutTarget) -> Unit,
    onToggleUnchecked: (AppShortcutAction) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onOpenIconPicker: (Int) -> Unit,
    formatCheckedLabel: (ShortcutTarget) -> String
) {
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val resetDragState = {
        draggedIndex = -1
        dragOffset = 0f
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (editShortcuts.isNotEmpty()) {
            item(key = "header_checked_shortcuts") {
                Text(
                    text = "Selected shortcuts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(
            count = editShortcuts.size,
            key = { "checked_shortcut_${ShortcutTarget.encode(editShortcuts[it].target)}" }
        ) { index ->
            val shortcut = editShortcuts[index]
            val offset = if (index == draggedIndex) {
                dragOffset.coerceIn(-itemHeightPx, itemHeightPx)
            } else {
                0f
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { translationY = offset }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(shortcut.target, editShortcuts.size) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffset = 0f
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    if (draggedIndex in editShortcuts.indices) {
                                        dragOffset += amount
                                        while (dragOffset >= itemHeightPx && draggedIndex < editShortcuts.size - 1) {
                                            val from = draggedIndex
                                            val to = (draggedIndex + 1).coerceAtMost(editShortcuts.lastIndex)
                                            if (from == to) break
                                            onReorder(from, to)
                                            draggedIndex = to
                                            dragOffset -= itemHeightPx
                                        }
                                        while (dragOffset <= -itemHeightPx && draggedIndex > 0) {
                                            val from = draggedIndex
                                            val to = (draggedIndex - 1).coerceAtLeast(0)
                                            if (from == to) break
                                            onReorder(from, to)
                                            draggedIndex = to
                                            dragOffset += itemHeightPx
                                        }
                                    }
                                },
                                onDragEnd = { resetDragState() },
                                onDragCancel = { resetDragState() }
                            )
                        }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Checkbox(
                    checked = true,
                    onCheckedChange = { onToggleChecked(shortcut.target) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = MinimalIcons.iconFor(shortcut.iconName),
                    contentDescription = "Change icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onOpenIconPicker(index) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatCheckedLabel(shortcut.target),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        item(key = "header_unchecked_shortcuts") {
            Text(
                text = "All app actions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        uncheckedActionsGrouped.forEach { (appLabel, actions) ->
            item(key = "group_$appLabel") {
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            items(
                items = actions,
                key = { "unchecked_shortcut_${it.id}" }
            ) { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))

                    Checkbox(
                        checked = false,
                        onCheckedChange = { onToggleUnchecked(action) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (action.actionLabel == AppShortcutAction.OPEN_APP_LABEL) "Open app" else action.actionLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun IconPickerDialog(
    currentIconName: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose icon", color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MinimalIcons.names) { name ->
                    Icon(
                        imageVector = MinimalIcons.iconFor(name),
                        contentDescription = name,
                        tint = if (name == currentIconName) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onSelect(name) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
