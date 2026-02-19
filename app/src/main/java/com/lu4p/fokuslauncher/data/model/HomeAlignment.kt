package com.lu4p.fokuslauncher.data.model

/**
 * Controls the alignment of favourite app labels and shortcut icons
 * on the home screen.
 *
 * - [LEFT]: Labels on the left, shortcut icons on the right (default).
 * - [CENTER]: Labels and shortcut icons centered together.
 * - [RIGHT]: Labels on the right, shortcut icons on the left (swapped).
 */
enum class HomeAlignment {
    LEFT,
    CENTER,
    RIGHT;

    val displayName: String
        get() = when (this) {
            LEFT -> "Left"
            CENTER -> "Center"
            RIGHT -> "Right"
        }

    companion object {
        fun fromString(value: String): HomeAlignment =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LEFT
    }
}
