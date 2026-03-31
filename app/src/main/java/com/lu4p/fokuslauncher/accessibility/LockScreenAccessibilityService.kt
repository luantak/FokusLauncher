package com.lu4p.fokuslauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.lu4p.fokuslauncher.MainActivity
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Minimal service used only for [performGlobalAction] lock screen from double-tap. */
class LockScreenAccessibilityService : AccessibilityService() {
    private val tag = "FokusLockA11y"
    private val transientLockStateGraceMs = 2_000L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var preferencesManager: PreferencesManager
    private var screenStateReceiverRegistered = false
    private var scheduledReturnHomeJob: Job? = null

    private val screenStateReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_OFF -> serviceScope.launch { handleScreenOff() }
                        Intent.ACTION_USER_PRESENT -> serviceScope.launch { handleUserPresent() }
                    }
                }
            }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString().orEmpty()
        serviceScope.launch { handleWindowStateChanged(packageName) }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(applicationContext)
        registerScreenStateReceiver()
        instance = this
    }

    override fun onDestroy() {
        scheduledReturnHomeJob?.cancel()
        unregisterScreenStateReceiver()
        serviceScope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private suspend fun handleScreenOff() {
        if (!preferencesManager.getLongLockReturnHomeEnabled()) return
        Log.d(tag, "Screen turned off, recording timestamp")
        val now = System.currentTimeMillis()
        preferencesManager.setLongLockLastScreenOffAtMs(now)
        scheduleLockedReturnHome(now)
    }

    private suspend fun handleUserPresent() {
        clearPendingLockTracking("user_present")
        maybeReturnHome(trigger = "user_present")
    }

    private suspend fun handleWindowStateChanged(packageName: String) {
        if (packageName == this.packageName) return
        if (!isDeviceCurrentlyLocked()) {
            val lockedAt = preferencesManager.getLongLockLastScreenOffAtMs()
            val withinTransientGrace =
                    lockedAt > 0L && (System.currentTimeMillis() - lockedAt) < transientLockStateGraceMs
            if (withinTransientGrace) {
                Log.d(tag, "Ignoring transient unlocked window event from $packageName")
                return
            }
            clearPendingLockTracking("window:$packageName")
            return
        }
        maybeReturnHome(trigger = "window:$packageName")
    }

    private suspend fun maybeReturnHome(trigger: String) {
        if (!preferencesManager.getLongLockReturnHomeEnabled()) {
            scheduledReturnHomeJob?.cancel()
            preferencesManager.clearLongLockLastScreenOffAtMs()
            return
        }

        val lockedAt = preferencesManager.getLongLockLastScreenOffAtMs()
        if (lockedAt <= 0L) return
        val thresholdMinutes = preferencesManager.getLongLockReturnHomeThresholdMinutes()
        val thresholdMs = thresholdMinutes * 60_000L
        val elapsedMs = System.currentTimeMillis() - lockedAt
        if (elapsedMs < thresholdMs) return

        Log.d(
                tag,
                "Threshold met via $trigger after ${elapsedMs}ms, attempting to return home"
        )

        val returnedHome = performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(tag, "GLOBAL_ACTION_HOME result for $trigger: $returnedHome")

        val launchedLauncher =
                if (trigger == "scheduled_locked") {
                    Log.d(tag, "Attempting explicit launcher foreground while device remains locked")
                    bringLauncherToFront()
                } else if (!returnedHome) {
                    Log.d(tag, "GLOBAL_ACTION_HOME failed, falling back to explicit activity launch")
                    bringLauncherToFront()
                } else {
                    false
                }

        if (returnedHome || launchedLauncher) {
            preferencesManager.clearLongLockLastScreenOffAtMs()
        }
    }

    private suspend fun scheduleLockedReturnHome(lockedAtMs: Long) {
        scheduledReturnHomeJob?.cancel()
        val thresholdMinutes = preferencesManager.getLongLockReturnHomeThresholdMinutes()
        val thresholdMs = thresholdMinutes * 60_000L
        Log.d(tag, "Scheduling locked return-home in ${thresholdMs}ms")
        scheduledReturnHomeJob =
                serviceScope.launch {
                    delay(thresholdMs)
                    val latestLockedAt = preferencesManager.getLongLockLastScreenOffAtMs()
                    if (latestLockedAt != lockedAtMs || latestLockedAt <= 0L) return@launch
                    if (!isDeviceCurrentlyLocked()) {
                        clearPendingLockTracking("scheduled_unlocked")
                        return@launch
                    }
                    maybeReturnHome(trigger = "scheduled_locked")
                }
    }

    private suspend fun clearPendingLockTracking(reason: String) {
        scheduledReturnHomeJob?.cancel()
        val lockedAt = preferencesManager.getLongLockLastScreenOffAtMs()
        if (lockedAt > 0L) {
            Log.d(tag, "Clearing pending lock tracking via $reason")
            preferencesManager.clearLongLockLastScreenOffAtMs()
        }
    }

    private fun isDeviceCurrentlyLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java) ?: return false
        return keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
    }

    private fun bringLauncherToFront(): Boolean {
        return runCatching {
            startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        component = ComponentName(this@LockScreenAccessibilityService, MainActivity::class.java)
                        addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                    }
            )
            Log.d(tag, "Explicit launcher activity start succeeded")
            true
        }.getOrElse {
            Log.d(tag, "Explicit launcher activity start failed: ${it.javaClass.simpleName}")
            false
        }
    }

    private fun registerScreenStateReceiver() {
        if (screenStateReceiverRegistered) return
        val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenStateReceiver, filter)
        }
        screenStateReceiverRegistered = true
    }

    private fun unregisterScreenStateReceiver() {
        if (!screenStateReceiverRegistered) return
        runCatching { unregisterReceiver(screenStateReceiver) }
        screenStateReceiverRegistered = false
    }

    companion object {
        @Volatile private var instance: LockScreenAccessibilityService? = null

        fun lockScreenNow(): Boolean {
            val svc = instance ?: return false
            return svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
