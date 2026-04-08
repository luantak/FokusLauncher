package com.lu4p.fokuslauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.util.applyVerticalSlotReorder
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
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.stableSelectionKey
import com.lu4p.fokuslauncher.ui.components.MinimalIconPickerDialog
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.drawer.DrawerProfileShortcutSectionUi
import com.lu4p.fokuslauncher.ui.drawer.groupShortcutActionsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedShortcutItems
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForHomeShortcut
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShortcutsScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val context = LocalContext.current
    val editShortcuts by viewModel.editRightShortcuts.collectAsStateWithLifecycle()
    val allActions by viewModel.allShortcutActions.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val iconPickerForIndex = remember { mutableStateOf<Int?>(null) }

    val selectedIds = remember(editShortcuts) {
        editShortcuts.map { it.stableSelectionKey() }.toSet()
    }
    val uncheckedActions = remember(allActions, selectedIds, searchQuery) {
        allActions
            .filter { it.id !in selectedIds }
            .let { list ->
                if (searchQuery.isBlank()) list
                else list.filter { it.displayLabel.containsNormalizedSearch(searchQuery) }
            }
    }
    val uncheckedShortcutSections = remember(uncheckedActions, allApps, context) {
        groupShortcutActionsIntoProfileSections(context, uncheckedActions, allApps)
    }

    val listState = rememberLazyListState()
    var didSnapListTop by remember { mutableStateOf(false) }
    LaunchedEffect(allActions.isNotEmpty()) {
        if (didSnapListTop || allActions.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(0, 0)
        didSnapListTop = true
    }

    BackHandler {
        viewModel.saveEditedRightShortcuts()
        onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundScrim)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(stringResource(R.string.edit_shortcuts_title), color = MaterialTheme.colorScheme.onBackground)
            },
            navigationIcon = {
                FokusIconButton(onClick = {
                    viewModel.saveEditedRightShortcuts()
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
                FokusIconButton(onClick = {
                    viewModel.saveEditedRightShortcuts()
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
            placeholder = { Text(stringResource(R.string.search_apps_and_actions)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        ReorderableShortcutList(
            listState = listState,
            editShortcuts = editShortcuts,
            allApps = allApps,
            uncheckedShortcutSections = uncheckedShortcutSections,
            onToggleChecked = { shortcut ->
                viewModel.toggleRightShortcut(
                    AppShortcutAction(
                        appLabel = viewModel.formatShortcutTarget(shortcut.target, shortcut.profileKey),
                        actionLabel = AppShortcutAction.OPEN_APP_LABEL,
                        target = shortcut.target,
                        profileKey = shortcut.profileKey,
                    )
                )
            },
            onToggleUnchecked = { action -> viewModel.toggleRightShortcut(action) },
            onReorder = { from, to -> viewModel.reorderRightShortcut(from, to) },
            onOpenIconPicker = { index -> iconPickerForIndex.value = index },
            formatCheckedLabel = { shortcut ->
                viewModel.formatShortcutTarget(shortcut.target, shortcut.profileKey)
            }
        )
    }

    iconPickerForIndex.value?.let { pickerIndex ->
        MinimalIconPickerDialog(
                storedIconKey = editShortcuts.getOrNull(pickerIndex)?.iconName ?: "circle",
                title = {
                    Text(
                            stringResource(R.string.edit_shortcuts_choose_icon),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onSelect = { name ->
                    viewModel.updateShortcutIcon(pickerIndex, name)
                    iconPickerForIndex.value = null
                },
                onDismiss = { iconPickerForIndex.value = null }
        )
    }
}

@Composable
private fun ReorderableShortcutList(
    listState: LazyListState,
    editShortcuts: List<HomeShortcut>,
    allApps: List<AppInfo>,
    uncheckedShortcutSections: List<DrawerProfileShortcutSectionUi>,
    onToggleChecked: (HomeShortcut) -> Unit,
    onToggleUnchecked: (AppShortcutAction) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onOpenIconPicker: (Int) -> Unit,
    formatCheckedLabel: (HomeShortcut) -> String
) {
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val openAppLabel = stringResource(R.string.shortcut_open_app)
    val openDialerLabel = stringResource(R.string.shortcut_open_dialer)
    val context = LocalContext.current
    val resetDragState = {
        draggedIndex = -1
        dragOffset = 0f
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (editShortcuts.isNotEmpty()) {
            item(key = "header_checked_shortcuts") {
                Text(
                    text = stringResource(R.string.edit_shortcuts_section_selected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(
            count = editShortcuts.size,
            key = { "checked_shortcut_${editShortcuts[it].stableSelectionKey()}" }
        ) { index ->
            val shortcut = editShortcuts[index]
            val profileBadge =
                remember(shortcut, allApps, context) {
                    profileOriginLabelForHomeShortcut(context, shortcut, allApps)
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
                        .pointerInput(shortcut.target, shortcut.profileKey, editShortcuts.size) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    draggedIndex = index
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    if (draggedIndex in editShortcuts.indices) {
                                        dragOffset += amount
                                        val (newOff, newIdx) =
                                                applyVerticalSlotReorder(
                                                        itemHeightPx,
                                                        dragOffset,
                                                        draggedIndex,
                                                        editShortcuts.lastIndex,
                                                ) { from, to -> onReorder(from, to) }
                                        dragOffset = newOff
                                        draggedIndex = newIdx
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
                    onCheckedChange =
                            rememberBooleanChangeWithSystemSound { _ -> onToggleChecked(shortcut) }
                )
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = MinimalIcons.iconFor(shortcut.iconName),
                    contentDescription = stringResource(R.string.cd_change_icon),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(40.dp)
                        .clickableWithSystemSound { onOpenIconPicker(index) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatCheckedLabel(shortcut),
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

        item(key = "header_unchecked_shortcuts") {
            Text(
                text = stringResource(R.string.edit_shortcuts_section_all_actions),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        profileGroupedShortcutItems(
            sections = uncheckedShortcutSections,
            keyPrefix = "unchecked_shortcut",
            horizontalPadding = 16.dp,
        ) { action ->
            val primaryText =
                when {
                    action.target is ShortcutTarget.PhoneDial -> openDialerLabel
                    action.actionLabel == AppShortcutAction.OPEN_APP_LABEL -> openAppLabel
                    else -> action.actionLabel
                }
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = false,
                    onCheckedChange =
                            rememberBooleanChangeWithSystemSound { _ -> onToggleUnchecked(action) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

