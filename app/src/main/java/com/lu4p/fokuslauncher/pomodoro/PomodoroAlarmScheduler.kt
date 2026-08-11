package com.lu4p.fokuslauncher.pomodoro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules / cancels the RTC wakeup that fires when a Pomodoro session should complete. */
@Singleton
class PomodoroAlarmScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun schedule(endsAtEpochMs: Long) {
        if (endsAtEpochMs <= 0L) {
            cancel()
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = createPendingIntent(createIfMissing = true) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtEpochMs, pendingIntent)
    }

    fun cancel() {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = createPendingIntent(createIfMissing = false) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(createIfMissing: Boolean): PendingIntent? {
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
                Intent(context, PomodoroAlarmReceiver::class.java).apply {
                    action = PomodoroAlarmReceiver.ACTION_POMODORO_COMPLETE
                },
                flags,
        )
    }

    companion object {
        private const val REQUEST_CODE = 4217
    }
}
