package com.lu4p.fokuslauncher.data.util

import android.content.Context
import androidx.core.text.util.LocalePreferences
import androidx.core.text.util.LocalePreferences.TemperatureUnit
import java.util.Locale

/**
 * Resolves whether to show weather in Fahrenheit using the system regional preference
 * ([LocalePreferences]) for the app's current format locale.
 */
object TemperatureUnitHelper {

    fun useFahrenheit(context: Context): Boolean {
        val locale = formatLocale(context)
        val unit = LocalePreferences.getTemperatureUnit(locale, true)
        return unit == TemperatureUnit.FAHRENHEIT
    }

    private fun formatLocale(context: Context): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.size() > 0) {
            locales[0]
        } else {
            Locale.getDefault(Locale.Category.FORMAT)
        }
    }
}
