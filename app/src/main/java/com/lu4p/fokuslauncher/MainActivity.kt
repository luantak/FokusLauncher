package com.lu4p.fokuslauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.ui.navigation.FokusNavGraph
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.decorView.overScrollMode = View.OVER_SCROLL_NEVER
        hideStatusBar()
        setContent {
            FokusLauncherTheme {
                FokusNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            CoroutineScope(Dispatchers.Main).launch {
                val hasCompleted = preferencesManager.hasCompletedOnboardingFlow.first()
                if (!hasCompleted) {
                    setIntent(Intent(this@MainActivity, MainActivity::class.java))
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
