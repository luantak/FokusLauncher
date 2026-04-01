package com.lu4p.fokuslauncher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.ui.components.CategoryChips
import com.lu4p.fokuslauncher.ui.components.DrawerCategorySidebar
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.components.SearchBar
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel
import java.util.Locale

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
        modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = onMenuToggle, modifier = Modifier.testTag("settings_button")) {
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
                        onClick = onPrivateSpaceToggle,
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
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.drawer_menu_launcher_settings)) },
                    onClick = onSettingsClick,
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

@Composable
private fun DrawerAppListColumn(
        listState: LazyListState,
        modifier: Modifier,
        uiState: AppDrawerUiState,
        showProfileSections: Boolean,
        anyProfileAppsVisible: Boolean,
        focusManager: FocusManager,
        onAppClick: (LaunchTarget) -> Unit,
        onAppLongPress: (AppInfo) -> Unit
) {
    LazyColumn(state = listState, modifier = modifier) {
        if (showProfileSections) {
            var hasEmittedProfileListContent = false
            for (section in uiState.filteredProfileSections) {
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
                        items = section.apps,
                        key = { app ->
                            val cn = app.componentName
                            val uh = app.userHandle
                            val appKey =
                                    if (cn != null && uh != null) {
                                        "${app.packageName}:${cn.flattenToString()}:${uh.hashCode()}"
                                    } else {
                                        app.packageName
                                    }
                            "${section.id}_$appKey"
                        }
                ) { app ->
                    AppListItem(
                            app = app,
                            onClick = {
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
                            onLongClick = { onAppLongPress(app) }
                    )
                }
            }
        }
        if (uiState.isPrivateSpaceUnlocked && uiState.filteredPrivateSpaceApps.isNotEmpty()) {
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
                    items = uiState.filteredPrivateSpaceApps,
                    key = { "private_${it.packageName}" }
            ) { app ->
                AppListItem(
                        app = app,
                        onClick = {
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
                        onLongClick = { onAppLongPress(app) }
                )
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
            onClose = closeAndReset,
            useSidebarCategoryDrawer = uiState.useSidebarCategoryDrawer,
            drawerCategorySidebarOnRight = uiState.drawerCategorySidebarOnRight,
            categoryDrawerIconOverrides = uiState.categoryDrawerIconOverrides
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
        onClose: () -> Unit = {}
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
                                IconButton(
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
                                        onSettingsClick = onSettingsClick
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
                                    onAppLongPress = onAppLongPress
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
                    Box(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                        SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged,
                                placeholder = stringResource(R.string.search_apps_hint),
                                focusRequester = focusRequester,
                                onImeAction = onSearchImeAction,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(end = 40.dp)
                                                .testTag("search_bar")
                        )
                        DrawerOverflowMenu(
                                uiState = uiState,
                                onMenuToggle = onMenuToggle,
                                onMenuDismiss = onMenuDismiss,
                                onPrivateSpaceToggle = onPrivateSpaceToggle,
                                onSettingsClick = onSettingsClick,
                                modifier = Modifier.align(Alignment.CenterEnd)
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
                            onAppLongPress = onAppLongPress
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
        onDismiss: () -> Unit,
        onRename: (String) -> Unit,
        onEditApps: () -> Unit,
        onDelete: () -> Unit,
        onSetCategoryIcon: (String) -> Unit,
        onResetCategoryIcon: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
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
                    TextButton(onClick = { renameMode = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
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
                        IconButton(
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

            Text(
                    text = stringResource(R.string.category_icon_picker_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier =
                            Modifier.heightIn(max = 220.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MinimalIcons.names, key = { it }) { name ->
                    IconButton(
                            onClick = { onSetCategoryIcon(name) },
                            modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                                imageVector = MinimalIcons.iconFor(name),
                                contentDescription = name,
                                tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
                            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .testTag("app_item_${app.packageName}")
    )
}
