package com.lu4p.fokuslauncher.pomodoro

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Plays the configured (or system default) alarm ringtone until [stop] is called. */
@Singleton
class PomodoroAlarmPlayer @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null

    /**
     * @param alarmSoundUri empty/null for the system default alarm; otherwise a ringtone URI
     * string from [android.media.RingtoneManager].
     */
    fun play(alarmSoundUri: String? = null) {
        mainHandler.post {
            stopInternal()
            val uri = resolveAlarmUri(alarmSoundUri) ?: return@post
            val tone =
                    runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull()
                            ?: return@post
            tone.audioAttributes =
                    AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tone.isLooping = true
            }
            ringtone = tone
            runCatching { tone.play() }
        }
    }

    fun stop() {
        mainHandler.post { stopInternal() }
    }

    private fun stopInternal() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    private fun resolveAlarmUri(alarmSoundUri: String?): Uri? {
        val custom =
                alarmSoundUri
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { runCatching { it.toUri() }.getOrNull() }
        if (custom != null) return custom
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
}
