package com.lu4p.fokuslauncher.data.model

import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R

/** Accent colors and optional glow for launcher text and icons. [CLASSIC] matches the original look. */
enum class LauncherVisualStyle(@param:StringRes val labelRes: Int) {
    CLASSIC(R.string.visual_style_classic),
    NEON_MAGENTA(R.string.visual_style_neon_magenta),
    NEON_LIME(R.string.visual_style_neon_lime),
    NEON_AMBER(R.string.visual_style_neon_amber),
    NEON_PINK(R.string.visual_style_neon_pink),
    LAVENDER(R.string.visual_style_lavender),
    SKY(R.string.visual_style_sky),
    SAGE(R.string.visual_style_sage),
    ROSE(R.string.visual_style_rose),
    EMERALD(R.string.visual_style_emerald);

    companion object {
        fun fromString(value: String): LauncherVisualStyle {
            if (value.isBlank()) return CLASSIC
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CLASSIC
        }
    }
}
