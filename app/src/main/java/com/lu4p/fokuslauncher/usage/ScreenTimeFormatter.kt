package com.lu4p.fokuslauncher.usage

/** Formats a foreground duration for the home screen time widget (e.g. "4h 32m"). */
fun formatScreenTimeDuration(totalMs: Long): String {
    if (totalMs <= 0L) return "0m"
    val totalMinutes = totalMs / 60_000L
    if (totalMinutes <= 0L) return "< 1m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
