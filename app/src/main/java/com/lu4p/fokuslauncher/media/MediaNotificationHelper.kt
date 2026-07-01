package com.lu4p.fokuslauncher.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object MediaNotificationHelper {

    fun componentName(context: Context): ComponentName =
            ComponentName(context, MediaNotificationListenerService::class.java)

    fun isListenerEnabled(context: Context): Boolean =
            context.packageName in
                    NotificationManagerCompat.getEnabledListenerPackages(context)

    fun openListenerSettings(context: Context) {
        context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
