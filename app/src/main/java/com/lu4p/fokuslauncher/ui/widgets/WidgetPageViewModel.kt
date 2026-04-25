package com.lu4p.fokuslauncher.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.HOSTED_WIDGET_DEFAULT_HEIGHT_DP
import com.lu4p.fokuslauncher.data.model.HostedWidget
import com.lu4p.fokuslauncher.data.model.clampHostedWidgetHeightDp
import com.lu4p.fokuslauncher.data.widget.WidgetHostManager
import com.lu4p.fokuslauncher.data.widget.WidgetProviderInfo
import com.lu4p.fokuslauncher.data.widget.WidgetProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WidgetPageUiState(
        val widgets: List<HostedWidget> = emptyList(),
        val widgetsLoaded: Boolean = false,
        val providers: List<WidgetProviderInfo> = emptyList(),
        val showActionSheet: Boolean = false,
        val showProviderPicker: Boolean = false,
        val manageMode: Boolean = false,
)

sealed interface WidgetPageEvent {
    data class RequestBind(val appWidgetId: Int, val provider: ComponentName) : WidgetPageEvent
    data class RequestConfigure(val appWidgetId: Int, val configure: ComponentName) :
            WidgetPageEvent

    data class ShowToast(val messageRes: Int) : WidgetPageEvent
}

