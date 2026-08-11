package com.lu4p.fokuslauncher.data.model

import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R
import org.json.JSONArray
import org.json.JSONObject

/**
 * One slot in the ordered home extras row (Configure widgets).
 * Each world-clock city and each countdown are separate reorderable items.
 */
sealed class HomeExtraWidgetEntry {
    data class WorldClock(val cityId: String) : HomeExtraWidgetEntry()
    data class Countdown(val eventId: String) : HomeExtraWidgetEntry()

    val stableKey: String
        get() =
                when (this) {
                    is WorldClock -> "world_clock:$cityId"
                    is Countdown -> "countdown:$eventId"
                }
}

/** Types offered by “Add other widget”. */
enum class HomeExtraWidgetAddType(@param:StringRes val labelRes: Int) {
    WORLD_CLOCK(R.string.settings_world_clock_cities),
    COUNTDOWN(R.string.settings_countdown_event),
}

private const val TYPE_WORLD_CLOCK = "WORLD_CLOCK"
private const val TYPE_COUNTDOWN = "COUNTDOWN"
private const val KEY_TYPE = "type"
private const val KEY_CITY_ID = "cityId"
private const val KEY_EVENT_ID = "eventId"

fun serializeHomeExtraWidgets(entries: List<HomeExtraWidgetEntry>): String {
    if (entries.isEmpty()) return ""
    val array = JSONArray()
    val seen = mutableSetOf<String>()
    entries.forEach { entry ->
        if (!seen.add(entry.stableKey)) return@forEach
        when (entry) {
            is HomeExtraWidgetEntry.WorldClock -> {
                if (entry.cityId.isBlank()) return@forEach
                array.put(
                        JSONObject()
                                .put(KEY_TYPE, TYPE_WORLD_CLOCK)
                                .put(KEY_CITY_ID, entry.cityId),
                )
            }
            is HomeExtraWidgetEntry.Countdown -> {
                if (entry.eventId.isBlank()) return@forEach
                array.put(
                        JSONObject()
                                .put(KEY_TYPE, TYPE_COUNTDOWN)
                                .put(KEY_EVENT_ID, entry.eventId),
                )
            }
        }
    }
    return array.toString()
}

/**
 * Parses the ordered extras list.
 *
 * Supports the current object format and a legacy string array of kinds
 * (`WORLD_CLOCK` / `COUNTDOWN`). Legacy kinds expand via [legacyWorldClockCityIds] /
 * [legacyCountdownEventIds]. Bare `COUNTDOWN` objects without [KEY_EVENT_ID] also expand
 * via [legacyCountdownEventIds].
 */
fun parseHomeExtraWidgets(
        raw: String,
        legacyWorldClockCityIds: List<String> = emptyList(),
        legacyCountdownEventIds: List<String> = emptyList(),
): List<HomeExtraWidgetEntry> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            val seen = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val entry =
                        when (val item = array.opt(i)) {
                            is JSONObject ->
                                    parseEntryObject(
                                            item,
                                            legacyCountdownEventIds,
                                    )
                            is String ->
                                    parseLegacyKind(
                                            item,
                                            legacyWorldClockCityIds,
                                            legacyCountdownEventIds,
                                    )
                            else -> null
                        }
                entry?.forEach { e ->
                    if (seen.add(e.stableKey)) add(e)
                }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseEntryObject(
        obj: JSONObject,
        legacyCountdownEventIds: List<String>,
): List<HomeExtraWidgetEntry>? {
    return when (obj.optString(KEY_TYPE).trim().uppercase()) {
        TYPE_WORLD_CLOCK -> {
            val cityId = obj.optString(KEY_CITY_ID).trim()
            if (cityId.isEmpty()) null
            else listOf(HomeExtraWidgetEntry.WorldClock(cityId))
        }
        TYPE_COUNTDOWN -> {
            val eventId = obj.optString(KEY_EVENT_ID).trim()
            if (eventId.isNotEmpty()) {
                listOf(HomeExtraWidgetEntry.Countdown(eventId))
            } else {
                legacyCountdownEventIds
                        .filter { it.isNotBlank() }
                        .map { HomeExtraWidgetEntry.Countdown(it) }
            }
        }
        else -> null
    }
}

/** Legacy: whole-widget kinds in a string array. */
private fun parseLegacyKind(
        raw: String,
        legacyWorldClockCityIds: List<String>,
        legacyCountdownEventIds: List<String>,
): List<HomeExtraWidgetEntry>? {
    return when (raw.trim().uppercase()) {
        TYPE_WORLD_CLOCK ->
                legacyWorldClockCityIds
                        .filter { it.isNotBlank() }
                        .map { HomeExtraWidgetEntry.WorldClock(it) }
        TYPE_COUNTDOWN ->
                legacyCountdownEventIds
                        .filter { it.isNotBlank() }
                        .map { HomeExtraWidgetEntry.Countdown(it) }
        else -> null
    }
}

fun moveHomeExtraWidget(
        entries: List<HomeExtraWidgetEntry>,
        from: Int,
        to: Int,
): List<HomeExtraWidgetEntry> {
    if (from == to) return entries
    if (from !in entries.indices || to !in entries.indices) return entries
    val mutable = entries.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    return mutable
}

fun homeExtraWorldClockCount(entries: List<HomeExtraWidgetEntry>): Int =
        entries.count { it is HomeExtraWidgetEntry.WorldClock }
