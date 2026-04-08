package com.lu4p.fokuslauncher.data.repository

import com.lu4p.fokuslauncher.data.model.WeatherData
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Repository for fetching weather data. Uses Open-Meteo (no API key required). Uses plain
 * HttpURLConnection to avoid additional dependencies.
 */
@Singleton
class WeatherRepository @Inject constructor() {

    companion object {
        var OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1/forecast"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    private var cachedWeather: WeatherData? = null
    private var cachedUseFahrenheit: Boolean? = null

    /**
     * Fetches weather data for the given coordinates. Returns cached data if less than 30 minutes
     * old and the requested temperature unit matches the cache.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @param useFahrenheit When true, requests and parses values in Fahrenheit per Open-Meteo.
     * @return WeatherData or null if the fetch fails
     */
    suspend fun getWeather(lat: Double, lon: Double, useFahrenheit: Boolean = false): WeatherData? {
        // Return cached data if still fresh
        cachedWeather?.let { cached ->
            if (cachedUseFahrenheit == useFahrenheit &&
                            System.currentTimeMillis() - cached.lastUpdated < CACHE_DURATION_MS
            ) {
                return cached
            }
        }

        val temperatureUnit = if (useFahrenheit) "fahrenheit" else "celsius"

        return withContext(Dispatchers.IO) {
            try {
                val url =
                        URL(
                                OPEN_METEO_BASE_URL +
                                        "?latitude=$lat" +
                                        "&longitude=$lon" +
                                        "&current=temperature_2m,weather_code" +
                                        "&temperature_unit=$temperatureUnit"
                        )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val weather = parseOpenMeteoResponse(response)
                    cachedWeather = weather
                    cachedUseFahrenheit = useFahrenheit
                    weather
                } else {
                    cachedWeather?.takeIf { cachedUseFahrenheit == useFahrenheit }
                }
            } catch (_: Exception) {
                cachedWeather?.takeIf { cachedUseFahrenheit == useFahrenheit }
            }
        }
    }

    /** Clears the weather cache. */
    fun invalidateCache() {
        cachedWeather = null
        cachedUseFahrenheit = null
    }

    private fun parseOpenMeteoResponse(json: String): WeatherData {
        val obj = JSONObject(json)
        val current = obj.getJSONObject("current")
        val temperature = current.getDouble("temperature_2m").toInt()
        val weatherCode = current.getInt("weather_code")
        val openMeteo = openMeteoPresentation(weatherCode)

        return WeatherData(
                temperature = temperature,
                description = openMeteo.description,
                iconCode = openMeteo.iconCode,
                lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Single lookup for Open-Meteo WMO weather codes. Description is not shown in the launcher UI
     * but kept on [WeatherData] for completeness / debugging.
     */
    private data class OpenMeteoPresentation(val iconCode: String, val description: String)

    private fun openMeteoPresentation(code: Int): OpenMeteoPresentation {
        return when (code) {
            0 -> OpenMeteoPresentation("01d", "Clear sky") // clear
            1, 2 -> OpenMeteoPresentation("02d", "Partly cloudy") // partly cloudy
            3 -> OpenMeteoPresentation("04d", "Overcast") // overcast
            45, 48 -> OpenMeteoPresentation("50d", "Foggy") // fog
            51, 53, 55, 56, 57 -> OpenMeteoPresentation("09d", "Drizzle") // drizzle
            61, 63, 65, 66, 67, 80, 81, 82 ->
                    OpenMeteoPresentation("10d", "Rain") // rain
            71, 73, 75, 77, 85, 86 -> OpenMeteoPresentation("13d", "Snow") // snow
            95, 96, 99 -> OpenMeteoPresentation("11d", "Thunderstorm") // thunderstorm
            else -> OpenMeteoPresentation("03d", "Cloudy")
        }
    }
}
