package com.lu4p.fokuslauncher.data.model

/**
 * Simple weather data for the home screen widget.
 */
data class WeatherData(
    val temperature: Int = 0,
    val description: String = "",
    /** Open-Meteo-style icon code (e.g. `01d`, `10n`) for mapping to a themed weather icon. */
    val iconCode: String = "",
    /**
     * Consolidated air quality index from Open-Meteo when requested (US AQI when Fahrenheit is
     * preferred, otherwise European AQI). Null when AQI was not fetched or the air-quality call
     * failed.
     */
    val aqi: Int? = null,
    val lastUpdated: Long = 0L
)
