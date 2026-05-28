package com.lu4p.fokuslauncher.data.model

/**
 * Shortcut action bound to a home widget tap (clock, date, or weather).
 * Persisted as [encodeWidgetTapTarget] / [decodeWidgetTapTarget].
 */
data class WidgetTapTarget(
        val target: ShortcutTarget,
        val profileKey: String = "0",
)

fun encodeWidgetTapTarget(binding: WidgetTapTarget?): String {
    if (binding == null) return ""
    return "${ShortcutTarget.encode(binding.target)};${binding.profileKey.ifBlank { "0" }}"
}

fun decodeWidgetTapTarget(raw: String): WidgetTapTarget? {
    if (raw.isBlank()) return null
    return if (raw.contains(';')) {
        val parts = raw.split(';', limit = 2)
        val target = ShortcutTarget.decode(parts[0]) ?: return null
        WidgetTapTarget(target, parts.getOrElse(1) { "0" }.ifBlank { "0" })
    } else {
        ShortcutTarget.decode(raw)?.let { WidgetTapTarget(it, "0") }
    }
}
