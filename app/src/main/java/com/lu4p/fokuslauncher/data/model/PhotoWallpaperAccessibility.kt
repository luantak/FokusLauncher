package com.lu4p.fokuslauncher.data.model

import kotlin.math.round

/**
 * Home text/icon shadow strength on a background image. **0** keeps each widget's built-in contrast
 * treatment; larger values use a stronger black blur radius at draw time.
 */
object PhotoWallpaperOutlineWidthDp {
    const val MIN: Float = 0f
    const val MAX: Float = 100f
    const val DEFAULT: Float = 0f

    /** 0 = continuous [androidx.compose.material3.Slider]. */
    const val SLIDER_STEPS: Int = 0

    fun snapToStep(value: Float): Float {
        if (value.isNaN() || value.isInfinite()) return DEFAULT
        return value.coerceIn(MIN, MAX)
    }

    fun fromStorage(raw: Float?): Float {
        if (raw == null) return DEFAULT
        return snapToStep(raw)
    }
}

/**
 * Multiplier for the black scrim behind the app drawer (over a busy / image wallpaper). 1x matches
 * the built-in default; only values above 1 add more dimming.
 */
object PhotoWallpaperDrawerOverlayIntensity {
    const val MIN: Float = 1f
    const val MAX: Float = 3f
    const val STEP: Float = 0.25f
    const val DEFAULT: Float = 1f

    const val SLIDER_STEPS: Int = ((MAX - MIN) / STEP).toInt() - 1

    fun snapToStep(value: Float): Float {
        if (value.isNaN() || value.isInfinite()) return DEFAULT
        val clamped = value.coerceIn(MIN, MAX)
        val stepsFromMin = round((clamped - MIN) / STEP.toDouble()).toInt()
        return (MIN + stepsFromMin * STEP).coerceIn(MIN, MAX)
    }

    fun fromStorage(raw: Float?): Float {
        if (raw == null) return DEFAULT
        return snapToStep(raw.coerceAtLeast(MIN))
    }
}
