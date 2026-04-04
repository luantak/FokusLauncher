package com.lu4p.fokuslauncher.data.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.core.text.util.LocalePreferences
import androidx.core.text.util.LocalePreferences.TemperatureUnit
import java.util.Locale

/**
 * Weather uses the user's regional temperature choice (Settings → Regional preferences), not only
 * what a plain language/region pair would imply.
 *
 * Per-app language can replace [Locale.getDefault] with a locale that **drops** the `mu` Unicode
 * extension Android uses for explicit Celsius/Fahrenheit. We read `mu` from the format default when
 * present, then from [LocaleManager.getSystemLocales] (ignores app overrides), then from
 * [Resources.getSystem], and only then fall back to resolved defaults for a system anchor locale.
 */
object TemperatureUnitHelper {

    private const val UNICODE_TEMPERATURE_UNIT = "mu"

    fun useFahrenheit(context: Context): Boolean {
        explicitTemperatureUnicode(context)?.let { mu ->
            return mu.startsWith(TemperatureUnit.FAHRENHEIT)
        }
        val anchor = systemPrimaryLocale(context)
        return LocalePreferences.getTemperatureUnit(anchor, true) == TemperatureUnit.FAHRENHEIT
    }

    private fun explicitTemperatureUnicode(context: Context): String? {
        unicodeTemperature(Locale.getDefault(Locale.Category.FORMAT))?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val systemList = localeManager?.systemLocales
            if (systemList != null) {
                for (i in 0 until systemList.size()) {
                    unicodeTemperature(systemList[i])?.let { return it }
                }
            }
        }

        val fromSystemResources = Resources.getSystem().configuration.locales
        for (i in 0 until fromSystemResources.size()) {
            unicodeTemperature(fromSystemResources[i])?.let { return it }
        }

        return null
    }

    private fun unicodeTemperature(locale: Locale): String? =
            locale.getUnicodeLocaleType(UNICODE_TEMPERATURE_UNIT)?.takeIf { it.isNotEmpty() }

    private fun systemPrimaryLocale(context: Context): Locale {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val list = localeManager?.systemLocales
            if (list != null && list.size() > 0) {
                return list[0]
            }
        }
        val sys = Resources.getSystem().configuration.locales
        return if (sys.size() > 0) {
            sys[0]
        } else {
            Locale.getDefault(Locale.Category.FORMAT)
        }
    }
}
