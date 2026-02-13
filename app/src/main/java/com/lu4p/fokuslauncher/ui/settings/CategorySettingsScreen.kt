package com.lu4p.fokuslauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit,
        onEditCategoryApps: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var newCategory by remember { mutableStateOf("") }
    val normalizedNewCategory = newCategory.trim()
    val canAddCategory =
            normalizedNewCategory.isNotBlank() &&
                    !normalizedNewCategory.equals("All apps", ignoreCase = true) &&
                    !normalizedNewCategory.equals("Private", ignoreCase = true)
    val categories = remember(uiState.allApps, uiState.appCategories, uiState.categoryDefinitions) {
        deriveEditableCategories(uiState)
    }
    val appCounts = remember(uiState.allApps, uiState.appCategories) { buildCategoryCounts(uiState) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
                title = { Text("App Categories", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
        )
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    singleLine = true,
                    label = { Text("New category") },
                    modifier = Modifier.weight(1f)
            )
            TextButton(
                    enabled = canAddCategory,
                    onClick = {
                        viewModel.addCategoryDefinition(newCategory)
                        newCategory = ""
                    }
            ) {
                Text("Add")
            }
        }

        ReorderableCategoryList(
                categories = categories,
                counts = appCounts,
                onReorder = { from, to ->
                    val reordered = categories.toMutableList()
                    val item = reordered.removeAt(from)
                    reordered.add(to, item)
                    viewModel.reorderCategories(reordered)
                },
                onEditCategoryApps = onEditCategoryApps,
                onDelete = { viewModel.deleteCategory(it) }
        )
    }
}

@Composable
private fun ReorderableCategoryList(
        categories: List<String>,
        counts: Map<String, Int>,
        onReorder: (Int, Int) -> Unit,
        onEditCategoryApps: (String) -> Unit,
        onDelete: (String) -> Unit
) {
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val resetDragState = {
        draggedIndex = -1
        dragOffset = 0f
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(count = categories.size, key = { categories[it] }) { index ->
            val category = categories[index]
            val offset = if (index == draggedIndex) dragOffset.coerceIn(-itemHeightPx, itemHeightPx) else 0f
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(56.dp)
                                    .graphicsLayer { translationY = offset }
                                    .clickable { onEditCategoryApps(category) }
                                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                                Modifier.size(24.dp)
                                        .pointerInput(category, categories.size) {
                                            detectVerticalDragGestures(
                                                    onDragStart = {
                                                        draggedIndex = index
                                                        dragOffset = 0f
                                                    },
                                                    onVerticalDrag = { change, amount ->
                                                        change.consume()
                                                        if (draggedIndex in categories.indices) {
                                                            dragOffset += amount
                                                            while (dragOffset >= itemHeightPx && draggedIndex < categories.size - 1) {
                                                                val from = draggedIndex
                                                                val to = draggedIndex + 1
                                                                onReorder(from, to)
                                                                draggedIndex = to
                                                                dragOffset -= itemHeightPx
                                                            }
                                                            while (dragOffset <= -itemHeightPx && draggedIndex > 0) {
                                                                val from = draggedIndex
                                                                val to = draggedIndex - 1
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = category,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                            text = "${counts[category] ?: 0} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                    )
                }
                TextButton(onClick = { onEditCategoryApps(category) }) { Text("Edit apps") }
                IconButton(onClick = { onDelete(category) }) {
                    Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAppsScreen(
        category: String,
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val checkedPackages = remember(uiState.allApps, uiState.appCategories, category) {
        uiState.allApps.filter { app ->
            val effective = uiState.appCategories[app.packageName] ?: app.category
            effective.equals(category, ignoreCase = true)
        }.map { it.packageName }.toSet()
    }
    val checkedApps = remember(uiState.allApps, checkedPackages) {
        uiState.allApps.filter { it.packageName in checkedPackages }.sortedBy { it.label.lowercase() }
    }
    val uncheckedApps = remember(uiState.allApps, checkedPackages, searchQuery) {
        uiState.allApps.filter { it.packageName !in checkedPackages }
                .let { apps ->
                    if (searchQuery.isBlank()) apps
                    else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                }
                .sortedBy { it.label.lowercase() }
    }

    BackHandler { onNavigateBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
                title = { Text(category, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
        )
        OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (checkedApps.isNotEmpty()) {
                item {
                    Text(
                            text = "Apps in category",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            items(checkedApps, key = { "checked_${it.packageName}" }) { app ->
                CategoryAppRow(
                        label = app.label,
                        checked = true,
                        secondary = category,
                        onToggle = { viewModel.setAppCategory(app.packageName, "") }
                )
            }
            item {
                Text(
                        text = "All apps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(uncheckedApps, key = { "unchecked_${it.packageName}" }) { app ->
                val currentCategory = (uiState.appCategories[app.packageName] ?: app.category).ifBlank { "No category" }
                CategoryAppRow(
                        label = app.label,
                        checked = false,
                        secondary = currentCategory,
                        onToggle = { viewModel.setAppCategory(app.packageName, category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryAppRow(
        label: String,
        checked: Boolean,
        secondary: String,
        onToggle: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .height(56.dp)
                            .clickable(onClick = onToggle)
                            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun deriveEditableCategories(uiState: SettingsUiState): List<String> {
    val orderedDefined = uiState.categoryDefinitions.distinct()
    val dynamic =
            uiState.allApps.mapNotNull { app ->
                        val category = uiState.appCategories[app.packageName] ?: app.category
                        category.takeIf { it.isNotBlank() }
                    }
                    .toSet()
    val extras = (dynamic - orderedDefined.toSet()).toList().sorted()
    return orderedDefined + extras
}

private fun buildCategoryCounts(uiState: SettingsUiState): Map<String, Int> {
    return uiState.allApps.map { app ->
                val effective = uiState.appCategories[app.packageName] ?: app.category
                effective.trim()
            }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
}
