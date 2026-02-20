package com.lu4p.fokuslauncher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FokusNavGraphViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val hasCompletedOnboarding = preferencesManager.hasCompletedOnboardingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val showWallpaper = preferencesManager.showWallpaperFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
