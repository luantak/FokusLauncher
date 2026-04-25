package com.lu4p.fokuslauncher.data.model

import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R

enum class TemperatureUnit(@param:StringRes val labelRes: Int) {
    SYSTEM_DEFAULT(R.string.settings_temperature_unit_system),
    CELSIUS(R.string.settings_temperature_unit_celsius),
    FAHRENHEIT(R.string.settings_temperature_unit_fahrenheit);

    companion object {
        fun fromString(value: String?): TemperatureUnit =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM_DEFAULT
    }
}