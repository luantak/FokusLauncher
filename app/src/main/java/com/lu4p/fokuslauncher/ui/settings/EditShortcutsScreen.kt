package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.stableSelectionKey
import com.lu4p.fokuslauncher.ui.components.EditorScreenScaffold
import com.lu4p.fokuslauncher.ui.components.MinimalIconPickerDialog
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.drawer.DrawerProfileShortcutSectionUi
import com.lu4p.fokuslauncher.ui.drawer.groupShortcutActionsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedShortcutItems
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForHomeShortcut
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.settings.components.EditorDragHandleReorderIcon
import com.lu4p.fokuslauncher.ui.settings.components.EditorSectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.EditorStandardCheckboxGutter
import com.lu4p.fokuslauncher.ui.settings.components.EditorUncheckedLeadingSpacers
import com.lu4p.fokuslauncher.ui.settings.components.ProfileBadgeSubtitle
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberVerticalSlotReorderState
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
    val iconPickerForIndex = remember { mutableStateOf<Int?>(null) }

    val saveAndBack: () -> Unit = {
        viewModel.saveEditedRightShortcuts()
        onNavigateBack()
    }

    EditorScreenScaffold(
            titleText = stringResource(R.string.edit_shortcuts_title),
            searchPlaceholderResId = R.string.search_apps_and_actions,
            backgroundScrim = backgroundScrim,
            listReadyToScroll = allActions.isNotEmpty(),
            onNavigateBack = saveAndBack,
            onDone = saveAndBack,
    ) { searchQuery, listState ->
        val selectedIds =
                remember(editShortcuts) { editShortcuts.map { it.stableSelectionKey() }.toSet() }
        val uncheckedActions =
                remember(allActions, selectedIds, searchQuery) {
                    allActions
                            .filter { it.id !in selectedIds }
                            .let { list ->
                                if (searchQuery.isBlank()) list
                                else list.filter { it.displayLabel.containsNormalizedSearch(searchQuery) }
                            }
                }
        val uncheckedShortcutSections =
                remember(uncheckedActions, allApps, context) {
                    groupShortcutActionsIntoProfileSections(context, uncheckedActions, allApps)
                }

        ReorderableShortcutList(
                listState = listState,
                editShortcuts = editShortcuts,
                allApps = allApps,
                uncheckedShortcutSections = uncheckedShortcutSections,
                onToggleChecked = { shortcut ->
                    viewModel.toggleRightShortcut(
                            AppShortcutAction(
                                    appLabel =
                                            viewModel.formatShortcutTarget(
                                                    shortcut.target,
                                                    shortcut.profileKey
                                            ),
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
    val reorderState = rememberVerticalSlotReorderState()
    val openAppLabel = stringResource(R.string.shortcut_open_app)
    val openDialerLabel = stringResource(R.string.shortcut_open_dialer)
    val context = LocalContext.current

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (editShortcuts.isNotEmpty()) {
            item(key = "header_checked_shortcuts") {
                EditorSectionHeader(R.string.edit_shortcuts_section_selected)
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
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .graphicsLayer { translationY = reorderState.translationYForIndex(index) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                EditorDragHandleReorderIcon(
                        reorderState = reorderState,
                        index = index,
                        lastIndex = editShortcuts.lastIndex,
                        onReorder = onReorder,
                        onReset = { reorderState.reset() },
                        shortcut.target,
                        shortcut.profileKey,
                        editShortcuts.size,
                )
                EditorStandardCheckboxGutter(
                        checked = true,
                        onCheckedChange =
                                rememberBooleanChangeWithSystemSound { _ -> onToggleChecked(shortcut) },
                ) {
                    Icon(
                            imageVector = MinimalIcons.iconFor(shortcut.iconName),
                            contentDescription = stringResource(R.string.cd_change_icon),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier =
                                    Modifier.size(40.dp)
                                            .clickableWithSystemSound { onOpenIconPicker(index) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = formatCheckedLabel(shortcut),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        ProfileBadgeSubtitle(profileBadge)
                    }
                }
            }
        }

        item(key = "header_unchecked_shortcuts") {
            EditorSectionHeader(R.string.edit_shortcuts_section_all_actions)
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
                EditorUncheckedLeadingSpacers()
                EditorStandardCheckboxGutter(
                        checked = false,
                        onCheckedChange =
                                rememberBooleanChangeWithSystemSound { _ -> onToggleUnchecked(action) },
                ) {
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
}
