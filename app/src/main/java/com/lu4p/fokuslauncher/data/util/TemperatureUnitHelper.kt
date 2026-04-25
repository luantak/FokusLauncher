package com.lu4p.fokuslauncher.data.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.core.text.util.LocalePreferences
import androidx.core.text.util.LocalePreferences.TemperatureUnit
import com.lu4p.fokuslauncher.data.model.TemperatureUnit as AppTemperatureUnit
import java.util.Locale

object TemperatureUnitHelper {

    private const val UNICODE_TEMPERATURE_UNIT = "mu"

    fun useFahrenheit(context: Context, override: AppTemperatureUnit = AppTemperatureUnit.SYSTEM_DEFAULT): Boolean {
        return when (override) {
            AppTemperatureUnit.FAHRENHEIT -> true
            AppTemperatureUnit.CELSIUS -> false
            AppTemperatureUnit.SYSTEM_DEFAULT -> useFahrenheitFromSystem(context)
        }
    }

    private fun useFahrenheitFromSystem(context: Context): Boolean {
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