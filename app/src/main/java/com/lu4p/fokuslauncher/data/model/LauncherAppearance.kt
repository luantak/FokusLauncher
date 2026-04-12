package com.lu4p.fokuslauncher.data.model

/**
 * Visual style and glow, always resolved from the same preference snapshot.
 *
 * [usesPhotoWallpaper] is true when the user keeps or sets an image wallpaper (vs solid black from
 * this app). The launcher then disables glow and uses classic white/gray text for readability.
 */
data class LauncherAppearance(
        val visualStyle: LauncherVisualStyle,
        val glowEnabled: Boolean,
        val usesPhotoWallpaper: Boolean = false,
)
