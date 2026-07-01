package com.lu4p.fokuslauncher.usage

import android.app.usage.UsageEvents
import android.os.Build

/**
 * Computes screen-on duration from [UsageEvents.Event.SCREEN_INTERACTIVE] /
 * [UsageEvents.Event.SCREEN_NON_INTERACTIVE] pairs. Unlike summing per-app foreground time,
 * this excludes background playback and other usage while the display is off.
 */
internal fun computeScreenOnTimeMs(
        events: UsageEvents?,
        windowStart: Long,
        windowEnd: Long,
): Long {
    if (events == null || windowEnd <= windowStart) return 0L
    val timeline = buildList {
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            add(event.eventType to event.timeStamp)
        }
    }
    return computeScreenOnTimeMs(timeline, windowStart, windowEnd)
}

internal fun computeScreenOnTimeMs(
        events: List<Pair<Int, Long>>,
        windowStart: Long,
        windowEnd: Long,
): Long {
    if (windowEnd <= windowStart) return 0L
    var screenOnSince: Long? = null
    var totalMs = 0L

    for ((eventType, timeStamp) in events) {
        when (eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                if (screenOnSince == null) {
                    screenOnSince = timeStamp.coerceIn(windowStart, windowEnd)
                }
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.DEVICE_SHUTDOWN -> {
                totalMs += closeOpenScreenOnSegment(screenOnSince, timeStamp, windowStart, windowEnd)
                screenOnSince = null
            }
        }
    }

    screenOnSince?.let {
        totalMs +=
                (windowEnd - it.coerceIn(windowStart, windowEnd)).coerceAtLeast(0L)
    }
    return totalMs.coerceAtLeast(0L)
}

/** Screen-on time from aggregated event stats (API 28+). */
internal fun screenOnTimeFromEventStats(
        eventStats: List<android.app.usage.EventStats>?,
): Long {
    if (eventStats.isNullOrEmpty()) return 0L
    return eventStats
            .asSequence()
            .filter { it.eventType == UsageEvents.Event.SCREEN_INTERACTIVE }
            .sumOf { it.totalTime }
}

internal fun supportsScreenInteractiveEvents(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

private fun closeOpenScreenOnSegment(
        screenOnSince: Long?,
        segmentEnd: Long,
        windowStart: Long,
        windowEnd: Long,
): Long {
    val end = segmentEnd.coerceIn(windowStart, windowEnd)
    val start = (screenOnSince ?: windowStart).coerceIn(windowStart, windowEnd)
    return (end - start).coerceAtLeast(0L)
}
