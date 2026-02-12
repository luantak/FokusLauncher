package com.lu4p.fokuslauncher.data.model

/**
 * Simple weather data for the home screen widget.
 */
data class WeatherData(
    val temperature: Int = 0,
    val description: String = "",
    val iconCode: String = "",
    val lastUpdated: Long = 0L
) {
    /**
     * Maps OpenWeatherMap icon codes to Unicode weather symbols.
     */
    val weatherEmoji: String
        get() = when {
            iconCode.startsWith("01") -> "☀️" // clear sky
            iconCode.startsWith("02") -> "⛅" // few clouds
            iconCode.startsWith("03") -> "☁️" // scattered clouds
            iconCode.startsWith("04") -> "☁️" // broken clouds
            iconCode.startsWith("09") -> "🌧️" // shower rain
            iconCode.startsWith("10") -> "🌦️" // rain
            iconCode.startsWith("11") -> "⛈️" // thunderstorm
            iconCode.startsWith("13") -> "❄️" // snow
            iconCode.startsWith("50") -> "🌫️" // mist
            else -> "☁️"
        }
}
