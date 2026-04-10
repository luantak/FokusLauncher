package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.ui.util.ProvideSystemClickSound

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
fun FokusLauncherTheme(
        fontFamily: FontFamily = FontFamily.Default,
        fontScale: Float = 1f,
        content: @Composable () -> Unit
) {
    val typography =
            remember(fontFamily, fontScale) { fokusTypographyForLauncher(fontFamily, fontScale) }
    val resolvedIconScale =
            remember(fontScale) {
                fontScale.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
            }
    CompositionLocalProvider(
            LocalOverscrollFactory provides null,
            LocalLauncherFontScale provides resolvedIconScale,
    ) {
        ProvideSystemClickSound {
            MaterialTheme(colorScheme = FokusColorScheme, typography = typography, content = content)
        }
    }
}
