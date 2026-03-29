package com.lu4p.fokuslauncher.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class LauncherHomeCoordinatorViewModel @Inject constructor() : ViewModel() {

    private val goHomeChannel = Channel<Unit>(Channel.CONFLATED)

    val goHomeRequests = goHomeChannel.receiveAsFlow()

    fun requestGoHome() {
        goHomeChannel.trySend(Unit)
    }
}
