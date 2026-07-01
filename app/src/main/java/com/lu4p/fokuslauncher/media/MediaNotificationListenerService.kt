package com.lu4p.fokuslauncher.media

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Lets [MediaRepository] read active media sessions (including Spotify) via
 * [android.media.session.MediaSessionManager]. The user must grant notification access in system
 * settings.
 */
@AndroidEntryPoint
class MediaNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var mediaRepository: MediaRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaRepository.onNotificationListenerConnected(
                ComponentName(this, MediaNotificationListenerService::class.java)
        )
    }

    override fun onListenerDisconnected() {
        mediaRepository.onNotificationListenerDisconnected()
        super.onListenerDisconnected()
    }
}
