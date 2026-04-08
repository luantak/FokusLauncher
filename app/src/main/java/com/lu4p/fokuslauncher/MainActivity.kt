package com.lu4p.fokuslauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.ui.navigation.FokusNavGraph
import com.lu4p.fokuslauncher.ui.navigation.LauncherHomeCoordinatorViewModel
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import com.lu4p.fokuslauncher.ui.util.ProvideAppLocale
import com.lu4p.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var appRepository: AppRepository

    private val launcherHomeCoordinator: LauncherHomeCoordinatorViewModel by viewModels()

    private var shouldShowStatusBar: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyLauncherScreenOrientation(allowLandscape = false)

        // Preload apps in background to warm up cache
        lifecycleScope.launch(Dispatchers.IO) {
            appRepository.getInstalledAppsOnBackground()
        }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.decorView.isSoundEffectsEnabled = true
        window.decorView.overScrollMode = View.OVER_SCROLL_NEVER
        applySystemBarsAppearance()
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    preferencesManager.showStatusBarFlow.collect { showStatusBar ->
                        shouldShowStatusBar = showStatusBar
                        applySystemBarsAppearance()
                    }
                }
                launch {
                    preferencesManager.allowLandscapeRotationFlow.collect { allowLandscape ->
                        applyLauncherScreenOrientation(allowLandscape)
                    }
                }
            }
        }
        setContent {
            val launcherFontFamilyName by produceState("") {
                preferencesManager.launcherFontFamilyFlow.collect { value = it }
            }
            val appLocaleTag by produceState("") {
                preferencesManager.appLocaleTagFlow.collect { value = it }
            }
            ProvideAppLocale(localeTag = appLocaleTag) {
                FokusLauncherTheme(
                        fontFamily = composeFontFamilyFromStoredName(launcherFontFamilyName)
                ) {
                    FokusNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch {
                val hasCompleted = preferencesManager.hasCompletedOnboardingFlow.first()
                if (hasCompleted) {
                    setIntent(intent)
                    launcherHomeCoordinator.requestGoHome()
                } else {
                    setIntent(Intent(this@MainActivity, MainActivity::class.java))
                }
            }
        } else {
            setIntent(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBarsAppearance()
    }

    private fun applySystemBarsAppearance() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (shouldShowStatusBar) {
                show(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun applyLauncherScreenOrientation(allowLandscape: Boolean) {
        requestedOrientation =
                if (allowLandscape) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }

    companion object {
        /**
         * Expands the notification shade via StatusBarManager.
         * Fallback: show status bar so user can swipe from top.
         */
        fun expandStatusBar(context: Context) {
            try {
                val statusBarManager = context.getSystemService("statusbar")
                val clazz = Class.forName("android.app.StatusBarManager")
                val method = clazz.getMethod("expandNotificationsPanel")

                method.invoke(statusBarManager)
                return
            } catch (_: Exception) { }
            (context as? Activity)?.let { activity ->
                WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}
