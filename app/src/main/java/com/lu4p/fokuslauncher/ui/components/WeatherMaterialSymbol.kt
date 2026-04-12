package com.lu4p.fokuslauncher.ui.components

import androidx.annotation.DrawableRes
import com.lu4p.fokuslauncher.R

/**
 * Maps Open-Meteo-style icon codes to [Material Symbols](https://fonts.google.com/icons?icon.set=Material+Symbols) **Outlined**
 * glyphs (24dp), shipped as vector drawables so [androidx.compose.material3.Icon] can tint them.
 */
@DrawableRes
fun weatherMaterialSymbolDrawableRes(iconCode: String): Int {
    return when {
        iconCode.startsWith("01") -> R.drawable.ic_weather_sunny
        iconCode.startsWith("02") -> R.drawable.ic_weather_partly_cloudy_day
        iconCode.startsWith("03") || iconCode.startsWith("04") -> R.drawable.ic_weather_cloud
        iconCode.startsWith("09") -> R.drawable.ic_weather_rainy_light
        iconCode.startsWith("10") -> R.drawable.ic_weather_rainy
        iconCode.startsWith("11") -> R.drawable.ic_weather_thunderstorm
        iconCode.startsWith("13") -> R.drawable.ic_weather_snowy
        iconCode.startsWith("50") -> R.drawable.ic_weather_foggy
        else -> R.drawable.ic_weather_cloud
    }
}
