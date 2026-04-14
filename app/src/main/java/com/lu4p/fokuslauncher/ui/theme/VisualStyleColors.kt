package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.ui.graphics.Color
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle

/** Bright primary and muted secondary for neon presets; null for [LauncherVisualStyle.CLASSIC]. */
data class NeonPalette(val primary: Color, val muted: Color)

fun LauncherVisualStyle.neonPalette(): NeonPalette? =
        when (this) {
            LauncherVisualStyle.CLASSIC -> null
            LauncherVisualStyle.NEON_MAGENTA ->
                    NeonPalette(primary = Color(0xFFE070FF), muted = Color(0xFFB092D0))
            LauncherVisualStyle.NEON_LIME ->
                    NeonPalette(primary = Color(0xFF58FF7A), muted = Color(0xFF78C892))
            LauncherVisualStyle.NEON_AMBER ->
                    NeonPalette(primary = Color(0xFFFF8F70), muted = Color(0xFFD9A894))
            LauncherVisualStyle.NEON_PINK ->
                    NeonPalette(primary = Color(0xFFD45080), muted = Color(0xFFA03860))
            LauncherVisualStyle.LAVENDER ->
                    NeonPalette(primary = Color(0xFFC49EE8), muted = Color(0xFF9870C0))
            LauncherVisualStyle.SKY ->
                    NeonPalette(primary = Color(0xFF87CEEB), muted = Color(0xFF6098B8))
            LauncherVisualStyle.SAGE ->
                    NeonPalette(primary = Color(0xFF8FBC8F), muted = Color(0xFF6A9070))
            LauncherVisualStyle.ROSE ->
                    NeonPalette(primary = Color(0xFFFB7185), muted = Color(0xFFD04060))
            LauncherVisualStyle.EMERALD ->
                    NeonPalette(primary = Color(0xFF10B981), muted = Color(0xFF0A8060))
        }

/** Primary accent as shown in settings (Classic = launcher white). */
fun LauncherVisualStyle.settingsPreviewColor(): Color = neonPalette()?.primary ?: White
