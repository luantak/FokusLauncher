package com.lu4p.fokuslauncher.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires when a scheduled Pomodoro session reaches its end time, including if the process was
 * killed. Enters overtime (negative countdown) and announces completion.
 */
class PomodoroAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_POMODORO_COMPLETE) return

        Log.d(TAG, "Pomodoro completion alarm fired")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val preferencesManager = PreferencesManager(appContext)
                val runtime = preferencesManager.getPomodoroRuntime()
                if (runtime.phase != PomodoroPhase.RUNNING) return@launch

                val now = System.currentTimeMillis()
                // Allow a small skew so early deliveries still complete the session.
                if (runtime.endsAtEpochMs > now + EARLY_SLACK_MS) return@launch

                val config = preferencesManager.getPomodoroConfig()
                preferencesManager.setPomodoroRuntime(runtime.copy(phase = PomodoroPhase.OVERTIME))
                PomodoroAlarmScheduler(appContext).cancel()
                EntryPointAccessors.fromApplication(appContext, PomodoroEntryPoint::class.java)
                        .pomodoroCompletionAlerter()
                        .announceComplete(runtime.mode, config.alarmSoundUri)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FokusPomodoroAlarm"
        private const val EARLY_SLACK_MS = 1_500L
        const val ACTION_POMODORO_COMPLETE =
                "com.lu4p.fokuslauncher.action.POMODORO_TIMER_COMPLETE"
    }
}
