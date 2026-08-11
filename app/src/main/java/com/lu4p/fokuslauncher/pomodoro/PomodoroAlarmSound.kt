package com.lu4p.fokuslauncher.pomodoro

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.lu4p.fokuslauncher.R

/** Builds the system ringtone picker for Pomodoro completion alarms. */
fun pomodoroAlarmSoundPickerIntent(existingUri: String?): Intent {
    val current =
            existingUri
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { runCatching { it.toUri() }.getOrNull() }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
    }
}

fun pomodoroAlarmSoundUriFromPickerResult(data: Intent?): String {
    val picked =
            data?.let {
                IntentCompat.getParcelableExtra(
                        it,
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        Uri::class.java,
                )
            }
    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    return when {
        picked == null -> ""
        defaultUri != null && picked == defaultUri -> ""
        else -> picked.toString()
    }
}

fun pomodoroAlarmSoundDisplayName(context: Context, alarmSoundUri: String): String {
    val trimmed = alarmSoundUri.trim()
    if (trimmed.isEmpty()) {
        return context.getString(R.string.settings_pomodoro_alarm_sound_default)
    }
    val uri = runCatching { trimmed.toUri() }.getOrNull() ?: return context.getString(
            R.string.settings_pomodoro_alarm_sound_default,
    )
    val title =
            runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
                    .getOrNull()
                    ?.trim()
                    .orEmpty()
    return title.ifEmpty { context.getString(R.string.settings_pomodoro_alarm_sound_default) }
}
