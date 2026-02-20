package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val FokusColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = LightGray,
    onSecondary = Black,
    background = Transparent,
    onBackground = White,
    surface = Transparent,
    onSurface = White,
    surfaceVariant = DarkGray,
    onSurfaceVariant = LightGray,
    surfaceContainerLowest = Transparent,
    surfaceContainerLow = Transparent,
    surfaceContainer = Transparent,
    surfaceContainerHigh = Transparent,
    surfaceContainerHighest = Transparent,
    surfaceBright = Transparent,
    surfaceDim = Transparent,
    inverseSurface = White,
    inverseOnSurface = Black
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FokusLauncherTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        MaterialTheme(
            colorScheme = FokusColorScheme,
            typography = FokusTypography,
            content = content
        )
    }
}
