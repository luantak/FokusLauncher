package com.lu4p.fokuslauncher.ui.onboarding

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.provider.Settings
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.flow.stateIn
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

    fun setShowWallpaper(show: Boolean) {
        viewModelScope.launch { preferencesManager.setShowWallpaper(show) }
    }

    fun setBlackWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create a larger black bitmap to ensure it applies correctly
                val width = context.resources.displayMetrics.widthPixels
                val height = context.resources.displayMetrics.heightPixels
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.BLACK)

                val wallpaperManager = WallpaperManager.getInstance(context)
                // Set for both home screen and lock screen (if supported)
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } catch (_: Exception) {
                    // Lock screen wallpaper may fail on some devices, ignore
                }

                // Also set launcher to show black (not transparent)
                preferencesManager.setShowWallpaper(false)
            } catch (e: Exception) {
                e.printStackTrace()
                // If setting wallpaper fails, at least set the launcher preference
                preferencesManager.setShowWallpaper(false)
            }
        }
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
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(
            homeIntent, PackageManager.MATCH_DEFAULT_ONLY
        )
        val isDefault = resolveInfo?.activityInfo?.packageName == context.packageName
        _isDefaultLauncher.value = isDefault
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

    fun onSkip() {
        onNext()
    }

    fun onSkipLocation() {
        viewModelScope.launch {
            preferencesManager.setWeatherLocationOptedOut(true)
        }
        onNext()
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
