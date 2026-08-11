package com.lu4p.fokuslauncher.ui.home

import android.content.Context
import android.text.format.DateFormat
import com.lu4p.fokuslauncher.data.model.CountdownEvent
import com.lu4p.fokuslauncher.data.model.WorldClockCity
import com.lu4p.fokuslauncher.ui.components.clockDisplayTimeWithoutDayPeriod
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** One row in the home world-clock strip. */
data class WorldClockCityUi(
        val id: String,
        val label: String,
        val timeText: String,
        /** Compact temperature like `18°C`, when world-clock weather is enabled. */
        val weatherText: String? = null,
)

/**
 * Formats [cities] for the current instant using the system 12/24h preference.
 * Invalid zone IDs fall back to GMT (Java's behavior for unknown IDs).
 */
fun formatWorldClockCities(
        context: Context,
        cities: List<WorldClockCity>,
        nowMillis: Long = System.currentTimeMillis(),
): List<WorldClockCityUi> {
    if (cities.isEmpty()) return emptyList()
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)
    return cities.map { city ->
        val zone = TimeZone.getTimeZone(city.timeZoneId)
        timeFormat.timeZone = zone
        val formatted = timeFormat.format(Date(nowMillis))
        WorldClockCityUi(
                id = city.id,
                label = city.label,
                timeText = clockDisplayTimeWithoutDayPeriod(formatted, is24Hour),
        )
    }
}

/** Milliseconds remaining until [targetEpochMillis] (negative if past). */
fun millisUntilCountdown(
        targetEpochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
): Long = targetEpochMillis - nowMillis

/**
 * Compact remaining label for the home countdown chip.
 *
 * - `≥ 7d`: `20d`
 * - `1d .. < 7d`: `3d 4h` (hours only under 7 days)
 * - `10h .. < 1d`: `12h`
 * - `1h .. < 10h`: `2h 15m` (minutes only under 10 hours)
 * - `10m .. < 1h`: `15m`
 * - `< 10m`: `5m 30s` / `45s` (seconds only under 10 minutes)
 */
fun formatCountdownRemaining(
        remainingMillis: Long,
        nowLabel: String,
        pastLabel: String,
): String {
    if (remainingMillis < 0) return pastLabel
    if (remainingMillis < 1_000L) return nowLabel

    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
    val totalHours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val totalDays = TimeUnit.MILLISECONDS.toDays(remainingMillis)

    return when {
        totalDays >= 7L -> "${totalDays}d"
        totalDays >= 1L -> {
            val hours = totalHours % 24
            if (hours > 0L) "${totalDays}d ${hours}h" else "${totalDays}d"
        }
        totalHours >= 10L -> "${totalHours}h"
        totalHours >= 1L -> {
            val minutes = totalMinutes % 60
            if (minutes > 0L) "${totalHours}h ${minutes}m" else "${totalHours}h"
        }
        totalMinutes >= 10L -> "${totalMinutes}m"
        totalMinutes >= 1L -> {
            val seconds = totalSeconds % 60
            if (seconds > 0L) "${totalMinutes}m ${seconds}s" else "${totalMinutes}m"
        }
        else -> "${totalSeconds}s"
    }
}

fun formatCountdownDateTimeLabel(
        context: Context,
        event: CountdownEvent,
        locale: Locale = Locale.getDefault(),
): String {
    val date =
            java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale)
                    .format(Date(event.targetEpochMillis))
    val time = DateFormat.getTimeFormat(context).format(Date(event.targetEpochMillis))
    return "$date · $time"
}

fun localDateTimeToEpochMillis(
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
): Long {
    val cal = Calendar.getInstance(timeZone)
    cal.clear()
    cal.set(year, monthZeroBased, dayOfMonth, hourOfDay, minute, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun epochMillisToLocalParts(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
): CountdownLocalParts {
    val cal = Calendar.getInstance(timeZone)
    cal.timeInMillis = epochMillis
    return CountdownLocalParts(
            year = cal.get(Calendar.YEAR),
            monthZeroBased = cal.get(Calendar.MONTH),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
    )
}

data class CountdownLocalParts(
        val year: Int,
        val monthZeroBased: Int,
        val dayOfMonth: Int,
        val hourOfDay: Int,
        val minute: Int,
) {
    val epochDay: Long
        get() = localDatePartsToEpochDay(year, monthZeroBased, dayOfMonth)
}

fun localDatePartsToEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, monthZeroBased)
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return TimeUnit.MILLISECONDS.toDays(cal.timeInMillis)
}

fun epochDayToYearMonthDay(epochDay: Long): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = TimeUnit.DAYS.toMillis(epochDay)
    return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
    )
}
