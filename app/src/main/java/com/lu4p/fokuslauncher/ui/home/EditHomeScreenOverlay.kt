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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp

/**
 * Fullscreen overlay for editing which apps appear on the home screen.
 *
 * Checked apps (on home screen) appear at the top and can be reordered
 * by dragging the handle. Unchecked apps are listed alphabetically below.
 */
@Composable
fun EditHomeScreenOverlay(viewModel: HomeViewModel) {
    val editFavorites by viewModel.editFavorites.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val checkedPackages = remember(editFavorites) {
        editFavorites.map { it.packageName }.toSet()
    }
    val uncheckedApps = remember(allApps, checkedPackages, searchQuery) {
        allApps.filter { it.packageName !in checkedPackages }
            .let { list ->
                if (searchQuery.isBlank()) list
                else list.filter { it.label.contains(searchQuery, ignoreCase = true) }
            }
            .sortedBy { it.label.lowercase() }
    }

    BackHandler { viewModel.saveEditedFavorites() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit home screen",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { viewModel.saveEditedFavorites() }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Search bar ─────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── Checked items (reorderable) + unchecked items ──────────
        ReorderableAppList(
            editFavorites = editFavorites,
            uncheckedApps = uncheckedApps,
            allApps = allApps,
            onToggle = { viewModel.toggleAppOnHomeScreen(it) },
            onReorder = { from, to -> viewModel.reorderFavorite(from, to) }
        )
    }
}

@Composable
private fun ReorderableAppList(
    editFavorites: List<FavoriteApp>,
    uncheckedApps: List<AppInfo>,
    allApps: List<AppInfo>,
    onToggle: (AppInfo) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val resetDragState = {
        draggedIndex = -1
        dragOffset = 0f
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Section: Home screen apps ──────────────────────────────
        if (editFavorites.isNotEmpty()) {
            item(key = "header_checked") {
                Text(
                    text = "Home screen apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(
            count = editFavorites.size,
            key = { "checked_${editFavorites[it].packageName}" }
        ) { index ->
            val fav = editFavorites[index]
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
                // Drag handle
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(fav.packageName, editFavorites.size) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffset = 0f
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    if (draggedIndex in editFavorites.indices) {
                                        dragOffset += amount

                                        // Move by full row steps to avoid half-threshold jitter.
                                        while (dragOffset >= itemHeightPx && draggedIndex < editFavorites.size - 1) {
                                            val from = draggedIndex
                                            val to = (draggedIndex + 1).coerceAtMost(editFavorites.lastIndex)
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
                    onCheckedChange = {
                        allApps.find { it.packageName == fav.packageName }
                            ?.let { onToggle(it) }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = fav.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Section: All apps ──────────────────────────────────────
        item(key = "header_unchecked") {
            Text(
                text = "All apps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(
            items = uncheckedApps,
            key = { "unchecked_${it.packageName}" }
        ) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder for drag handle alignment
                Spacer(modifier = Modifier.size(24.dp))

                Spacer(modifier = Modifier.width(8.dp))

                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggle(app) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
