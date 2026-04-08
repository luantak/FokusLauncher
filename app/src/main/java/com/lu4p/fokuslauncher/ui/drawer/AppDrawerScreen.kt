package com.lu4p.fokuslauncher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.appListStableKey
import com.lu4p.fokuslauncher.utils.DotSearchSyntax
import com.lu4p.fokuslauncher.ui.components.CategoryChips
import com.lu4p.fokuslauncher.ui.components.CategoryIconPickerDialog
import com.lu4p.fokuslauncher.ui.components.DrawerCategorySidebar
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.components.SearchBar
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound
import com.lu4p.fokuslauncher.ui.util.resolvedCategoryDrawerIconName
import java.util.Locale

private fun deepCopyProfileSections(
        sections: List<DrawerProfileSectionUi>
): List<DrawerProfileSectionUi> =
        sections.map { DrawerProfileSectionUi(it.id, it.title, it.apps.toList()) }

/** Applies one adjacent move in [sectionId]'s app list (same semantics as VM reorder). */
private fun swapAdjacentInProfileSection(
        sections: List<DrawerProfileSectionUi>,
        sectionId: String,
        from: Int,
        to: Int
): List<DrawerProfileSectionUi> {
    return sections.map { sec ->
        if (sec.id != sectionId) sec
        else {
            val apps = sec.apps.toMutableList()
            val item = apps.removeAt(from)
            apps.add(to, item)
            sec.copy(apps = apps)
        }
    }
}

private fun applyOptimisticProfileSwap(
        optimistic: List<DrawerProfileSectionUi>?,
        fallback: List<DrawerProfileSectionUi>,
        sectionId: String,
        from: Int,
        to: Int
): List<DrawerProfileSectionUi> {
    val base = optimistic ?: deepCopyProfileSections(fallback)
    return swapAdjacentInProfileSection(base, sectionId, from, to)
}

private fun applyOptimisticPrivateSwap(
        optimistic: List<AppInfo>?,
        fallback: List<AppInfo>,
        from: Int,
        to: Int
): List<AppInfo> {
    val base = optimistic ?: fallback.toList()
    val apps = base.toMutableList()
    val item = apps.removeAt(from)
    apps.add(to, item)
    return apps
}

/** Horizontal swipe distance (px) to move to the next/previous category in the app list. */
private const val DRAWER_CATEGORY_SWIPE_THRESHOLD_PX = 120f
private val DRAWER_MIN_TOP_PADDING = 48.dp
private val DRAWER_TOP_INSET_BUFFER = 16.dp

