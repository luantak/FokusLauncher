package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.LauncherFontScale

/**
 * Same multiplier as launcher typography ([FokusLauncherTheme] `fontScale`), for scaling vector
 * icon sizes in **dp** so icons track the user's font size setting.
 */
val LocalLauncherFontScale = compositionLocalOf { LauncherFontScale.DEFAULT }

/**
 * Uniform black shadow strength for home text and icons on busy backgrounds. **0** = use each call
 * site's built-in contrast treatment; home sets this from preferences when an image wallpaper is
 * active.
 */
val LocalPhotoWallpaperOutlineWidthDp = compositionLocalOf { 0f }

@Composable
@ReadOnlyComposable
fun Dp.launcherIconDp(): Dp {
    val s = LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
    return (this.value * s).dp
}
