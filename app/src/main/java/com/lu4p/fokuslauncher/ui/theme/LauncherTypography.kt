package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle

/**
 * Resolves a stored font family name to a Compose [FontFamily]. Empty or blank uses the platform
 * default. Names match device config / [android.graphics.Typeface.create] (e.g. sans-serif, Roboto).
 */
fun composeFontFamilyFromStoredName(storedName: String?): FontFamily {
    val name = storedName?.trim().orEmpty()
    if (name.isEmpty()) return FontFamily.Default
    val dn = DeviceFontFamilyName(name)
    return FontFamily(
            Font(dn, FontWeight.Thin, FontStyle.Normal),
            Font(dn, FontWeight.ExtraLight, FontStyle.Normal),
            Font(dn, FontWeight.Light, FontStyle.Normal),
            Font(dn, FontWeight.Normal, FontStyle.Normal),
            Font(dn, FontWeight.Medium, FontStyle.Normal),
            Font(dn, FontWeight.SemiBold, FontStyle.Normal),
            Font(dn, FontWeight.Bold, FontStyle.Normal),
            Font(dn, FontWeight.Normal, FontStyle.Italic),
            Font(dn, FontWeight.Medium, FontStyle.Italic),
    )
}

private fun TextStyle.withLauncherFont(fontFamily: FontFamily): TextStyle = copy(fontFamily = fontFamily)

/** Clears the soft text shadow applied when launcher glow is enabled. */
fun TextStyle.withoutLauncherTextGlow(): TextStyle =
        copy(shadow = Shadow(color = Color.Transparent, offset = Offset.Zero, blurRadius = 0f))

/**
 * Keeps blur/offset from an existing launcher glow [shadow] but tints it [glowColor] (e.g. error red).
 * No-ops when there is no shadow or blur is zero.
 */
fun TextStyle.withLauncherTextGlowRecolored(glowColor: Color): TextStyle {
    val s = shadow ?: return this
    if (s.blurRadius <= 0f) return this
    return copy(shadow = s.copy(color = glowColor.copy(alpha = 1f)))
}

private fun TextStyle.withLauncherFontScale(scale: Float): TextStyle {
    if (scale == 1f) return this
    fun scaled(u: TextUnit): TextUnit =
            when {
                u == TextUnit.Unspecified -> u
                u.type == TextUnitType.Sp -> (u.value * scale).sp
                u.type == TextUnitType.Em -> (u.value * scale).em
                else -> u
            }
    return copy(fontSize = scaled(fontSize), lineHeight = scaled(lineHeight))
}

/**
 * Applies a system [FontFamily] and an optional launcher-only size multiplier to the typography
 * scale. The multiplier stacks with Android system font scale because sizes remain in [sp].
 *
 * @param glowEnabled When true, draws a soft shadow on text (independent of [visualStyle]).
 */
fun fokusTypographyForLauncher(
        fontFamily: FontFamily,
        fontScale: Float = 1f,
        visualStyle: LauncherVisualStyle = LauncherVisualStyle.CLASSIC,
        glowEnabled: Boolean = false,
): Typography {
    val base = FokusTypography
    val palette = visualStyle.neonPalette()
    val homeClockScale = fontScale.coerceAtMost(LauncherFontScale.CLOCK_MAX)
    fun tune(style: TextStyle, scale: Float = fontScale): TextStyle {
        var t = style.withLauncherFontScale(scale).withLauncherFont(fontFamily)
        val isMuted = style.color == LightGray
        if (palette != null) {
            t =
                    t.copy(
                            color = if (isMuted) palette.muted else palette.primary,
                    )
        }
        if (glowEnabled) {
            val glowSource =
                    when {
                        palette != null && isMuted -> palette.muted
                        palette != null -> palette.primary
                        isMuted -> LightGray
                        else -> White
                    }
            val shadowColor = glowSource.copy(alpha = 1f)
            val blurBoost = scale.coerceIn(0.85f, 1.45f)
            val blur =
                    (when {
                        isMuted -> 28f
                        style.fontSize == 66.sp && style.lineHeight == 74.sp -> 62f
                        style.fontSize == 24.sp && style.lineHeight == 36.sp -> 44f
                        style.fontSize == 20.sp && style.lineHeight == 32.sp -> 40f
                        style.fontSize == 14.sp && style.lineHeight == 20.sp -> 30f
                        style.fontSize == 16.sp && style.lineHeight == 24.sp -> 38f
                        else -> 38f
                    } * blurBoost)
            t =
                    t.copy(
                            shadow =
                                    Shadow(
                                            color = shadowColor,
                                            offset = Offset.Zero,
                                            blurRadius = blur,
                                    ),
                    )
        }
        return t
    }
    return base.copy(
            displayLarge = tune(base.displayLarge, homeClockScale),
            displayMedium = tune(base.displayMedium),
            displaySmall = tune(base.displaySmall),
            headlineLarge = tune(base.headlineLarge),
            headlineMedium = tune(base.headlineMedium),
            headlineSmall = tune(base.headlineSmall),
            titleLarge = tune(base.titleLarge),
            titleMedium = tune(base.titleMedium),
            titleSmall = tune(base.titleSmall),
            bodyLarge = tune(base.bodyLarge),
            bodyMedium = tune(base.bodyMedium),
            bodySmall = tune(base.bodySmall),
            labelLarge = tune(base.labelLarge),
            labelMedium = tune(base.labelMedium),
            labelSmall = tune(base.labelSmall),
    )
}
