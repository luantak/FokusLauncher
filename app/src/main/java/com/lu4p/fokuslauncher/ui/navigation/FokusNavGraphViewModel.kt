package com.lu4p.fokuslauncher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import com.lu4p.fokuslauncher.ui.util.stateWhileSubscribedIn
import javax.inject.Inject

@HiltViewModel
class FokusNavGraphViewModel @Inject constructor(
    preferencesManager: PreferencesManager
) : ViewModel() {

    val hasCompletedOnboarding =
            preferencesManager.hasCompletedOnboardingFlow.stateWhileSubscribedIn(
                    viewModelScope,
                    false,
            )
}
