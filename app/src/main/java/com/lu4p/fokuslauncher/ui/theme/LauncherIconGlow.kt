package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Icon bloom when [enabled]. On API 31+ [LauncherIcon] uses a blurred underlay (like text shadow).
 * On older APIs it stacks two scaled copies using [outerScale]/[midScale] and alphas below.
 */
data class LauncherIconGlowSpec(
        val enabled: Boolean,
        val haloColor: Color,
        val outerScale: Float = 1.22f,
        val midScale: Float = 1.11f,
        val outerAlpha: Float = 0.38f,
        val midAlpha: Float = 0.58f,
) {
    companion object {
        val None = LauncherIconGlowSpec(enabled = false, haloColor = Color.Transparent)
    }
}

val LocalLauncherIconGlow = compositionLocalOf { LauncherIconGlowSpec.None }
