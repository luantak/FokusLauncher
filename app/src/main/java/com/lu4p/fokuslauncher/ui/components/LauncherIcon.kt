package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.theme.LauncherIconGlowSpec
import com.lu4p.fokuslauncher.ui.theme.LocalLauncherIconGlow
import com.lu4p.fokuslauncher.ui.theme.launcherIconDp

/**
 * [Icon] with size in **dp** scaled by [com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale] so
 * icons follow the launcher font size setting.
 *
 * @param suppressGlow When true, skips the neon halo (e.g. destructive / error-colored icons).
 * @param tint Icon color; when null, uses [LocalContentColor] without glow, or
 *     [MaterialTheme.colorScheme.onSurface] when glow is enabled so popups/menus match the theme.
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
    val glowSpec = LocalLauncherIconGlow.current
    val glow = if (suppressGlow) LauncherIconGlowSpec.None else glowSpec
    val resolvedTint =
            tint
                    ?: if (glow.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        LocalContentColor.current
                    }
    val painter = rememberVectorPainter(imageVector)
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
    Box(modifier = modifier.size(scaledSize), contentAlignment = Alignment.Center) {
        Icon(
                painter = painter,
                contentDescription = null,
                modifier =
                        Modifier.fillMaxSize()
                                .graphicsLayer {
                                    scaleX = glow.outerScale
                                    scaleY = glow.outerScale
                                    alpha = glow.outerAlpha
                                }
                                .clearAndSetSemantics {},
                tint = glow.haloColor,
        )
        Icon(
                painter = painter,
                contentDescription = null,
                modifier =
                        Modifier.fillMaxSize()
                                .graphicsLayer {
                                    scaleX = glow.midScale
                                    scaleY = glow.midScale
                                    alpha = glow.midAlpha
                                }
                                .clearAndSetSemantics {},
                tint = glow.haloColor,
        )
        Icon(
                painter = painter,
                contentDescription = null,
                modifier =
                        Modifier.fillMaxSize()
                                .graphicsLayer {
                                    scaleX = glow.innerScale
                                    scaleY = glow.innerScale
                                    alpha = glow.innerAlpha
                                }
                                .clearAndSetSemantics {},
                tint = glow.haloColor,
        )
        Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                tint = resolvedTint,
        )
    }
}
