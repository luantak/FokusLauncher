package com.lu4p.fokuslauncher.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors

/**
 * Dismisses the completion alarm and starts the next Pomodoro phase when the notification is
 * cleared or its Dismiss action is used.
 */
class PomodoroDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return
        EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        PomodoroEntryPoint::class.java,
                )
                .pomodoroRepository()
                .dismissOvertimeAndStartNext()
    }

    companion object {
        const val ACTION_DISMISS = "com.lu4p.fokuslauncher.action.POMODORO_DISMISS_ALARM"
    }
}
