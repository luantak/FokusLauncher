package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object FokusBackdrop {
    // Single source of truth for overlay intensity in each mode.
    private const val OverlayStrengthWithBlur = 0.26f
    private const val OverlayStrengthWithoutBlur = 0.50f
    // Dim ratio is still mode-specific because Android's window dim
    // does not visually match Compose scrim 1:1.
    private const val WindowDimScaleWithBlur = 0.23076923f // 0.06 / 0.26
    private const val WindowDimScaleWithoutBlur = 0.64f // 0.32 / 0.50

    val ScrimColorWithBlur: Color = Color.Black.copy(alpha = OverlayStrengthWithBlur)
    val ScrimColorWithoutBlur: Color = Color.Black.copy(alpha = OverlayStrengthWithoutBlur)
    val BlurRadius = 28.dp
    const val WindowBackgroundBlurRadius = 80
    const val WindowBlurBehindRadius = 20
    val WindowDimAmountWithBlur = OverlayStrengthWithBlur * WindowDimScaleWithBlur
    val WindowDimAmountWithoutBlur = OverlayStrengthWithoutBlur * WindowDimScaleWithoutBlur

    fun scrimColor(blurEnabled: Boolean): Color =
        if (blurEnabled) ScrimColorWithBlur else ScrimColorWithoutBlur

    fun windowDimAmount(blurEnabled: Boolean): Float =
        if (blurEnabled) WindowDimAmountWithBlur else WindowDimAmountWithoutBlur
}
