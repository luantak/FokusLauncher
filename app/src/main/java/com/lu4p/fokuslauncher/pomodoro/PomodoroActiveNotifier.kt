package com.lu4p.fokuslauncher.pomodoro

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lu4p.fokuslauncher.MainActivity
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Ongoing status notification while a Pomodoro session is running (shown on the lock screen). */
@Singleton
class PomodoroActiveNotifier
@Inject
constructor(@param:ApplicationContext private val context: Context) {

    @SuppressLint("MissingPermission") // Gated by [PomodoroCompletionAlerter.canPostNotifications].
    fun showRunning(mode: PomodoroMode, endsAtEpochMs: Long) {
        if (!PomodoroCompletionAlerter.canPostNotifications(context)) return
        ensureChannel()
        val modeLabel =
                when (mode) {
                    PomodoroMode.FOCUS -> context.getString(R.string.pomodoro_mode_focus)
                    PomodoroMode.BREAK -> context.getString(R.string.pomodoro_mode_break)
                }
        val title = context.getString(R.string.pomodoro_notification_active_title, modeLabel)
        val openApp =
                PendingIntent.getActivity(
                        context,
                        REQUEST_OPEN,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
        val pause =
                PendingIntent.getBroadcast(
                        context,
                        REQUEST_PAUSE,
                        Intent(context, PomodoroActionReceiver::class.java).apply {
                            action = PomodoroActionReceiver.ACTION_PAUSE
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(context.getString(R.string.pomodoro_notification_active_body))
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setUsesChronometer(true)
                        .setChronometerCountDown(true)
                        .setWhen(endsAtEpochMs)
                        .setShowWhen(true)
                        .setContentIntent(openApp)
                        .addAction(
                                0,
                                context.getString(R.string.pomodoro_pause),
                                pause,
                        )
                        .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // New id so devices that created the old LOW/silent channel pick up lock-screen visibility.
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
                NotificationChannel(
                                CHANNEL_ID,
                                context.getString(R.string.pomodoro_notification_active_channel_name),
                                NotificationManager.IMPORTANCE_DEFAULT,
                        )
                        .apply {
                            description =
                                    context.getString(
                                            R.string.pomodoro_notification_active_channel_description,
                                    )
                            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                            setShowBadge(true)
                        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_active_v2"
        const val NOTIFICATION_ID = 4217
        private const val REQUEST_OPEN = 42170
        private const val REQUEST_PAUSE = 42171
    }
}
