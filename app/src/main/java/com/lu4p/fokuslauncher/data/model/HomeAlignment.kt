package com.lu4p.fokuslauncher.data.model

import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R

/**
 * Controls the alignment of favourite app labels and shortcut icons
 * on the home screen.
 *
 * - [LEFT]: Labels on the left, shortcut icons on the right (default).
 * - [CENTER]: Labels and shortcut icons horizontally centered together near the bottom.
 * - [MIDDLE]: Labels and shortcut icons horizontally centered in the middle of the screen.
 * - [RIGHT]: Labels on the right, shortcut icons on the left (swapped).
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
