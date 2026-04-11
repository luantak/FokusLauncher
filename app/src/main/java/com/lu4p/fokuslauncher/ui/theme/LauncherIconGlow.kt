package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Multi-layer icon halo when [enabled]; [haloColor] is drawn at reduced alpha behind the main
 * vector. See [com.lu4p.fokuslauncher.ui.components.LauncherIcon].
 */
data class LauncherIconGlowSpec(
        val enabled: Boolean,
        val haloColor: Color,
        val outerScale: Float = 1.34f,
        val midScale: Float = 1.17f,
        /** Tight inner bloom just outside the glyph. */
        val innerScale: Float = 1.09f,
        val outerAlpha: Float = 0.68f,
        val midAlpha: Float = 0.88f,
        val innerAlpha: Float = 0.72f,
) {
    companion object {
        val None = LauncherIconGlowSpec(enabled = false, haloColor = Color.Transparent)
    }
}

val LocalLauncherIconGlow = compositionLocalOf { LauncherIconGlowSpec.None }