@HiltViewModel
class WidgetPageViewModel
@Inject
constructor(
        private val preferencesManager: PreferencesManager,
        private val widgetHostManager: WidgetHostManager,
        private val widgetProviderRepository: WidgetProviderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WidgetPageUiState())
    val uiState: StateFlow<WidgetPageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WidgetPageEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<WidgetPageEvent> = _events.asSharedFlow()

    private var pendingAdd: PendingWidgetAdd? = null

    init {
        refreshProviders()
        observeWidgets()
    }

    fun startListening() {
        widgetHostManager.startListening()
        refreshStoredWidgets()
    }

    fun stopListening() = widgetHostManager.stopListening()

    fun leaveWidgetPage() {
        _uiState.value =
                _uiState.value.copy(
                        showActionSheet = false,
                        showProviderPicker = false,
                        manageMode = false,
                )
        stopListening()
    }

    fun createWidgetView(appWidgetId: Int) = widgetHostManager.createView(appWidgetId)

    fun openActions() {
        _uiState.value = _uiState.value.copy(showActionSheet = true)
    }

    fun dismissActions() {
        _uiState.value = _uiState.value.copy(showActionSheet = false)
    }

    fun openProviderPicker() {
        refreshProviders()
        _uiState.value =
                _uiState.value.copy(showActionSheet = false, showProviderPicker = true)
    }

    fun dismissProviderPicker() {
        _uiState.value = _uiState.value.copy(showProviderPicker = false)
    }

    fun enterManageMode() {
        _uiState.value =
                _uiState.value.copy(
                        showActionSheet = false,
                        manageMode = true,
                )
    }

    fun editWidgets() {
        _uiState.value =
                _uiState.value.copy(
                        showActionSheet = false,
                        manageMode = true,
                )
    }

    fun exitManageMode() {
        _uiState.value = _uiState.value.copy(manageMode = false)
    }

    fun addProvider(provider: WidgetProviderInfo) {
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        pendingAdd = PendingWidgetAdd(appWidgetId, provider)
        _uiState.value = _uiState.value.copy(showProviderPicker = false)
        if (widgetHostManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)) {
            continueAfterBind()
        } else {
            _events.tryEmit(WidgetPageEvent.RequestBind(appWidgetId, provider.provider))
        }
    }

    fun onBindResult(success: Boolean) {
        if (!success) {
            failPendingAdd(R.string.widget_bind_failed)
            return
        }
        continueAfterBind()
    }

    fun onConfigureResult(success: Boolean) {
        if (!success) {
            failPendingAdd(R.string.widget_configure_failed)
            return
        }
        persistPendingAdd()
    }

    fun removeWidget(widget: HostedWidget) {
        viewModelScope.launch {
            val remaining =
                    storedWidgets()
                            .filterNot { it.id == widget.id }
                            .mapIndexed { index, item -> item.copy(position = index) }
            preferencesManager.setHostedWidgets(remaining, allowEmpty = true)
            widgetHostManager.deleteAppWidgetId(widget.appWidgetId)
        }
    }

    fun moveWidget(widgetId: String, direction: Int) {
        viewModelScope.launch {
            val current = storedWidgets().sortedBy { it.position }
            val from = current.indexOfFirst { it.id == widgetId }
            val to =
                    (from + direction).takeIf { from >= 0 && it in current.indices }
                            ?: return@launch
            val reordered =
                    current.toMutableList()
                            .apply { add(to, removeAt(from)) }
                            .mapIndexed { index, widget -> widget.copy(position = index) }
            preferencesManager.setHostedWidgets(reordered)
        }
    }

    fun resizeWidget(widgetId: String, heightDp: Int) {
        viewModelScope.launch {
            val resized =
                    storedWidgets().map { widget ->
                        if (widget.id == widgetId) {
                            widget.copy(heightDp = clampHostedWidgetHeightDp(heightDp))
                        } else {
                            widget
                        }
                    }
            preferencesManager.setHostedWidgets(resized)
        }
    }

    private fun continueAfterBind() {
        val pending = pendingAdd ?: return
        val configure = pending.provider.appWidgetProviderInfo.configure
        if (configure != null) {
            _events.tryEmit(WidgetPageEvent.RequestConfigure(pending.appWidgetId, configure))
        } else {
            persistPendingAdd()
        }
    }

    private fun persistPendingAdd() {
        val pending = pendingAdd ?: return
        pendingAdd = null
        viewModelScope.launch {
            val info = widgetHostManager.getAppWidgetInfo(pending.appWidgetId)
            val providerInfo = info ?: pending.provider.appWidgetProviderInfo
            val provider = providerInfo.provider ?: pending.provider.provider
            val current = storedWidgets()
            val nextPosition = (current.maxOfOrNull { it.position } ?: -1) + 1
            val heightDp =
                    pending.provider.defaultHeightDp.takeIf { it > 0 }
                            ?: defaultHeightDp(providerInfo)
            val widget =
                    HostedWidget(
                            id = UUID.randomUUID().toString(),
                            appWidgetId = pending.appWidgetId,
                            providerPackageName = provider.packageName,
                            providerClassName = provider.className,
                            label = pending.provider.label,
                            heightDp = clampHostedWidgetHeightDp(heightDp),
                            position = nextPosition,
                    )
            preferencesManager.setHostedWidgets(current + widget)
        }
    }

    private suspend fun storedWidgets(): List<HostedWidget> =
            preferencesManager.hostedWidgetsFlow.first()

    private fun failPendingAdd(messageRes: Int) {
        pendingAdd?.let { widgetHostManager.deleteAppWidgetId(it.appWidgetId) }
        pendingAdd = null
        _events.tryEmit(WidgetPageEvent.ShowToast(messageRes))
    }

    private fun refreshProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                    _uiState.value.copy(
                            providers = widgetProviderRepository.getOwnerProfileProviders()
                    )
        }
    }

    private fun observeWidgets() {
        viewModelScope.launch {
            preferencesManager.hostedWidgetsFlow.collectLatest { stored ->
                applyStoredWidgets(stored)
            }
        }
    }

    private fun refreshStoredWidgets() {
        viewModelScope.launch {
            applyStoredWidgets(storedWidgets())
        }
    }

    private suspend fun applyStoredWidgets(stored: List<HostedWidget>) {
        val repaired = repairHostedWidgetBindings(stored)
        if (repaired != stored) {
            preferencesManager.setHostedWidgets(repaired)
        }
        _uiState.value = _uiState.value.copy(widgets = repaired, widgetsLoaded = true)
    }

    private fun repairHostedWidgetBindings(widgets: List<HostedWidget>): List<HostedWidget> {
        if (widgets.isEmpty()) return widgets
        val boundIds = widgetHostManager.getAppWidgetIds().toSet()
        var changed = false
        val repaired =
                widgets.map { widget ->
                    val stillBound =
                            widget.appWidgetId in boundIds &&
                                    widgetHostManager.getAppWidgetInfo(widget.appWidgetId) != null
                    if (stillBound) return@map widget

                    val provider =
                            ComponentName(
                                    widget.providerPackageName,
                                    widget.providerClassName,
                            )
                    val replacementId = widgetHostManager.allocateAppWidgetId()
                    val rebound =
                            widgetHostManager.bindAppWidgetIdIfAllowed(
                                    replacementId,
                                    provider,
                            )
                    if (rebound) {
                        changed = true
                        widget.copy(appWidgetId = replacementId)
                    } else {
                        widgetHostManager.deleteAppWidgetId(replacementId)
                        widget
                    }
                }
        return if (changed) repaired else widgets
    }

    private fun defaultHeightDp(info: AppWidgetProviderInfo): Int =
            clampHostedWidgetHeightDp(
                    listOf(info.minResizeHeight, info.minHeight)
                            .firstOrNull { it > 0 }
                            ?: HOSTED_WIDGET_DEFAULT_HEIGHT_DP
            )

    private data class PendingWidgetAdd(
            val appWidgetId: Int,
            val provider: WidgetProviderInfo,
    )
}
