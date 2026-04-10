package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.material3.Typography
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
 */
fun fokusTypographyForLauncher(fontFamily: FontFamily, fontScale: Float = 1f): Typography {
    val base = FokusTypography
    fun tune(style: TextStyle) =
            style.withLauncherFontScale(fontScale).withLauncherFont(fontFamily)
    return base.copy(
            displayLarge = tune(base.displayLarge),
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
