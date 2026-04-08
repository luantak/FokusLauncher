package com.lu4p.fokuslauncher.data.model

import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R

/**
 * How the home screen date line is formatted. Time-of-day still follows the system 12/24h setting.
 *
 * - [SYSTEM_DEFAULT]: Short weekday + day + abbreviated month, locale best pattern (legacy behavior).
 * - [US_SLASHES]: MM/dd/yyyy
 * - [EU_SLASHES]: dd/MM/yyyy
 * - [EU_DOTS]: dd.MM.yyyy
 * - [WEEKDAY_MONTH_ABBR]: Abbreviated weekday, abbreviated month, day, year (e.g. Tue Apr 7, 2026).
 * - [MONTH_LONG]: Full month name, day, year (e.g. April 7, 2026).
 */
enum class HomeDateFormatStyle(@param:StringRes val labelRes: Int) {
    SYSTEM_DEFAULT(R.string.settings_home_date_format_system),
    US_SLASHES(R.string.settings_home_date_format_us_slashes),
    EU_SLASHES(R.string.settings_home_date_format_eu_slashes),
    EU_DOTS(R.string.settings_home_date_format_eu_dots),
    WEEKDAY_MONTH_ABBR(R.string.settings_home_date_format_weekday_month),
    MONTH_LONG(R.string.settings_home_date_format_month_long);

    companion object {
        fun fromString(value: String?): HomeDateFormatStyle =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM_DEFAULT
    }
}
