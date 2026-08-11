package com.lu4p.fokuslauncher.data.model

import android.icu.text.TimeZoneNames
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** A city shown in the home world-clock strip. */
data class WorldClockCity(
        val id: String,
        val label: String,
        val timeZoneId: String,
        val position: Int,
)

/**
 * One entry in the timezone picker.
 *
 * [displayName] is a human city/region label (ICU exemplar when available); [timeZoneId] stays the
 * IANA id used for formatting times.
 */
data class TimeZonePickerEntry(
        val timeZoneId: String,
        val displayName: String,
        val utcOffsetLabel: String,
        val rawOffsetMillis: Int,
)

const val WORLD_CLOCK_LABEL_MAX_LENGTH = 24

/** Reindexes [position] after sorting; no count cap. */
fun clampWorldClockCities(cities: List<WorldClockCity>): List<WorldClockCity> =
        cities
                .sortedBy { it.position }
                .mapIndexed { index, city -> city.copy(position = index) }

fun serializeWorldClockCities(cities: List<WorldClockCity>): String {
    val clamped = clampWorldClockCities(cities)
    if (clamped.isEmpty()) return ""
    val array = JSONArray()
    clamped.forEach { city ->
        array.put(
                JSONObject()
                        .put("id", city.id)
                        .put("label", city.label)
                        .put("timeZoneId", city.timeZoneId)
                        .put("position", city.position)
        )
    }
    return array.toString()
}

fun parseWorldClockCities(raw: String): List<WorldClockCity> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("id").trim()
                        val label =
                                item.optString("label")
                                        .trim()
                                        .take(WORLD_CLOCK_LABEL_MAX_LENGTH)
                        val timeZoneId = item.optString("timeZoneId").trim()
                        if (id.isBlank() || label.isBlank() || timeZoneId.isBlank()) continue
                        add(
                                WorldClockCity(
                                        id = id,
                                        label = label,
                                        timeZoneId = timeZoneId,
                                        position = item.optInt("position", i),
                                )
                        )
                    }
                }
                .let(::clampWorldClockCities)
    } catch (_: Exception) {
        emptyList()
    }
}

/** Formats a zone's current offset as `UTC`, `UTC+02:00`, or `UTC-05:30`. */
fun formatUtcOffsetLabel(
        timeZoneId: String,
        nowMillis: Long = System.currentTimeMillis(),
): String {
    val zone = TimeZone.getTimeZone(timeZoneId)
    val totalMillis = zone.getOffset(nowMillis)
    if (totalMillis == 0) return "UTC"
    val sign = if (totalMillis >= 0) '+' else '-'
    val absMillis = abs(totalMillis.toLong())
    val hours = TimeUnit.MILLISECONDS.toHours(absMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(absMillis) % 60
    return String.format(Locale.US, "UTC%c%02d:%02d", sign, hours, minutes)
}

/**
 * All system timezone IDs, sorted by current UTC offset then display name.
 * Prefer region/city IDs (`Continent/City`); keep short ids like `UTC` / `GMT`.
 */
fun listSystemTimeZonePickerEntries(
        nowMillis: Long = System.currentTimeMillis(),
        locale: Locale = Locale.getDefault(),
): List<TimeZonePickerEntry> {
    val names = timeZoneNamesOrNull(locale)
    return TimeZone.getAvailableIDs()
            .asSequence()
            .filter { id ->
                id == "UTC" ||
                        id == "GMT" ||
                        id.contains('/') &&
                                !id.startsWith("SystemV/") &&
                                !id.startsWith("Etc/")
            }
            .distinct()
            .map { id ->
                val offset = TimeZone.getTimeZone(id).getOffset(nowMillis)
                TimeZonePickerEntry(
                        timeZoneId = id,
                        displayName = displayNameForTimeZoneId(id, names),
                        utcOffsetLabel = formatUtcOffsetLabel(id, nowMillis),
                        rawOffsetMillis = offset,
                )
            }
            .sortedWith(
                    compareBy<TimeZonePickerEntry> { it.rawOffsetMillis }
                            .thenBy { it.displayName.lowercase(locale) }
                            .thenBy { it.timeZoneId.lowercase(Locale.US) }
            )
            .toList()
}

/**
 * Human-readable city/region for an IANA id.
 *
 * Prefers ICU exemplar location names (`America/New_York` → localized "New York") over
 * [TimeZone.getDisplayName] ("Eastern Daylight Time"), which is ambiguous across cities.
 */
fun displayNameForTimeZoneId(
        timeZoneId: String,
        locale: Locale = Locale.getDefault(),
): String = displayNameForTimeZoneId(timeZoneId, timeZoneNamesOrNull(locale))

/** Default home-widget label from a zone id (same as picker display name). */
fun defaultLabelForTimeZoneId(timeZoneId: String, locale: Locale = Locale.getDefault()): String =
        displayNameForTimeZoneId(timeZoneId, locale)

private fun displayNameForTimeZoneId(timeZoneId: String, names: TimeZoneNames?): String {
    val trimmed = timeZoneId.trim()
    if (trimmed.isEmpty()) return trimmed
    val exemplar = names?.getExemplarLocationName(trimmed)?.trim().orEmpty()
    if (exemplar.isNotEmpty()) return exemplar
    return ianaLeafLabel(trimmed)
}

/** `Europe/Berlin` → `Berlin`; `UTC` → `UTC`. */
fun ianaLeafLabel(timeZoneId: String): String {
    val trimmed = timeZoneId.trim()
    if (trimmed.isEmpty()) return trimmed
    val leaf = trimmed.substringAfterLast('/').ifBlank { trimmed }
    return leaf.replace('_', ' ')
}

private fun timeZoneNamesOrNull(locale: Locale): TimeZoneNames? =
        try {
            TimeZoneNames.getInstance(locale)
        } catch (_: Exception) {
            null
        }

fun filterTimeZonePickerEntries(
        entries: List<TimeZonePickerEntry>,
        query: String,
): List<TimeZonePickerEntry> {
    val q = query.trim()
    if (q.isEmpty()) return entries
    val lower = q.lowercase(Locale.US)
    return entries.filter { entry ->
        entry.displayName.lowercase(Locale.US).contains(lower) ||
                entry.timeZoneId.lowercase(Locale.US).contains(lower) ||
                entry.utcOffsetLabel.lowercase(Locale.US).contains(lower) ||
                ianaLeafLabel(entry.timeZoneId).lowercase(Locale.US).contains(lower)
    }
}
