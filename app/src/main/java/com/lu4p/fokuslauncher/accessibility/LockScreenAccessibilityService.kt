package com.lu4p.fokuslauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.utils.registerBroadcastReceiverNotExported
import com.lu4p.fokuslauncher.utils.tryStartLauncherMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs only when the user enables this app in system accessibility settings. Uses
 * [performGlobalAction] for lock/home and optional return-home-after-long-lock, driven by
 * screen off / user present broadcasts. It does not read other apps' UI (window content is
 * disabled in XML and the service config limits events to this package only).
 */
class LockScreenAccessibilityService : AccessibilityService() {
    private val tag = "FokusLockA11y"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var preferencesManager: PreferencesManager
    private var screenStateReceiverRegistered = false

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
        // Events are scoped to this app in lock_screen_accessibility_config.xml; behavior uses
        // screen broadcasts and scheduling instead of observing other packages.
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(applicationContext)
        registerScreenStateReceiver()
        instance = this
    }

    override fun onDestroy() {
        cancelScheduledReturnHomeAlarm()
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
        maybeReturnHome(trigger = "user_present")
        clearPendingLockTracking("user_present")
    }

    private suspend fun maybeReturnHome(trigger: String) {
        if (!preferencesManager.getLongLockReturnHomeEnabled()) {
            cancelScheduledReturnHomeAlarm()
            preferencesManager.clearLongLockLastScreenOffAtMs()
            return
        }

        val lockedAt = preferencesManager.getLongLockLastScreenOffAtMs()
        if (lockedAt <= 0L) return
        val thresholdMs =
                longLockThresholdMs(preferencesManager.getLongLockReturnHomeThresholdMinutes())
        val elapsedMs = System.currentTimeMillis() - lockedAt
        if (elapsedMs < thresholdMs) return

        Log.d(tag, "Threshold met via $trigger after ${elapsedMs}ms, attempting to return home")

        val returnedHome = performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(tag, "GLOBAL_ACTION_HOME result for $trigger: $returnedHome")

        val launchedLauncher =
                !returnedHome &&
                        run {
                            Log.d(
                                    tag,
                                    "GLOBAL_ACTION_HOME failed, falling back to explicit activity launch"
                            )
                            bringLauncherToFront()
                        }

        if (returnedHome || launchedLauncher) {
            preferencesManager.clearLongLockLastScreenOffAtMs()
        }
    }

    private suspend fun scheduleLockedReturnHome(lockedAtMs: Long) {
        val thresholdMs =
                longLockThresholdMs(preferencesManager.getLongLockReturnHomeThresholdMinutes())
        val triggerAtMs = lockedAtMs + thresholdMs
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        cancelScheduledReturnHomeAlarm()
        val pendingIntent =
                LongLockAlarmReceiver.createPendingIntent(
                        context = this,
                        lockedAtMs = lockedAtMs,
                        createIfMissing = true,
                )
                        ?: return
        Log.d(tag, "Scheduling locked return-home alarm in ${thresholdMs}ms at triggerAt=$triggerAtMs")
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
    }

    private suspend fun clearPendingLockTracking(reason: String) {
        cancelScheduledReturnHomeAlarm()
        val lockedAt = preferencesManager.getLongLockLastScreenOffAtMs()
        if (lockedAt > 0L) {
            Log.d(tag, "Clearing pending lock tracking via $reason")
            preferencesManager.clearLongLockLastScreenOffAtMs()
        }
    }

    private fun bringLauncherToFront(): Boolean {
        val (ok, err) = tryStartLauncherMainActivity()
        if (ok) Log.d(tag, "Explicit launcher activity start succeeded")
        else Log.d(tag, "Explicit launcher activity start failed: $err")
        return ok
    }

    private fun cancelScheduledReturnHomeAlarm() {
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent =
                LongLockAlarmReceiver.createPendingIntent(
                        context = this,
                        createIfMissing = false,
                )
                        ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun registerScreenStateReceiver() {
        if (screenStateReceiverRegistered) return
        val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
        registerBroadcastReceiverNotExported(screenStateReceiver, filter)
        screenStateReceiverRegistered = true
    }

    private fun unregisterScreenStateReceiver() {
        if (!screenStateReceiverRegistered) return
        runCatching { unregisterReceiver(screenStateReceiver) }
        screenStateReceiverRegistered = false
    }

    private fun longLockThresholdMs(minutes: Int): Long = minutes * 60_000L

    companion object {
        @Volatile private var instance: LockScreenAccessibilityService? = null

        fun lockScreenNow(): Boolean {
            val svc = instance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
