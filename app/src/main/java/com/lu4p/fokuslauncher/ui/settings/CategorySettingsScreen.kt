package com.lu4p.fokuslauncher.ui.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AddCategoryResult
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.categoryAddFieldFailure
import com.lu4p.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedAppItems
import com.lu4p.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.ui.components.CategoryIconPickerDialog
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel
import com.lu4p.fokuslauncher.ui.util.resolvedCategoryDrawerIconName
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit,
        onEditCategoryApps: (String) -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var newCategory by remember { mutableStateOf("") }
    val normalizedNewCategory = newCategory.trim()
    val inlineFieldFailure: AddCategoryResult.Failure? =
            remember(newCategory, context, uiState.categoryDefinitions) {
                categoryAddFieldFailure(context, newCategory, uiState.categoryDefinitions)
            }
    val canAddCategory = inlineFieldFailure == null && normalizedNewCategory.isNotBlank()
    val categories =
            remember(uiState.allApps, uiState.categoryDefinitions) { deriveEditableCategories(uiState) }
    var localCategories by remember(categories) { mutableStateOf(categories) }
    val appCounts = remember(uiState.allApps) { buildCategoryCounts(uiState) }
    var categoryIconPickerFor by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.addCategoryResults.collect { result ->
            when (result) {
                AddCategoryResult.Success -> newCategory = ""
                is AddCategoryResult.Failure -> {
                    snackbarHostState.showSnackbar(
                            message = categoryAddFailureMessage(context, result),
                            withDismissAction = true
                    )
                }
            }
        }
    }

    Scaffold(
            containerColor = backgroundScrim,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    stringResource(R.string.category_settings_title),
                                    color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            FokusIconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back),
                                        tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                )
                )
            }
    ) { innerPadding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(innerPadding)
                                .navigationBarsPadding()
        ) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.category_new_label)) },
                        isError = inlineFieldFailure != null,
                        supportingText =
                                inlineFieldFailure?.let { failure ->
                                    { Text(categoryAddFailureMessage(context, failure)) }
                                },
                        modifier = Modifier.weight(1f)
                )
                FokusTextButton(
                        enabled = canAddCategory,
                        onClick = { viewModel.addCategoryDefinition(newCategory) }
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }

            ReorderableCategoryList(
                    categories = localCategories,
                    counts = appCounts,
                    showDrawerCategoryIcons = uiState.drawerSidebarCategories,
                    categoryDrawerIconOverrides = uiState.categoryDrawerIconOverrides,
                    onOpenCategoryIconPicker = { categoryIconPickerFor = it },
                    onResetCategoryDrawerIcon = { viewModel.clearCategoryDrawerIcon(it) },
                    onReorder = { from, to ->
                        val reordered = localCategories.toMutableList()
                        val item = reordered.removeAt(from)
                        reordered.add(to, item)
                        localCategories = reordered
                    },
                    onReorderFinished = { viewModel.reorderCategories(it) },
                    onEditCategoryApps = onEditCategoryApps,
                    onDelete = { viewModel.deleteCategory(it) }
            )
        }
    }

    val pickerCategory = categoryIconPickerFor
    if (pickerCategory != null) {
        CategoryIconPickerDialog(
                category = pickerCategory,
                iconOverrides = uiState.categoryDrawerIconOverrides,
                onSelect = { name ->
                    viewModel.setCategoryDrawerIcon(pickerCategory, name)
                },
                onDismiss = { }
        )
    }
}

private fun categoryAddFailureMessage(
        context: Context,
        failure: AddCategoryResult.Failure
): String {
    return when (failure) {
        AddCategoryResult.Failure.Blank ->
            context.getString(R.string.category_add_error_blank)
        AddCategoryResult.Failure.ReservedAllApps ->
            context.getString(R.string.category_add_error_reserved_all_apps)
        AddCategoryResult.Failure.ReservedUncategorized ->
            context.getString(R.string.category_add_error_reserved_uncategorized)
        AddCategoryResult.Failure.ReservedPrivate ->
            context.getString(R.string.category_add_error_reserved_private)
        AddCategoryResult.Failure.ReservedWork ->
            context.getString(R.string.category_add_error_reserved_work)
        is AddCategoryResult.Failure.Duplicate ->
            context.getString(
                    R.string.category_add_error_duplicate,
                    categoryChipDisplayLabel(context, failure.canonicalName)
            )
    }
}

