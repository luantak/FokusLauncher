package com.lu4p.fokuslauncher.pomodoro

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows a clear completion notification and plays the alarm ringtone together.
 *
 * Dismiss always cancels the notification before stopping the ring so the user is not left with
 * unexplained ringing.
 */
@Singleton
class PomodoroCompletionAlerter
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val alarmPlayer: PomodoroAlarmPlayer,
        private val activeNotifier: PomodoroActiveNotifier,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val safetyStopRingRunnable = Runnable { alarmPlayer.stop() }
    @Volatile private var alerting: Boolean = false

    fun announceComplete(mode: PomodoroMode, alarmSoundUri: String = "") {
        activeNotifier.cancel()
        ensureChannel()
        // Never ring without a clear, dismissible notification explaining what finished.
        if (!showNotification(mode)) return
        alerting = true
        alarmPlayer.play(alarmSoundUri)
        mainHandler.removeCallbacks(safetyStopRingRunnable)
        mainHandler.postDelayed(safetyStopRingRunnable, SAFETY_STOP_RING_MS)
    }

    /** Re-show / re-ring after process death while still in overtime. */
    fun ensureAlerting(mode: PomodoroMode, alarmSoundUri: String = "") {
        if (alerting) {
            showNotification(mode)
            return
        }
        announceComplete(mode, alarmSoundUri)
    }

    /** Cancel the notification first, then stop the ringtone. */
    fun dismiss() {
        mainHandler.removeCallbacks(safetyStopRingRunnable)
        alerting = false
        cancelNotification()
        alarmPlayer.stop()
    }

    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** @return true when a completion notification was posted. */
    @SuppressLint("MissingPermission") // Gated by [canPostNotifications] above.
    private fun showNotification(mode: PomodoroMode): Boolean {
        if (!canPostNotifications(context)) return false

        val fokusLabel = context.getString(R.string.pomodoro_mode_focus)
        val title =
                when (mode) {
                    PomodoroMode.FOCUS ->
                            context.getString(R.string.pomodoro_notification_focus_complete, fokusLabel)
                    PomodoroMode.BREAK ->
                            context.getString(R.string.pomodoro_notification_break_complete)
                }
        val body = context.getString(R.string.pomodoro_notification_dismiss_body)
        val dismissPending = dismissPendingIntent()
        val contentPending = dismissPendingIntent(requestCode = REQUEST_CONTENT)

        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setSilent(true) // ringtone is played separately on the alarm stream
                        .setContentIntent(contentPending)
                        .setDeleteIntent(dismissPending)
                        .addAction(
                                0,
                                context.getString(R.string.pomodoro_notification_dismiss_action),
                                dismissPendingIntent(requestCode = REQUEST_ACTION),
                        )
                        .build()

        return runCatching {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                    true
                }
                .getOrDefault(false)
    }

    private fun dismissPendingIntent(requestCode: Int = REQUEST_DELETE): PendingIntent {
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, PomodoroDismissReceiver::class.java).apply {
                    action = PomodoroDismissReceiver.ACTION_DISMISS
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel =
                NotificationChannel(
                                CHANNEL_ID,
                                context.getString(R.string.pomodoro_notification_channel_name),
                                NotificationManager.IMPORTANCE_HIGH,
                        )
                        .apply {
                            description =
                                    context.getString(
                                            R.string.pomodoro_notification_channel_description,
                                            context.getString(R.string.pomodoro_mode_focus),
                                    )
                            setSound(null, null)
                            enableVibration(true)
                            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_completion"
        const val NOTIFICATION_ID = 4218
        private const val REQUEST_DELETE = 42180
        private const val REQUEST_ACTION = 42181
        private const val REQUEST_CONTENT = 42182
        /** Stop ringing eventually; overtime UI stays until the user dismisses. */
        private const val SAFETY_STOP_RING_MS = 10 * 60_000L

        fun canPostNotifications(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted =
                        ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return false
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
