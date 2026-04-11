package com.lu4p.fokuslauncher.data.model

/** Visual style and glow, always resolved from the same preference snapshot. */
data class LauncherAppearance(
        val visualStyle: LauncherVisualStyle,
        val glowEnabled: Boolean,
)