@Composable
private fun ReorderableCategoryList(
        categories: List<String>,
        counts: Map<String, Int>,
        showDrawerCategoryIcons: Boolean,
        categoryDrawerIconOverrides: Map<String, String>,
        onOpenCategoryIconPicker: (String) -> Unit,
        onResetCategoryDrawerIcon: (String) -> Unit,
        onReorder: (Int, Int) -> Unit,
        onReorderFinished: (List<String>) -> Unit,
        onEditCategoryApps: (String) -> Unit,
        onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val currentCategories by rememberUpdatedState(categories)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnReorderFinished by rememberUpdatedState(onReorderFinished)
    val resetDragState = {
        if (draggedIndex != -1) {
            currentOnReorderFinished(currentCategories)
        }
        draggedIndex = -1
        dragOffset = 0f
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(count = categories.size, key = { categories[it] }) { index ->
            val category = categories[index]
            val count = counts[category] ?: 0
            val currentIndex by rememberUpdatedState(index)
            val offset = if (index == draggedIndex) dragOffset.coerceIn(-itemHeightPx, itemHeightPx) else 0f
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .graphicsLayer { translationY = offset }
                                    .clickableWithSystemSound { onEditCategoryApps(category) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = stringResource(R.string.cd_drag_to_reorder),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                                Modifier.size(24.dp)
                                        .pointerInput(category, categories.size) {
                                            detectVerticalDragGestures(
                                                    onDragStart = {
                                                        draggedIndex = currentIndex
                                                        dragOffset = 0f
                                                    },
                                                    onVerticalDrag = { change, amount ->
                                                        change.consume()
                                                        if (draggedIndex in currentCategories.indices) {
                                                            dragOffset += amount
                                                            while (dragOffset >= itemHeightPx && draggedIndex < currentCategories.size - 1) {
                                                                val from = draggedIndex
                                                                val to = draggedIndex + 1
                                                                currentOnReorder(from, to)
                                                                draggedIndex = to
                                                                dragOffset -= itemHeightPx
                                                            }
                                                            while (dragOffset <= -itemHeightPx && draggedIndex > 0) {
                                                                val from = draggedIndex
                                                                val to = draggedIndex - 1
                                                                currentOnReorder(from, to)
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
                if (showDrawerCategoryIcons) {
                    val railIconName =
                            resolvedCategoryDrawerIconName(
                                    context,
                                    category,
                                    categoryDrawerIconOverrides
                            )
                    FokusIconButton(
                            onClick = { onOpenCategoryIconPicker(category) },
                            modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                                imageVector = MinimalIcons.iconFor(railIconName),
                                contentDescription = stringResource(R.string.category_icon_picker_title),
                                tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    FokusIconButton(
                            onClick = { onResetCategoryDrawerIcon(category) },
                            modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription =
                                        stringResource(R.string.category_action_reset_icon),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = categoryChipDisplayLabel(context, category),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                            text = pluralStringResource(R.plurals.category_app_count, count, count),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                    )
                }
                FokusTextButton(onClick = { onEditCategoryApps(category) }) {
                    Text(stringResource(R.string.category_edit_apps))
                }
                FokusIconButton(onClick = { onDelete(category) }) {
                    Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_category),
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
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val isUncategorizedBucket =
            category.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(isUncategorizedBucket) {
        if (isUncategorizedBucket) onNavigateBack()
    }
    val checkedPackages = remember(uiState.allApps, category) {
        uiState.allApps
                .filter { app -> app.category.equals(category, ignoreCase = true) }
                .map { it.packageName }
                .toSet()
    }
    val checkedApps = remember(uiState.allApps, checkedPackages) {
        uiState.allApps.filter { it.packageName in checkedPackages }
    }
    val checkedSections = remember(checkedApps, context) {
        groupAppsIntoProfileSections(context, checkedApps, ::sortAppsAlphabeticallyByProfileSection)
    }
    val uncheckedApps = remember(uiState.allApps, checkedPackages, searchQuery) {
        uiState.allApps.filter { it.packageName !in checkedPackages }
                .let { apps ->
                    if (searchQuery.isBlank()) apps
                    else apps.filter { it.label.containsNormalizedSearch(searchQuery) }
                }
    }
    val uncheckedSections = remember(uncheckedApps, context) {
        groupAppsIntoProfileSections(context, uncheckedApps, ::sortAppsAlphabeticallyByProfileSection)
    }

    val listState = rememberLazyListState()
    var didSnapListTop by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.allApps.isNotEmpty()) {
        if (didSnapListTop || uiState.allApps.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(0, 0)
        didSnapListTop = true
    }

    BackHandler { onNavigateBack() }

    if (isUncategorizedBucket) {
        Box(
                modifier = Modifier.fillMaxSize().background(backgroundScrim)
        )
    } else {
        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundScrim)
                        .navigationBarsPadding()
        ) {
        TopAppBar(
                title = {
                    Text(
                            categoryChipDisplayLabel(context, category),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    FokusIconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    FokusIconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.action_done),
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
        )
        OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (checkedApps.isNotEmpty()) {
                item {
                    Text(
                            text = stringResource(R.string.category_apps_screen_section_in_category),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            profileGroupedAppItems(
                    sections = checkedSections,
                    keyPrefix = "cat_checked",
                    horizontalPadding = 16.dp,
            ) { app ->
                CategoryAppRow(
                        label = app.label,
                        checked = true,
                        secondary = categoryChipDisplayLabel(context, category),
                        onToggle = { viewModel.setAppCategory(app.packageName, "") }
                )
            }
            item {
                Text(
                        text = stringResource(R.string.edit_home_section_all_apps),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            profileGroupedAppItems(
                    sections = uncheckedSections,
                    keyPrefix = "cat_unchecked",
                    horizontalPadding = 16.dp,
            ) { app ->
                val currentCategory = app.category
                CategoryAppRow(
                        label = app.label,
                        checked = false,
                        secondary =
                                currentCategory.ifBlank {
                                    stringResource(R.string.category_no_category)
                                }.let { categoryChipDisplayLabel(context, it) },
                        onToggle = { viewModel.setAppCategory(app.packageName, category) }
                )
            }
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
    val onCheckboxChange = rememberBooleanChangeWithSystemSound { onToggle() }
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .height(56.dp)
                            .clickableWithSystemSound(onClick = onToggle)
                            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(checked = checked, onCheckedChange = onCheckboxChange)
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
            uiState.allApps
                    .mapNotNull { app -> app.category.takeIf { it.isNotBlank() } }
                    .toSet()
    val extras = (dynamic - orderedDefined.toSet()).toList().sorted()
    return orderedDefined + extras
}

private fun buildCategoryCounts(uiState: SettingsUiState): Map<String, Int> {
    return uiState.allApps
            .map { app -> app.category.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
}
