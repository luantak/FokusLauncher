package com.lu4p.fokuslauncher.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.ui.theme.LauncherIconGlowSpec
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherIconGlow
import com.lu4p.fokuslauncher.ui.theme.launcherIconDp

/**
 * [Icon] with size in **dp** scaled by [com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale] so
 * icons follow the launcher font size setting.
 *
 * @param suppressGlow When true, skips the neon halo (flat icon on a busy background, etc.).
 * @param tint Icon color; when null, uses [LocalContentColor] without glow, or
 *     [MaterialTheme.colorScheme.onSurface] when glow is enabled so popups/menus match the theme.
 *     When glow is on and [tint] is non-null, halo layers use the same color as the glyph so
 *     secondary / variant tints are not ringed with the global accent halo.
 */
@Composable
fun LauncherIcon(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color? = null,
        iconSize: Dp = 24.dp,
        suppressGlow: Boolean = false,
) {
    LauncherIcon(
            painter = rememberVectorPainter(imageVector),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            iconSize = iconSize,
            suppressGlow = suppressGlow,
    )
}

@Composable
fun LauncherIcon(
        painter: Painter,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color? = null,
        iconSize: Dp = 24.dp,
        suppressGlow: Boolean = false,
) {
    val glowSpec = LocalLauncherIconGlow.current
    val glow = if (suppressGlow) LauncherIconGlowSpec.None else glowSpec
    val resolvedTint =
            tint
                    ?: if (glow.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        LocalContentColor.current
                    }
    val haloTint =
            if (!glow.enabled) {
                Color.Transparent
            } else if (tint != null) {
                resolvedTint
            } else {
                glow.haloColor
            }
    val scaledSize = iconSize.launcherIconDp()
    if (!glow.enabled) {
        Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = modifier.size(scaledSize),
                tint = resolvedTint,
        )
        return
    }
    val density = LocalDensity.current
    val fontBoost =
            LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
    // Stronger than earlier subtle bloom so icons visually match the bold text shadow.
    val blurRadiusPx = with(density) { (10f * fontBoost).dp.toPx() }

    Box(modifier = modifier.size(scaledSize), contentAlignment = Alignment.Center) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer {
                                        clip = false
                                        scaleX = 1.04f
                                        scaleY = 1.04f
                                        alpha = 0.28f
                                        renderEffect =
                                                BlurEffect(
                                                        blurRadiusPx * 1.35f,
                                                        blurRadiusPx * 1.35f,
                                                        TileMode.Decal,
                                                )
                                    }
                                    .clearAndSetSemantics {},
                    tint = haloTint,
            )
            Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer {
                                        clip = false
                                        scaleX = 1.12f
                                        scaleY = 1.12f
                                        alpha = 0.5f
                                        renderEffect =
                                                BlurEffect(
                                                        blurRadiusPx * 0.72f,
                                                        blurRadiusPx * 0.72f,
                                                        TileMode.Decal,
                                                )
                                    }
                                    .clearAndSetSemantics {},
                    tint = haloTint,
            )
        } else {
            Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer {
                                        clip = false
                                        scaleX = glow.outerScale
                                        scaleY = glow.outerScale
                                        alpha = glow.outerAlpha
                                    }
                                    .clearAndSetSemantics {},
                    tint = haloTint,
            )
            Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer {
                                        clip = false
                                        scaleX = glow.midScale
                                        scaleY = glow.midScale
                                        alpha = glow.midAlpha
                                    }
                                    .clearAndSetSemantics {},
                    tint = haloTint,
            )
        }
        Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                tint = resolvedTint,
        )
    }
}
