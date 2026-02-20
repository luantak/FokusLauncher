package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.ui.graphics.Color

object FokusBackdrop {
    // Single source of truth for overlay intensity in each mode.
    private const val OVERLAY_STRENGTH_WITH_BLUR = 0.26f
    private const val OVERLAY_STRENGTH_WITHOUT_BLUR = 0.50f
    // Dim ratio is still mode-specific because Android's window dim
    // does not visually match Compose scrim 1:1.
    private const val WINDOW_DIM_SCALE_WITH_BLUR = 0.23076923f // 0.06 / 0.26
    private const val WINDOW_DIM_SCALE_WITHOUT_BLUR = 0.64f // 0.32 / 0.50

    val ScrimColorWithBlur: Color = Color.Black.copy(alpha = OVERLAY_STRENGTH_WITH_BLUR)
    val ScrimColorWithoutBlur: Color = Color.Black.copy(alpha = OVERLAY_STRENGTH_WITHOUT_BLUR)
    const val WINDOW_BACKGROUND_BLUR_RADIUS = 80
    const val WINDOW_BLUR_BEHIND_RADIUS = 20
    const val WINDOW_DIM_AMOUNT_WITH_BLUR = OVERLAY_STRENGTH_WITH_BLUR * WINDOW_DIM_SCALE_WITH_BLUR
    const val WINDOW_DIM_AMOUNT_WITHOUT_BLUR = OVERLAY_STRENGTH_WITHOUT_BLUR * WINDOW_DIM_SCALE_WITHOUT_BLUR

    fun scrimColor(blurEnabled: Boolean): Color =
        if (blurEnabled) ScrimColorWithBlur else ScrimColorWithoutBlur

    fun windowDimAmount(blurEnabled: Boolean): Float =
        if (blurEnabled) WINDOW_DIM_AMOUNT_WITH_BLUR else WINDOW_DIM_AMOUNT_WITHOUT_BLUR
}
