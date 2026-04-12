package com.lu4p.fokuslauncher.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.utils.WallpaperHelper
import com.lu4p.fokuslauncher.utils.isDefaultHomeApp
import com.lu4p.fokuslauncher.utils.openDefaultLauncherSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.lu4p.fokuslauncher.ui.util.stateWhileSubscribedIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    BACKGROUND,
    LOCATION,
    SET_DEFAULT_LAUNCHER,
    CUSTOMIZE_HOME,
    SWIPE_SHORTCUTS,
    QUICK_TIPS
}

data class SwipeShortcutsState(
    val allApps: List<AppInfo> = emptyList(),
    val allShortcutActions: List<AppShortcutAction> = emptyList(),
    val swipeLeftTarget: ShortcutTarget? = null,
    val swipeRightTarget: ShortcutTarget? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _currentStepIndex = MutableStateFlow(0)

    private val _isDefaultLauncher = MutableStateFlow(true)

    private val _hasLocationPermission = MutableStateFlow(false)

    /** Ordered list of steps to show. SET_DEFAULT_LAUNCHER is omitted when already default, LOCATION is omitted when permission already granted. */
    val steps: StateFlow<List<OnboardingStep>> = combine(_isDefaultLauncher, _hasLocationPermission) { isDefault, hasLocation ->
        buildList {
            add(OnboardingStep.WELCOME)
            add(OnboardingStep.BACKGROUND)
            if (!hasLocation) {
                add(OnboardingStep.LOCATION)
            }
            if (!isDefault) {
                add(OnboardingStep.SET_DEFAULT_LAUNCHER)
            }
            add(OnboardingStep.CUSTOMIZE_HOME)
            add(OnboardingStep.SWIPE_SHORTCUTS)
            add(OnboardingStep.QUICK_TIPS)
        }
    }.stateWhileSubscribedIn(viewModelScope, emptyList())

    // Swipe shortcuts state for SWIPE_SHORTCUTS step
    val swipeShortcutsState: StateFlow<SwipeShortcutsState> = combine(
        preferencesManager.swipeLeftTargetFlow,
        preferencesManager.swipeRightTargetFlow,
        appRepository.getInstalledAppsVersion()
    ) { swipeLeft, swipeRight, _ ->
        SwipeShortcutsState(
            allApps = appRepository.getInstalledApps(),
            allShortcutActions = appRepository.getAllShortcutActions(),
            swipeLeftTarget = swipeLeft,
            swipeRightTarget = swipeRight
        )
    }.stateWhileSubscribedIn(viewModelScope, SwipeShortcutsState())

    fun setSwipeLeftTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeLeftTarget(target) }
    }

    fun setSwipeRightTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeRightTarget(target) }
    }

    /** Sets solid black wallpaper, records it, then advances (order guaranteed). */
    fun onBackgroundChooseBlack() {
        viewModelScope.launch {
            WallpaperHelper.setBlackWallpaper(context)
            preferencesManager.setHomeUsesPhotoWallpaper(false)
            onNext()
        }
    }

    /** User kept their existing (typically image) wallpaper; record then advance. */
    fun onBackgroundChooseWallpaper() {
        viewModelScope.launch {
            preferencesManager.setHomeUsesPhotoWallpaper(true)
            onNext()
        }
    }

    val currentStep: StateFlow<OnboardingStep?> = combine(
        steps,
        _currentStepIndex
    ) { stepList, index ->
        stepList.getOrNull(index)
    }.stateWhileSubscribedIn(viewModelScope, null)

    val isLastStep: StateFlow<Boolean> = combine(
        steps,
        _currentStepIndex
    ) { stepList, index ->
        index >= stepList.lastIndex
    }.stateWhileSubscribedIn(viewModelScope, false)

    init {
        // Always start at the first step when onboarding is shown
        _currentStepIndex.value = 0

        checkDefaultLauncher()
        checkLocationPermission()
        viewModelScope.launch {
            if (_isDefaultLauncher.value) {
                val reached = preferencesManager.getOnboardingReachedSetDefault()
                if (reached) {
                    preferencesManager.setOnboardingReachedSetDefault(false)
                    // Restore to CUSTOMIZE_HOME (index depends on whether LOCATION was shown)
                    val locationShown = !_hasLocationPermission.value
                    _currentStepIndex.value = if (locationShown) 3 else 2
                }
            }
        }
    }

    private fun checkDefaultLauncher() {
        _isDefaultLauncher.value = context.isDefaultHomeApp()
    }

    private fun checkLocationPermission() {
        _hasLocationPermission.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onNext() {
        val stepList = steps.value
        if (_currentStepIndex.value < stepList.lastIndex) {
            _currentStepIndex.value += 1
        }
    }

    private val _showEditHomeApps = MutableStateFlow(false)
    val showEditHomeApps: StateFlow<Boolean> = _showEditHomeApps

    fun onChooseApps() {
        _showEditHomeApps.value = true
    }

    fun onEditHomeAppsDismissed() {
        _showEditHomeApps.value = false
        onNext()
    }

    fun onDone(onNavigateToHome: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setHasCompletedOnboarding(true)
            onNavigateToHome()
        }
    }

    fun openDefaultLauncherSettings() {
        viewModelScope.launch {
            preferencesManager.setOnboardingReachedSetDefault(true)
            withContext(Dispatchers.Main.immediate) { context.openDefaultLauncherSettings() }
        }
    }

    fun recheckDefaultLauncher() {
        checkDefaultLauncher()
    }
}