@Composable
private fun DrawerOverflowMenu(
        uiState: AppDrawerUiState,
        onMenuToggle: () -> Unit,
        onMenuDismiss: () -> Unit,
        onPrivateSpaceToggle: () -> Unit,
        onSettingsClick: () -> Unit,
        showReorderMenuItem: Boolean,
        onToggleReorderApps: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FokusIconButton(onClick = onMenuToggle, modifier = Modifier.testTag("settings_button")) {
            Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_menu),
                    tint = MaterialTheme.colorScheme.onBackground
            )
        }
        DropdownMenu(
                expanded = uiState.showMenu,
                onDismissRequest = onMenuDismiss,
                modifier =
                        Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                        )
        ) {
            if (uiState.isPrivateSpaceSupported) {
                DropdownMenuItem(
                        text = {
                            Text(
                                    text =
                                            if (uiState.isPrivateSpaceUnlocked)
                                                    stringResource(R.string.drawer_private_space_lock)
                                            else stringResource(R.string.drawer_private_space_unlock)
                            )
                        },
                        onClick = rememberClickWithSystemSound { onPrivateSpaceToggle() },
                        leadingIcon = {
                            Icon(
                                    imageVector =
                                            if (uiState.isPrivateSpaceUnlocked)
                                                    Icons.Default.LockOpen
                                            else Icons.Default.Lock,
                                    contentDescription = null
                            )
                        },
                        modifier = Modifier.testTag("menu_private_space")
                )
            }
            if (showReorderMenuItem) {
                DropdownMenuItem(
                        text = {
                            Text(
                                    stringResource(
                                            if (uiState.drawerReorderSessionActive) {
                                                R.string.drawer_reorder_done
                                            } else {
                                                R.string.drawer_reorder
                                            }
                                    )
                            )
                        },
                        onClick = rememberClickWithSystemSound { onToggleReorderApps() },
                        leadingIcon = {
                            Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = null
                            )
                        },
                        modifier = Modifier.testTag("menu_reorder_apps")
                )
            }
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.drawer_menu_launcher_settings)) },
                    onClick = rememberClickWithSystemSound { onSettingsClick() },
                    leadingIcon = {
                        Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("menu_settings")
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppListColumn(
        listState: LazyListState,
        modifier: Modifier,
        uiState: AppDrawerUiState,
        showProfileSections: Boolean,
        anyProfileAppsVisible: Boolean,
        focusManager: FocusManager,
        onAppClick: (LaunchTarget) -> Unit,
        onAppLongPress: (AppInfo) -> Unit,
        allowCustomDragReorder: Boolean,
        onReorderProfileSection: (sectionId: String, fromIndex: Int, toIndex: Int) -> Unit,
        onReorderPrivateApps: (fromIndex: Int, toIndex: Int) -> Unit
) {
    // Match CategorySettingsScreen.ReorderableCategoryList: 56dp steps, ±slot-coerced
    // offset, adjacent swaps in while-loops during drag (well-tested pattern).
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    var optimisticProfileSections by remember { mutableStateOf<List<DrawerProfileSectionUi>?>(null) }
    var optimisticPrivateApps by remember { mutableStateOf<List<AppInfo>?>(null) }

    LaunchedEffect(allowCustomDragReorder) {
        if (!allowCustomDragReorder) {
            optimisticProfileSections = null
            optimisticPrivateApps = null
        } else {
            optimisticProfileSections = deepCopyProfileSections(uiState.filteredProfileSections)
            optimisticPrivateApps = uiState.filteredPrivateSpaceApps.toList()
        }
    }

    val displayProfileSections = optimisticProfileSections ?: uiState.filteredProfileSections
    val displayPrivateApps = optimisticPrivateApps ?: uiState.filteredPrivateSpaceApps
    val latestDisplayProfileSections by rememberUpdatedState(displayProfileSections)
    val latestDisplayPrivateApps by rememberUpdatedState(displayPrivateApps)
    val currentOnReorderProfile by rememberUpdatedState(onReorderProfileSection)
    val currentOnReorderPrivate by rememberUpdatedState(onReorderPrivateApps)
    var draggedProfileSectionId by remember(allowCustomDragReorder) {
        mutableStateOf<String?>(null)
    }
    var draggedProfileIndex by remember(allowCustomDragReorder) { mutableIntStateOf(-1) }
    var profileDragOffset by remember(allowCustomDragReorder) { mutableFloatStateOf(0f) }
    var draggedPrivateIndex by remember(allowCustomDragReorder) { mutableIntStateOf(-1) }
    var privateDragOffset by remember(allowCustomDragReorder) { mutableFloatStateOf(0f) }
    val resetProfileDrag = {
        draggedProfileSectionId = null
        draggedProfileIndex = -1
        profileDragOffset = 0f
    }
    val resetPrivateDrag = {
        draggedPrivateIndex = -1
        privateDragOffset = 0f
    }

    LazyColumn(state = listState, modifier = modifier) {
        if (showProfileSections) {
            var hasEmittedProfileListContent = false
            for (section in displayProfileSections) {
                if (section.apps.isEmpty()) continue
                val showSectionLabel = section.id != "owner"
                if (showSectionLabel) {
                    if (hasEmittedProfileListContent) {
                        item(key = "div_profile_${section.id}") {
                            HorizontalDivider(
                                    modifier =
                                            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    item(key = "hdr_profile_${section.id}") {
                        Text(
                                text = section.title.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
                hasEmittedProfileListContent = true
                items(
                        count = section.apps.size,
                        key = { index ->
                            "${section.id}_${appListStableKey(section.apps[index])}"
                        }
                ) { index ->
                    val app = section.apps[index]
                    val currentIndex by rememberUpdatedState(index)
                    val isDraggedRow =
                            allowCustomDragReorder &&
                                    section.id == draggedProfileSectionId &&
                                    index == draggedProfileIndex
                    val offsetY =
                            if (isDraggedRow) {
                                profileDragOffset.coerceIn(-itemHeightPx, itemHeightPx)
                            } else {
                                0f
                            }
                    Row(
                            modifier =
                                    Modifier.then(
                                            if (allowCustomDragReorder && !isDraggedRow) {
                                                Modifier.animateItem(
                                                        fadeInSpec = null,
                                                        fadeOutSpec = null,
                                                        placementSpec =
                                                                tween(
                                                                        180,
                                                                        easing =
                                                                                FastOutSlowInEasing,
                                                                ),
                                                )
                                            } else {
                                                Modifier
                                            }
                                    )
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                            .graphicsLayer { translationY = offsetY },
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allowCustomDragReorder) {
                            Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription =
                                            stringResource(R.string.cd_drag_to_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                            Modifier.padding(start = 8.dp)
                                                    .size(24.dp)
                                                    .pointerInput(
                                                            section.id,
                                                            appListStableKey(app),
                                                            allowCustomDragReorder
                                                    ) {
                                                        detectVerticalDragGestures(
                                                                onDragStart = {
                                                                    draggedProfileSectionId = section.id
                                                                    draggedProfileIndex = currentIndex
                                                                    profileDragOffset = 0f
                                                                },
                                                                onVerticalDrag = { change, amount ->
                                                                    change.consume()
                                                                    val sectionApps =
                                                                            latestDisplayProfileSections
                                                                                    .find {
                                                                                        it.id == section.id
                                                                                    }
                                                                                    ?.apps
                                                                    if (draggedProfileSectionId == section.id &&
                                                                                    sectionApps != null &&
                                                                                    draggedProfileIndex in
                                                                                            sectionApps.indices
                                                                    ) {
                                                                        profileDragOffset += amount
                                                                        while (profileDragOffset >= itemHeightPx &&
                                                                                        draggedProfileIndex <
                                                                                                sectionApps.lastIndex
                                                                        ) {
                                                                            val from = draggedProfileIndex
                                                                            val to = draggedProfileIndex + 1
                                                                            optimisticProfileSections =
                                                                                    applyOptimisticProfileSwap(
                                                                                            optimisticProfileSections,
                                                                                            latestDisplayProfileSections,
                                                                                            section.id,
                                                                                            from,
                                                                                            to
                                                                                    )
                                                                            currentOnReorderProfile(
                                                                                    section.id,
                                                                                    from,
                                                                                    to
                                                                            )
                                                                            draggedProfileIndex = to
                                                                            profileDragOffset -= itemHeightPx
                                                                        }
                                                                        while (profileDragOffset <= -itemHeightPx &&
                                                                                        draggedProfileIndex > 0
                                                                        ) {
                                                                            val from = draggedProfileIndex
                                                                            val to = draggedProfileIndex - 1
                                                                            optimisticProfileSections =
                                                                                    applyOptimisticProfileSwap(
                                                                                            optimisticProfileSections,
                                                                                            latestDisplayProfileSections,
                                                                                            section.id,
                                                                                            from,
                                                                                            to
                                                                                    )
                                                                            currentOnReorderProfile(
                                                                                    section.id,
                                                                                    from,
                                                                                    to
                                                                            )
                                                                            draggedProfileIndex = to
                                                                            profileDragOffset += itemHeightPx
                                                                        }
                                                                    }
                                                                },
                                                                onDragEnd = { resetProfileDrag() },
                                                                onDragCancel = { resetProfileDrag() }
                                                        )
                                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        AppListItem(
                                app = app,
                                onClick = {
                                    if (allowCustomDragReorder) return@AppListItem
                                    focusManager.clearFocus(force = true)
                                    val cn = app.componentName
                                    val uh = app.userHandle
                                    if (cn != null && uh != null) {
                                        onAppClick(
                                                LaunchTarget.PrivateApp(
                                                        packageName = app.packageName,
                                                        componentName = cn,
                                                        userHandle = uh
                                                )
                                        )
                                    } else {
                                        onAppClick(LaunchTarget.MainApp(app.packageName))
                                    }
                                },
                                onLongClick = {
                                    if (!allowCustomDragReorder) onAppLongPress(app)
                                },
                                modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        if (uiState.isPrivateSpaceUnlocked && displayPrivateApps.isNotEmpty()) {
            if (showProfileSections && anyProfileAppsVisible) {
                item {
                    HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            item {
                Text(
                        text =
                                stringResource(R.string.drawer_section_private_space)
                                        .uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            items(
                    count = displayPrivateApps.size,
                    key = { index ->
                        "private_${appListStableKey(displayPrivateApps[index])}"
                    }
            ) { index ->
                val app = displayPrivateApps[index]
                val currentIndex by rememberUpdatedState(index)
                val isDraggedRow =
                        allowCustomDragReorder && index == draggedPrivateIndex
                val offsetY =
                        if (isDraggedRow) {
                            privateDragOffset.coerceIn(-itemHeightPx, itemHeightPx)
                        } else {
                            0f
                        }
                Row(
                        modifier =
                                Modifier.then(
                                        if (allowCustomDragReorder && !isDraggedRow) {
                                            Modifier.animateItem(
                                                    fadeInSpec = null,
                                                    fadeOutSpec = null,
                                                    placementSpec =
                                                            tween(
                                                                    180,
                                                                    easing = FastOutSlowInEasing,
                                                            ),
                                            )
                                        } else {
                                            Modifier
                                        }
                                )
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .graphicsLayer { translationY = offsetY },
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allowCustomDragReorder) {
                        Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.cd_drag_to_reorder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                        Modifier.padding(start = 8.dp)
                                                .size(24.dp)
                                                .pointerInput(
                                                        appListStableKey(app),
                                                        latestDisplayPrivateApps.size,
                                                        allowCustomDragReorder
                                                ) {
                                                    detectVerticalDragGestures(
                                                            onDragStart = {
                                                                draggedPrivateIndex = currentIndex
                                                                privateDragOffset = 0f
                                                            },
                                                            onVerticalDrag = { change, amount ->
                                                                change.consume()
                                                                if (draggedPrivateIndex in latestDisplayPrivateApps.indices) {
                                                                    privateDragOffset += amount
                                                                    while (privateDragOffset >= itemHeightPx &&
                                                                                    draggedPrivateIndex <
                                                                                            latestDisplayPrivateApps
                                                                                                    .lastIndex
                                                                    ) {
                                                                        val from = draggedPrivateIndex
                                                                        val to = draggedPrivateIndex + 1
                                                                        optimisticPrivateApps =
                                                                                applyOptimisticPrivateSwap(
                                                                                        optimisticPrivateApps,
                                                                                        latestDisplayPrivateApps,
                                                                                        from,
                                                                                        to
                                                                                )
                                                                        currentOnReorderPrivate(from, to)
                                                                        draggedPrivateIndex = to
                                                                        privateDragOffset -= itemHeightPx
                                                                    }
                                                                    while (privateDragOffset <= -itemHeightPx &&
                                                                                    draggedPrivateIndex > 0
                                                                    ) {
                                                                        val from = draggedPrivateIndex
                                                                        val to = draggedPrivateIndex - 1
                                                                        optimisticPrivateApps =
                                                                                applyOptimisticPrivateSwap(
                                                                                        optimisticPrivateApps,
                                                                                        latestDisplayPrivateApps,
                                                                                        from,
                                                                                        to
                                                                                )
                                                                        currentOnReorderPrivate(from, to)
                                                                        draggedPrivateIndex = to
                                                                        privateDragOffset += itemHeightPx
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = { resetPrivateDrag() },
                                                            onDragCancel = { resetPrivateDrag() }
                                                    )
                                                }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    AppListItem(
                            app = app,
                            onClick = {
                                if (allowCustomDragReorder) return@AppListItem
                                val componentName = app.componentName
                                val userHandle = app.userHandle
                                if (componentName != null && userHandle != null) {
                                    focusManager.clearFocus(force = true)
                                    onAppClick(
                                            LaunchTarget.PrivateApp(
                                                    packageName = app.packageName,
                                                    componentName = componentName,
                                                    userHandle = userHandle
                                            )
                                    )
                                }
                            },
                            onLongClick = {
                                if (!allowCustomDragReorder) onAppLongPress(app)
                            },
                            modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppDrawerScreen(
        viewModel: AppDrawerViewModel = hiltViewModel(),
        onSettingsClick: () -> Unit = {},
        onEditCategoryApps: (String) -> Unit = {},
        onClose: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val onCloseUpdated by rememberUpdatedState(onClose)
    val closeAndReset: () -> Unit = {
        viewModel.resetSearchState()
        onCloseUpdated()
    }
    // Defer closing until after startActivity is processed so the drawer exit animation does not
    // run in the same frame as the launch handoff (smoother transition, avoids perceived "close
    // before open").
    val closeAndResetAfterLaunch: () -> Unit = {
        view.post {
            viewModel.resetSearchState()
            onCloseUpdated()
        }
    }

    // Close the drawer after an app is auto-launched from search
    LaunchedEffect(Unit) {
        viewModel.resetSearchStateIfNeeded()
        viewModel.events.collect { event ->
            when (event) {
                is DrawerEvent.AutoLaunch -> {
                    closeAndResetAfterLaunch()
                }
            }
        }
    }

    AppDrawerContent(
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearchImeAction = {
                if (viewModel.tryLaunchFirstSearchResult()) closeAndResetAfterLaunch()
            },
            onCategorySelected = viewModel::onCategorySelected,
            onCategoryLongPress = viewModel::onCategoryLongPress,
            onAppClick = { target ->
                if (viewModel.launchTarget(target)) {
                    closeAndResetAfterLaunch()
                }
            },
            onAppLongPress = viewModel::onAppLongPress,
            onMenuToggle = viewModel::toggleMenu,
            onMenuDismiss = viewModel::dismissMenu,
            onSettingsClick = {
                viewModel.dismissMenu()
                onSettingsClick()
            },
            onPrivateSpaceToggle = viewModel::togglePrivateSpace,
            onToggleDrawerReorderApps = viewModel::toggleDrawerReorderSession,
            onClose = closeAndReset,
            useSidebarCategoryDrawer = uiState.useSidebarCategoryDrawer,
            drawerCategorySidebarOnRight = uiState.drawerCategorySidebarOnRight,
            categoryDrawerIconOverrides = uiState.categoryDrawerIconOverrides,
            onReorderDrawerProfileSection = viewModel::reorderDrawerProfileSectionApps,
            onReorderPrivateDrawerApps = viewModel::reorderPrivateDrawerApps
    )

    // Action sheet on long-press
    uiState.selectedApp?.let { app ->
        AppActionSheet(
                app = app,
                onDismiss = viewModel::dismissActionSheet,
                onAddToHome = {
                    viewModel.addToHomeScreen(it)
                    closeAndReset()
                },
                onRename = { newName -> viewModel.renameApp(app.packageName, newName) },
                onHide = { viewModel.hideApp(it) },
                isOnHomeScreen = app.packageName in uiState.favoritePackageNames
        )
    }

    uiState.selectedCategoryForActions?.let { category ->
        CategoryActionSheet(
                category = category,
                categoryDrawerIconOverrides = uiState.categoryDrawerIconOverrides,
                onDismiss = viewModel::dismissCategoryActionSheet,
                onRename = { newName: String -> viewModel.renameCategory(category, newName) },
                onEditApps = {
                    viewModel.dismissCategoryActionSheet()
                    onEditCategoryApps(category)
                },
                onDelete = { viewModel.deleteCategory(category) },
                onSetCategoryIcon = { iconName ->
                    viewModel.setCategoryDrawerIcon(category, iconName)
                },
                onResetCategoryIcon = { viewModel.clearCategoryDrawerIcon(category) }
        )
    }
}

@Composable
fun AppDrawerContent(
        uiState: AppDrawerUiState,
        onSearchQueryChanged: (String) -> Unit,
        onSearchImeAction: () -> Unit = {},
        onCategorySelected: (String) -> Unit,
        onAppClick: (LaunchTarget) -> Unit,
        onSettingsClick: () -> Unit,
        modifier: Modifier = Modifier,
        useSidebarCategoryDrawer: Boolean = false,
        drawerCategorySidebarOnRight: Boolean = true,
        categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        onCategoryLongPress: (String) -> Unit = {},
        onAppLongPress: (AppInfo) -> Unit = {},
        onMenuToggle: () -> Unit = {},
        onMenuDismiss: () -> Unit = {},
        onPrivateSpaceToggle: () -> Unit = {},
        onToggleDrawerReorderApps: () -> Unit = {},
        onClose: () -> Unit = {},
        onReorderDrawerProfileSection: (sectionId: String, fromIndex: Int, toIndex: Int) -> Unit =
                { _, _, _ -> },
        onReorderPrivateDrawerApps: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
) {
    val drawerContext = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val latestCategories = rememberUpdatedState(uiState.categories)
    val latestSelectedCategory = rememberUpdatedState(uiState.selectedCategory)
    val latestOnCategorySelected = rememberUpdatedState(onCategorySelected)
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedCategory) { listState.scrollToItem(0) }

    LaunchedEffect(useSidebarCategoryDrawer) {
        if (!useSidebarCategoryDrawer) showSearch = false
    }

    val showProfileSections =
            !uiState.useSidebarCategoryDrawer ||
                    !uiState.selectedCategory.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ||
                    uiState.searchQuery.isNotBlank()
    val anyProfileAppsVisible =
            uiState.filteredProfileSections.any { it.apps.isNotEmpty() }
    val closeWithFocusReset: () -> Unit = {
        focusManager.clearFocus(force = true)
        onClose()
    }

    val searchFilterBlank =
            remember(uiState.searchQuery) {
                val trimmed = uiState.searchQuery.trimStart()
                val q =
                        if (DotSearchSyntax.isPossibleDotSearchPrefix(trimmed)) ""
                        else trimmed.trim()
                q.isBlank()
            }
    val showDrawerReorderMenuToggle =
            uiState.useSidebarCategoryDrawer &&
                    uiState.drawerAppSortMode == DrawerAppSortMode.CUSTOM
    val allowCustomDragReorder =
            useSidebarCategoryDrawer &&
                    uiState.drawerAppSortMode == DrawerAppSortMode.CUSTOM &&
                    uiState.drawerReorderSessionActive &&
                    searchFilterBlank

    BackHandler { closeWithFocusReset() }

    LaunchedEffect(useSidebarCategoryDrawer, showSearch) {
        val wantKeyboard =
                if (useSidebarCategoryDrawer) showSearch else true
        if (!wantKeyboard) return@LaunchedEffect
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(listState, keyboardController, focusManager) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                    listState.isScrollInProgress,
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset
            )
        }.collect { (scrolling, index, offset) ->
            if (scrolling) {
                val scrolledDown =
                        index > prevIndex ||
                                (index == prevIndex && offset > prevOffset)
                if (scrolledDown) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
            }
            prevIndex = index
            prevOffset = offset
        }
    }

    var overscrollY by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0 && !listState.canScrollBackward) {
                    overscrollY += available.y
                    if (overscrollY > 300f) {
                        overscrollY = 0f
                        closeWithFocusReset()
                        return available
                    }
                } else {
                    overscrollY = 0f
                }
                return Offset.Zero
            }
        }
    }
    val contentTopPadding =
            maxOf(
                    DRAWER_MIN_TOP_PADDING,
                    with(density) {
                        (ViewCompat.getRootWindowInsets(view)
                                        ?.getInsets(WindowInsetsCompat.Type.statusBars())
                                        ?.top ?: 0)
                                .toDp()
                    } +
                            DRAWER_TOP_INSET_BUFFER
            )

    val categorySwipeModifier =
            if (uiState.categories.size > 1) {
                Modifier.pointerInput(Unit) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                accumulated += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                val categories = latestCategories.value
                                if (categories.size <= 1) return@detectHorizontalDragGestures
                                val selected = latestSelectedCategory.value
                                val idx =
                                        categories.indexOfFirst {
                                            it.equals(selected, ignoreCase = true)
                                        }
                                if (idx < 0) return@detectHorizontalDragGestures
                                when {
                                    accumulated <= -DRAWER_CATEGORY_SWIPE_THRESHOLD_PX &&
                                            idx < categories.lastIndex ->
                                            latestOnCategorySelected.value(categories[idx + 1])
                                    accumulated >= DRAWER_CATEGORY_SWIPE_THRESHOLD_PX && idx > 0 ->
                                            latestOnCategorySelected.value(categories[idx - 1])
                                }
                            },
                            onDragCancel = { accumulated = 0f }
                    )
                }
            } else {
                Modifier
            }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(top = contentTopPadding)
                                .testTag("app_drawer_screen")
        ) {
            if (useSidebarCategoryDrawer && uiState.categories.size > 1) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val sidebar: @Composable () -> Unit = {
                        DrawerCategorySidebar(
                                categories = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = onCategorySelected,
                                onCategoryLongPress = onCategoryLongPress,
                                sidebarOnLeft = !drawerCategorySidebarOnRight,
                                categoryIconOverrides = categoryDrawerIconOverrides,
                                modifier = Modifier.weight(18f).fillMaxHeight()
                        )
                    }
                    val body: @Composable () -> Unit = {
                        Column(
                                modifier =
                                        Modifier.weight(82f)
                                                .fillMaxHeight()
                                                .nestedScroll(nestedScrollConnection)
                        ) {
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                        text =
                                                categoryChipDisplayLabel(
                                                        drawerContext,
                                                        uiState.selectedCategory
                                                ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                )
                                FokusIconButton(
                                        onClick = { showSearch = !showSearch },
                                        modifier = Modifier.testTag("drawer_search_icon")
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription =
                                                    stringResource(R.string.search_apps),
                                            tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                DrawerOverflowMenu(
                                        uiState = uiState,
                                        onMenuToggle = onMenuToggle,
                                        onMenuDismiss = onMenuDismiss,
                                        onPrivateSpaceToggle = onPrivateSpaceToggle,
                                        onSettingsClick = onSettingsClick,
                                        showReorderMenuItem = showDrawerReorderMenuToggle,
                                        onToggleReorderApps = onToggleDrawerReorderApps
                                )
                            }
                            if (showSearch) {
                                OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = onSearchQueryChanged,
                                        placeholder = {
                                            Text(stringResource(R.string.search_apps_hint))
                                        },
                                        singleLine = true,
                                        keyboardOptions =
                                                KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions =
                                                KeyboardActions(onSearch = { onSearchImeAction() }),
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(horizontal = 16.dp)
                                                        .focusRequester(focusRequester)
                                                        .testTag("search_bar")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            DrawerAppListColumn(
                                    listState = listState,
                                    modifier =
                                            Modifier.weight(1f)
                                                    .fillMaxWidth()
                                                    .then(categorySwipeModifier)
                                                    .testTag("app_list"),
                                    uiState = uiState,
                                    showProfileSections = showProfileSections,
                                    anyProfileAppsVisible = anyProfileAppsVisible,
                                    focusManager = focusManager,
                                    onAppClick = onAppClick,
                                    onAppLongPress = onAppLongPress,
                                    allowCustomDragReorder = allowCustomDragReorder,
                                    onReorderProfileSection = onReorderDrawerProfileSection,
                                    onReorderPrivateApps = onReorderPrivateDrawerApps
                            )
                        }
                    }
                    if (drawerCategorySidebarOnRight) {
                        body()
                        sidebar()
                    } else {
                        sidebar()
                        body()
                    }
                }
            } else {
                Column(
                        modifier =
                                Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged,
                                placeholder = stringResource(R.string.search_apps_hint),
                                focusRequester = focusRequester,
                                onImeAction = onSearchImeAction,
                                modifier =
                                        Modifier.weight(1f).testTag("search_bar")
                        )
                        DrawerOverflowMenu(
                                uiState = uiState,
                                onMenuToggle = onMenuToggle,
                                onMenuDismiss = onMenuDismiss,
                                onPrivateSpaceToggle = onPrivateSpaceToggle,
                                onSettingsClick = onSettingsClick,
                                showReorderMenuItem = showDrawerReorderMenuToggle,
                                onToggleReorderApps = onToggleDrawerReorderApps
                        )
                    }
                    if (uiState.categories.size > 1) {
                        CategoryChips(
                                categories = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = onCategorySelected,
                                onCategoryLongPress = onCategoryLongPress,
                                modifier = Modifier.testTag("category_chips")
                        )
                    }
                    DrawerAppListColumn(
                            listState = listState,
                            modifier =
                                    Modifier.weight(1f)
                                            .fillMaxWidth()
                                            .then(categorySwipeModifier)
                                            .testTag("app_list"),
                            uiState = uiState,
                            showProfileSections = showProfileSections,
                            anyProfileAppsVisible = anyProfileAppsVisible,
                            focusManager = focusManager,
                            onAppClick = onAppClick,
                            onAppLongPress = onAppLongPress,
                            allowCustomDragReorder = allowCustomDragReorder,
                            onReorderProfileSection = onReorderDrawerProfileSection,
                            onReorderPrivateApps = onReorderPrivateDrawerApps
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryActionSheet(
        category: String,
        categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        onDismiss: () -> Unit,
        onRename: (String) -> Unit,
        onEditApps: () -> Unit,
        onDelete: () -> Unit,
        onSetCategoryIcon: (String) -> Unit,
        onResetCategoryIcon: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showIconPickerDialog by remember(category) { mutableStateOf(false) }
    var renameMode by remember(category) { mutableStateOf(false) }
    var renameValue by remember(category) {
        mutableStateOf(categoryChipDisplayLabel(context, category))
    }
    val normalized = renameValue.trim()
    val isReservedDrawerCategory =
            category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ||
                    category.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) ||
                    category.equals(ReservedCategoryNames.WORK, ignoreCase = true) ||
                    category.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)
    val canSaveRename = !isReservedDrawerCategory && normalized.isNotBlank()
    val showEditApps = !isReservedDrawerCategory
    val canDelete = showEditApps
    val displayTitle = categoryChipDisplayLabel(context, category)
    val drawerRailIconKey =
            resolvedCategoryDrawerIconName(context, category, categoryDrawerIconOverrides)

    ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.testTag("category_action_sheet")
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(bottom = 32.dp)
        ) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                if (renameMode) {
                    OutlinedTextField(
                            value = renameValue,
                            onValueChange = { renameValue = it },
                            placeholder = { Text(stringResource(R.string.category_name_label)) },
                            singleLine = true,
                            modifier =
                                    Modifier.weight(1f)
                                            .testTag("category_rename_inline_input")
                    )
                    FokusTextButton(onClick = { renameMode = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    FokusTextButton(
                            enabled = canSaveRename,
                            onClick = {
                                onRename(normalized)
                                onDismiss()
                            }
                    ) { Text(stringResource(R.string.action_save)) }
                } else {
                    Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                    if (!isReservedDrawerCategory) {
                        FokusIconButton(
                                onClick = { renameMode = true },
                                modifier = Modifier.testTag("category_action_rename")
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription =
                                            stringResource(R.string.category_action_rename),
                                    tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            if (showEditApps) {
                DrawerSheetActionRow(
                        icon = Icons.Default.Apps,
                        label = stringResource(R.string.category_action_edit_apps),
                        testTag = "category_action_edit_apps",
                        onClick = onEditApps
                )
            }

            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickableWithSystemSound { showIconPickerDialog = true }
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                    .testTag("category_action_choose_icon")
            ) {
                Icon(
                        imageVector = MinimalIcons.iconFor(drawerRailIconKey),
                        contentDescription = stringResource(R.string.icon_picker_current_icon),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                        text = stringResource(R.string.category_icon_picker_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                )
            }
            DrawerSheetActionRow(
                    icon = Icons.Default.Restore,
                    label = stringResource(R.string.category_action_reset_icon),
                    testTag = "category_action_reset_icon",
                    onClick = onResetCategoryIcon
            )
            if (canDelete) {
                DrawerSheetActionRow(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.category_action_remove),
                        testTag = "category_action_remove",
                        destructive = true,
                        onClick = onDelete
                )
            }
        }
    }

    if (showIconPickerDialog) {
        CategoryIconPickerDialog(
                category = category,
                iconOverrides = categoryDrawerIconOverrides,
                onSelect = { name ->
                    onSetCategoryIcon(name)
                    showIconPickerDialog = false
                },
                onDismiss = { showIconPickerDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
        app: AppInfo,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                    modifier.fillMaxWidth()
                            .combinedClickableWithSystemSound(
                                    onClick = onClick,
                                    onLongClick = onLongClick
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .testTag("app_item_${app.packageName}")
    )
}
