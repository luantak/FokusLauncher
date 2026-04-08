package com.lu4p.fokuslauncher.accessibility

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.utils.tryStartLauncherMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LongLockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RETURN_HOME_AFTER_LONG_LOCK) return

        Log.d(TAG, "Alarm receiver fired")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val preferencesManager = PreferencesManager(appContext)
                if (!preferencesManager.getLongLockReturnHomeEnabled()) return@launch

                val alarmLockedAtMs = intent.getLongExtra(EXTRA_LOCKED_AT_MS, 0L)
                val latestLockedAtMs = preferencesManager.getLongLockLastScreenOffAtMs()
                if (alarmLockedAtMs <= 0L || latestLockedAtMs != alarmLockedAtMs) return@launch

                val keyguardManager = appContext.getSystemService(KeyguardManager::class.java)
                val isLocked =
                        keyguardManager?.isDeviceLocked == true ||
                                keyguardManager?.isKeyguardLocked == true
                if (!isLocked) {
                    preferencesManager.clearLongLockLastScreenOffAtMs()
                    return@launch
                }

                Log.d(TAG, "Alarm fired while device is still locked; attempting launcher foreground")
                val (launched, launchErr) = appContext.tryStartLauncherMainActivity()
                if (launched) Log.d(TAG, "Long-lock alarm launched launcher activity")
                else
                    Log.d(
                            TAG,
                            "Long-lock alarm failed to launch launcher: $launchErr",
                    )

                if (launched) {
                    preferencesManager.clearLongLockLastScreenOffAtMs()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FokusLongLockAlarm"
        private const val REQUEST_CODE = 4107
        const val ACTION_RETURN_HOME_AFTER_LONG_LOCK =
                "com.lu4p.fokuslauncher.action.RETURN_HOME_AFTER_LONG_LOCK"
        private const val EXTRA_LOCKED_AT_MS = "extra_locked_at_ms"

        fun createPendingIntent(
                context: Context,
                lockedAtMs: Long? = null,
                createIfMissing: Boolean,
        ): PendingIntent? {
            val flags =
                    PendingIntent.FLAG_IMMUTABLE or
                            if (createIfMissing) {
                                PendingIntent.FLAG_UPDATE_CURRENT
                            } else {
                                PendingIntent.FLAG_NO_CREATE
                            }
            return PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    Intent(context, LongLockAlarmReceiver::class.java).apply {
                        action = ACTION_RETURN_HOME_AFTER_LONG_LOCK
                        if (lockedAtMs != null) putExtra(EXTRA_LOCKED_AT_MS, lockedAtMs)
                    },
                    flags,
            )
        }
    }
}
