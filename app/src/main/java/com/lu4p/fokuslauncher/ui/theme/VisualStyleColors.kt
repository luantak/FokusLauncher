package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.ui.graphics.Color
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle

/** Bright primary and muted secondary for neon presets; null for [LauncherVisualStyle.CLASSIC]. */
data class NeonPalette(val primary: Color, val muted: Color)

fun LauncherVisualStyle.neonPalette(): NeonPalette? =
        when (this) {
            LauncherVisualStyle.CLASSIC -> null
            LauncherVisualStyle.NEON_CYAN ->
                    NeonPalette(primary = Color(0xFF00FFF0), muted = Color(0xFF5EC8C3))
            LauncherVisualStyle.NEON_MAGENTA ->
                    NeonPalette(primary = Color(0xFFE070FF), muted = Color(0xFFB092D0))
            LauncherVisualStyle.NEON_LIME ->
                    NeonPalette(primary = Color(0xFF58FF7A), muted = Color(0xFF78C892))
            LauncherVisualStyle.NEON_AMBER ->
                    NeonPalette(primary = Color(0xFFFF8F70), muted = Color(0xFFD9A894))
            LauncherVisualStyle.NEON_PINK ->
                    NeonPalette(primary = Color(0xFFFF4D9A), muted = Color(0xFFD088B0))
        }

/** Primary accent as shown in settings (Classic = launcher white). */
fun LauncherVisualStyle.settingsPreviewColor(): Color = neonPalette()?.primary ?: White
