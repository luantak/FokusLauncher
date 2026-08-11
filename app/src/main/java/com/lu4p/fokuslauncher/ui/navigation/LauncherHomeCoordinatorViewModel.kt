package com.lu4p.fokuslauncher.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class LauncherHomeCoordinatorViewModel @Inject constructor() : ViewModel() {

    private val _goHomeRequests =
            MutableSharedFlow<Unit>(
                    extraBufferCapacity = 1,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

    /** Broadcast when the system home affordance asks the launcher to return to the home surface. */
    val goHomeRequests: SharedFlow<Unit> = _goHomeRequests.asSharedFlow()

    fun requestGoHome() {
        _goHomeRequests.tryEmit(Unit)
    }
}
