package com.lu4p.fokuslauncher.data.model

import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R

/**
 * Controls horizontal placement of home widgets, favourite app labels, and shortcut icons.
 *
 * - [LEFT]: Widgets and labels on the left, shortcut icons on the right (default).
 * - [CENTER]: Widgets, labels, and shortcut icons horizontally centered near the bottom.
 * - [MIDDLE]: Same horizontal centering, with labels and shortcuts in the middle of the screen.
 * - [RIGHT]: Widgets and labels on the right, shortcut icons on the left (swapped).
 */
enum class HomeAlignment(@param:StringRes val labelRes: Int) {
    LEFT(R.string.home_alignment_left),
    CENTER(R.string.home_alignment_center),
    MIDDLE(R.string.home_alignment_middle),
    RIGHT(R.string.home_alignment_right);

    companion object {
        fun fromString(value: String): HomeAlignment =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LEFT
    }
}
