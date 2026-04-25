package com.lu4p.fokuslauncher.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.FokusBottomSheet
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import com.lu4p.fokuslauncher.ui.components.SheetActionRow

@Composable
fun WidgetPageScreen(
        onClose: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: WidgetPageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val bindLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                viewModel.onBindResult(result.resultCode == Activity.RESULT_OK)
            }
    val configureLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                viewModel.onConfigureResult(result.resultCode == Activity.RESULT_OK)
            }

    DisposableEffect(viewModel) {
        viewModel.startListening()
        onDispose { viewModel.leaveWidgetPage() }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is WidgetPageEvent.RequestBind -> {
                    bindLauncher.launch(
                            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, event.appWidgetId)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, event.provider)
                            }
                    )
                }
                is WidgetPageEvent.RequestConfigure -> {
                    configureLauncher.launch(
                            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                component = event.configure
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, event.appWidgetId)
                            }
                    )
                }
                is WidgetPageEvent.ShowToast ->
                        Toast.makeText(context, event.messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val leavePage = {
        viewModel.exitManageMode()
        onClose()
    }

    BackHandler(onBack = leavePage)

    WidgetPageContent(
            uiState = uiState,
            createWidgetView = viewModel::createWidgetView,
            onLongPress = viewModel::openActions,
            onEditWidgets = viewModel::editWidgets,
            onAddWidget = viewModel::openProviderPicker,
            onDismissActions = viewModel::dismissActions,
            onDismissPicker = viewModel::dismissProviderPicker,
            onSelectProvider = viewModel::addProvider,
            onRemoveWidget = viewModel::removeWidget,
            onMoveWidget = viewModel::moveWidget,
            onResizeWidget = viewModel::resizeWidget,
            onDoneManage = viewModel::exitManageMode,
            modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WidgetPageContent(
        uiState: WidgetPageUiState,
        createWidgetView: (Int) -> android.view.View?,
        onLongPress: () -> Unit,
        onEditWidgets: () -> Unit,
        onAddWidget: () -> Unit,
        onDismissActions: () -> Unit,
        onDismissPicker: () -> Unit,
        onSelectProvider: (com.lu4p.fokuslauncher.data.widget.WidgetProviderInfo) -> Unit,
        onRemoveWidget: (com.lu4p.fokuslauncher.data.model.HostedWidget) -> Unit,
        onMoveWidget: (String, Int) -> Unit,
        onResizeWidget: (String, Int) -> Unit,
        onDoneManage: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
            modifier =
                    modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {},
                                    onLongClick = onLongPress,
                            )
                            .testTag("widget_page")
    ) {
        if (uiState.widgets.isEmpty() && uiState.widgetsLoaded) {
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                            Modifier.fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(32.dp),
            ) {
                Text(
                        text = stringResource(R.string.no_widgets),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.padding(top = 12.dp))
                FokusIconButton(onClick = onAddWidget) {
                    com.lu4p.fokuslauncher.ui.components.LauncherIcon(
                            Icons.Default.Add,
                            stringResource(R.string.add_widget),
                            tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else if (uiState.widgets.isNotEmpty()) {
            LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier =
                            Modifier.fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                if (uiState.manageMode) {
                    stickyHeader(key = "widget_edit_done") {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(top = 6.dp, bottom = 10.dp)
                                                .zIndex(1f)
                        ) {
                            Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.extraLarge,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                            ) {
                                FokusIconButton(onClick = onDoneManage) {
                                    com.lu4p.fokuslauncher.ui.components.LauncherIcon(
                                            Icons.Default.Check,
                                            stringResource(R.string.action_done),
                                            tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
                items(uiState.widgets, key = { it.id }) { widget ->
                    val index = uiState.widgets.indexOf(widget)
                    HostedWidgetView(
                            widget = widget,
                            editMode = uiState.manageMode,
                            canMoveUp = index > 0,
                            canMoveDown = index < uiState.widgets.lastIndex,
                            createView = createWidgetView,
                            onLongPress = onEditWidgets,
                            onRemove = { onRemoveWidget(widget) },
                            onMoveUp = { onMoveWidget(widget.id, -1) },
                            onMoveDown = { onMoveWidget(widget.id, 1) },
                            onResize = { height -> onResizeWidget(widget.id, height) },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

    }

    if (uiState.showActionSheet) {
        FokusBottomSheet(onDismissRequest = onDismissActions) {
            SheetActionRow(
                    label = stringResource(R.string.add_widget),
                    icon = Icons.Default.Add,
                    onClick = onAddWidget,
            )
        }
    }

    if (uiState.showProviderPicker) {
        WidgetProviderPickerDialog(
                providers = uiState.providers,
                onSelect = onSelectProvider,
                onDismiss = onDismissPicker,
        )
    }
}
