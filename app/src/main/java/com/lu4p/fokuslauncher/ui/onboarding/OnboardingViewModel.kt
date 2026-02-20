package com.lu4p.fokuslauncher.ui.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    LOCATION,
    SET_DEFAULT_LAUNCHER,
    CUSTOMIZE_HOME,
    SWIPE_SHORTCUTS,
    QUICK_TIPS
}

data class SwipeShortcutsState(
    val allApps: List<AppInfo> = emptyList(),
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

    /** Ordered list of steps to show. SET_DEFAULT_LAUNCHER is omitted when already default. */
    val steps: StateFlow<List<OnboardingStep>> = _isDefaultLauncher.map { isDefault ->
        if (isDefault) {
            listOf(
                OnboardingStep.WELCOME,
                OnboardingStep.LOCATION,
                OnboardingStep.CUSTOMIZE_HOME,
                OnboardingStep.SWIPE_SHORTCUTS,
                OnboardingStep.QUICK_TIPS
            )
        } else {
            listOf(
                OnboardingStep.WELCOME,
                OnboardingStep.LOCATION,
                OnboardingStep.SET_DEFAULT_LAUNCHER,
                OnboardingStep.CUSTOMIZE_HOME,
                OnboardingStep.SWIPE_SHORTCUTS,
                OnboardingStep.QUICK_TIPS
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Swipe shortcuts state for SWIPE_SHORTCUTS step
    val swipeShortcutsState: StateFlow<SwipeShortcutsState> = combine(
        preferencesManager.swipeLeftTargetFlow,
        preferencesManager.swipeRightTargetFlow
    ) { swipeLeft, swipeRight ->
        SwipeShortcutsState(
            allApps = appRepository.getInstalledApps(),
            swipeLeftTarget = swipeLeft,
            swipeRightTarget = swipeRight
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeShortcutsState())

    fun setSwipeLeftTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeLeftTarget(target) }
    }

    fun setSwipeRightTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeRightTarget(target) }
    }

    val currentStep: StateFlow<OnboardingStep?> = combine(
        steps,
        _currentStepIndex
    ) { stepList, index ->
        stepList.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isLastStep: StateFlow<Boolean> = combine(
        steps,
        _currentStepIndex
    ) { stepList, index ->
        index >= stepList.lastIndex
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        checkDefaultLauncher()
        viewModelScope.launch {
            if (_isDefaultLauncher.value) {
                val reached = preferencesManager.getOnboardingReachedSetDefault()
                if (reached) {
                    preferencesManager.setOnboardingReachedSetDefault(false)
                    // Restore to CUSTOMIZE_HOME (index 2 in 5-step flow without SET_DEFAULT_LAUNCHER)
                    _currentStepIndex.value = 2
                }
            }
        }
    }

    private fun checkDefaultLauncher() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(
            homeIntent, PackageManager.MATCH_DEFAULT_ONLY
        )
        val isDefault = resolveInfo?.activityInfo?.packageName == context.packageName
        _isDefaultLauncher.value = isDefault
    }

    fun onNext() {
        val stepList = steps.value
        if (_currentStepIndex.value < stepList.lastIndex) {
            _currentStepIndex.value += 1
        }
    }

    fun onSkip() {
        onNext()
    }

    fun onSkipLocation() {
        viewModelScope.launch {
            preferencesManager.setWeatherLocationOptedOut(true)
        }
        onNext()
    }

    fun onChooseApps(onNavigateToHomeWithEdit: () -> Unit) {
        onNext()
        onNavigateToHomeWithEdit()
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
            withContext(Dispatchers.Main.immediate) {
                try {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun recheckDefaultLauncher() {
        checkDefaultLauncher()
    }
}
