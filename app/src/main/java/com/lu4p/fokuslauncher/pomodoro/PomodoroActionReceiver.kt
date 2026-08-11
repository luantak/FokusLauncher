package com.lu4p.fokuslauncher.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors

/** Handles actions from the ongoing Pomodoro notification (e.g. Pause). */
class PomodoroActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PAUSE) return
        EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        PomodoroEntryPoint::class.java,
                )
                .pomodoroRepository()
                .togglePlayPause()
    }

    companion object {
        const val ACTION_PAUSE = "com.lu4p.fokuslauncher.action.POMODORO_PAUSE"
    }
}
