package com.lu4p.fokuslauncher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.ui.components.CategoryChips
import com.lu4p.fokuslauncher.ui.components.SearchBar
import kotlinx.coroutines.delay

@Composable
fun AppDrawerScreen(
        viewModel: AppDrawerViewModel = hiltViewModel(),
        onSettingsClick: () -> Unit = {},
        onEditCategoryApps: (String) -> Unit = {},
        onClose: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val closeAndReset: () -> Unit = {
        viewModel.resetSearchState()
        onClose()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Close the drawer after an app is auto-launched from search
    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                is DrawerEvent.AutoLaunch -> {
                    closeAndReset()
                }
            }
        }
    }

    AppDrawerContent(
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onCategoryLongPress = viewModel::onCategoryLongPress,
            onAppClick = { target ->
                viewModel.launchTarget(target)
                closeAndReset()
            },
            onAppLongPress = viewModel::onAppLongPress,
            onMenuToggle = viewModel::toggleMenu,
            onMenuDismiss = viewModel::dismissMenu,
            onSettingsClick = {
                viewModel.dismissMenu()
                onSettingsClick()
            },
            onPrivateSpaceToggle = viewModel::togglePrivateSpace,
            onClose = closeAndReset
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
                onRename = { newName -> viewModel.renameCategory(category, newName) },
                onEditApps = {
                    viewModel.dismissCategoryActionSheet()
                    onEditCategoryApps(category)
                },
                onDelete = { viewModel.deleteCategory(category) }
        )
    }
}

@Composable
fun AppDrawerContent(
        uiState: AppDrawerUiState,
        onSearchQueryChanged: (String) -> Unit,
        onCategorySelected: (String) -> Unit,
        onCategoryLongPress: (String) -> Unit = {},
        onAppClick: (LaunchTarget) -> Unit,
        onAppLongPress: (AppInfo) -> Unit = {},
        onMenuToggle: () -> Unit = {},
        onMenuDismiss: () -> Unit = {},
        onSettingsClick: () -> Unit,
        onPrivateSpaceToggle: () -> Unit = {},
        onClose: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val closeWithFocusReset: () -> Unit = {
        focusManager.clearFocus(force = true)
        onClose()
    }

    // Back button closes the drawer through onClose (which also clears search)
    BackHandler { closeWithFocusReset() }

    // Auto-focus the search bar and show keyboard when the drawer opens
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    // Swipe-down-to-close: detect overscroll at the top of the list
    var overscrollY by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When list is at top and user pulls down, accumulate
                if (available.y > 0 && !listState.canScrollBackward) {
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

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .padding(top = 48.dp)
                            .nestedScroll(nestedScrollConnection)
                            .testTag("app_drawer_screen")
    ) {
        // Search bar + menu button row
        Box(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
            SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChanged,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth().padding(end = 40.dp).testTag("search_bar")
            )

            // 3-dot menu with dropdown
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = onMenuToggle, modifier = Modifier.testTag("settings_button")) {
                    Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(expanded = uiState.showMenu, onDismissRequest = onMenuDismiss) {
                    if (uiState.isPrivateSpaceSupported) {
                        DropdownMenuItem(
                                text = {
                                    Text(
                                            text =
                                                    if (uiState.isPrivateSpaceUnlocked)
                                                            "Lock Private Space"
                                                    else "Unlock Private Space"
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
                            text = { Text("Launcher Settings") },
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

        // Category chips
        CategoryChips(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected,
                onCategoryLongPress = onCategoryLongPress,
                modifier = Modifier.testTag("category_chips")
        )

        // App list: normal apps first, then private space (deprioritized)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("app_list")) {
            if (uiState.filteredApps.isNotEmpty()) {
                item {
                    Text(
                            text = uiState.selectedCategory,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
            items(items = uiState.filteredApps, key = { it.packageName }) { app ->
                AppListItem(
                        app = app,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAppClick(LaunchTarget.MainApp(app.packageName))
                        },
                        onLongClick = { onAppLongPress(app) }
                )
            }
            if (uiState.isPrivateSpaceUnlocked && uiState.filteredPrivateSpaceApps.isNotEmpty()) {
                if (uiState.filteredApps.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                item {
                    Text(
                            text = "Private Space",
                            style = MaterialTheme.typography.titleMedium,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryActionSheet(
        category: String,
        onDismiss: () -> Unit,
        onRename: (String) -> Unit,
        onEditApps: () -> Unit,
        onDelete: () -> Unit
) {
    var renameValue by remember(category) { mutableStateOf(category) }
    val normalized = renameValue.trim()
    val canRename =
            normalized.isNotBlank() &&
                    !normalized.equals("All apps", ignoreCase = true) &&
                    !normalized.equals("Private", ignoreCase = true)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        OutlinedTextField(
                value = renameValue,
                onValueChange = { renameValue = it },
                singleLine = true,
                label = { Text("Category name") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        TextButton(
                enabled = canRename,
                onClick = { onRename(renameValue) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Rename")
        }
        TextButton(
                onClick = onEditApps,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Edit apps in category")
        }
        TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Remove category", color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
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
