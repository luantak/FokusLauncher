package com.lu4p.fokuslauncher.data.model

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
enum class HomeDateFormatStyle {
    SYSTEM_DEFAULT,
    US_SLASHES,
    EU_SLASHES,
    EU_DOTS,
    WEEKDAY_MONTH_ABBR,
    MONTH_LONG;

    companion object {
        fun fromString(value: String?): HomeDateFormatStyle =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM_DEFAULT
    }
}
