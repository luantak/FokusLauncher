package com.lu4p.fokuslauncher.data.model

import kotlin.math.round

/** Launcher-only text size multiplier (stacks with system font scale). */
object LauncherFontScale {
    const val MIN: Float = 0.7f
    const val MAX: Float = 1.5f
    const val STEP: Float = 0.1f
    const val DEFAULT: Float = 1.0f

    /**
     * [androidx.compose.material3.Slider] `steps` for [MIN]..[MAX] with [STEP]: 9 stops → 7 steps.
     */
    const val SLIDER_STEPS: Int =
            ((MAX * 10f).toInt() - (MIN * 10f).toInt()) / (STEP * 10f).toInt() - 1

    fun snapToStep(value: Float): Float {
        if (value.isNaN() || value.isInfinite()) return DEFAULT
        val clamped = value.coerceIn(MIN, MAX)
        return (round(clamped.toDouble() * 10.0) / 10.0).toFloat().coerceIn(MIN, MAX)
    }

    /** Normalizes a stored float from DataStore; invalid or missing uses [DEFAULT]. */
    fun fromStorage(raw: Float?): Float {
        if (raw == null) return DEFAULT
        return snapToStep(raw)
    }
}
