package com.lu4p.fokuslauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.drawerOpenCountKey
import com.lu4p.fokuslauncher.ui.drawer.DrawerProfileSectionUi
import com.lu4p.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedAppItems
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForApp
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForFavorite
import com.lu4p.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHomeAppsScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val editFavorites by viewModel.editFavorites.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val checkedKeys = remember(editFavorites) {
        editFavorites.map { drawerOpenCountKey(it.packageName, it.profileKey) }.toSet()
    }
    val uncheckedApps = remember(allApps, checkedKeys, searchQuery) {
        allApps.filter { drawerOpenCountKey(it.packageName, it.userHandle) !in checkedKeys }
            .let { list ->
                if (searchQuery.isBlank()) list
                else list.filter { it.label.containsNormalizedSearch(searchQuery) }
            }
    }
    val uncheckedSections = remember(uncheckedApps, context) {
        groupAppsIntoProfileSections(context, uncheckedApps, ::sortAppsAlphabeticallyByProfileSection)
    }

    val listState = rememberLazyListState()
    var didSnapListTop by remember { mutableStateOf(false) }
    LaunchedEffect(allApps.isNotEmpty()) {
        if (didSnapListTop || allApps.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(0, 0)
        didSnapListTop = true
    }

    BackHandler {
        viewModel.saveEditedFavorites()
        onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundScrim)
    ) {
        TopAppBar(
            title = {
                Text(stringResource(R.string.edit_home_title), color = MaterialTheme.colorScheme.onBackground)
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.saveEditedFavorites()
                    onNavigateBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    viewModel.saveEditedFavorites()
                    onNavigateBack()
                }) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        ReorderableEditHomeAppsList(
            listState = listState,
            editFavorites = editFavorites,
            uncheckedSections = uncheckedSections,
            allApps = allApps,
            onToggle = { viewModel.toggleAppOnHomeScreen(it) },
            onReorder = { from, to -> viewModel.reorderFavorite(from, to) }
        )
    }
}

@Composable
private fun ReorderableEditHomeAppsList(
    listState: LazyListState,
    editFavorites: List<FavoriteApp>,
    uncheckedSections: List<DrawerProfileSectionUi>,
    allApps: List<AppInfo>,
    onToggle: (AppInfo) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val resetDragState = {
        draggedIndex = -1
        dragOffset = 0f
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (editFavorites.isNotEmpty()) {
            item(key = "header_checked") {
                Text(
                    text = stringResource(R.string.edit_home_section_on_home),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(
            count = editFavorites.size,
            key = {
                val fav = editFavorites[it]
                "checked_${drawerOpenCountKey(fav.packageName, fav.profileKey)}"
            }
        ) { index ->
            val fav = editFavorites[index]
            val matchingApp =
                remember(fav.packageName, fav.profileKey, allApps) {
                    allApps.find {
                        it.packageName == fav.packageName &&
                            appProfileKey(it.userHandle) == fav.profileKey
                    }
                }
            val profileBadge =
                remember(fav, matchingApp, context) {
                    profileOriginLabelForFavorite(context, fav, matchingApp)
                }
            val offset = if (index == draggedIndex) {
                dragOffset.coerceIn(-itemHeightPx, itemHeightPx)
            } else {
                0f
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .graphicsLayer { translationY = offset }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.cd_drag_to_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(fav.packageName, fav.profileKey, editFavorites.size) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffset = 0f
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    if (draggedIndex in editFavorites.indices) {
                                        dragOffset += amount
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
                        allApps.find {
                            it.packageName == fav.packageName &&
                                    appProfileKey(it.userHandle) == fav.profileKey
                        }?.let { onToggle(it) }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fav.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (profileBadge != null) {
                        Text(
                            text = profileBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        item(key = "header_unchecked") {
            Text(
                text = stringResource(R.string.edit_home_section_all_apps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        profileGroupedAppItems(
            sections = uncheckedSections,
            keyPrefix = "unchecked",
            horizontalPadding = 16.dp,
        ) { app ->
            val profileBadge =
                remember(app.packageName, app.componentName, app.userHandle, context) {
                    profileOriginLabelForApp(context, app)
                }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggle(app) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (profileBadge != null) {
                        Text(
                            text = profileBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        item(key = "edit_home_list_bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
