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

    /**
     * Fetches weather data for the given coordinates. Returns cached data if less than 30 minutes
     * old.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return WeatherData or null if the fetch fails
     */
    suspend fun getWeather(lat: Double, lon: Double): WeatherData? {
        // Return cached data if still fresh
        cachedWeather?.let { cached ->
            if (System.currentTimeMillis() - cached.lastUpdated < CACHE_DURATION_MS) {
                return cached
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val url =
                        URL(
                                OPEN_METEO_BASE_URL +
                                        "?latitude=$lat" +
                                        "&longitude=$lon" +
                                        "&current=temperature_2m,weather_code" +
                                        "&temperature_unit=celsius"
                        )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val weather = parseOpenMeteoResponse(response)
                    cachedWeather = weather
                    weather
                } else {
                    cachedWeather
                }
            } catch (_: Exception) {
                cachedWeather
            }
        }
    }

    /** Clears the weather cache. */
    fun invalidateCache() {
        cachedWeather = null
    }

    private fun parseOpenMeteoResponse(json: String): WeatherData {
        val obj = JSONObject(json)
        val current = obj.getJSONObject("current")
        val temperature = current.getDouble("temperature_2m").toInt()
        val weatherCode = current.getInt("weather_code")
        val iconCode = mapOpenMeteoCodeToIcon(weatherCode)
        val description = mapOpenMeteoCodeToDescription(weatherCode)

        return WeatherData(
                temperature = temperature,
                description = description,
                iconCode = iconCode,
                lastUpdated = System.currentTimeMillis()
        )
    }

    private fun mapOpenMeteoCodeToIcon(code: Int): String {
        return when (code) {
            0 -> "01d" // clear
            1, 2 -> "02d" // partly cloudy
            3 -> "04d" // overcast
            45, 48 -> "50d" // fog
            51, 53, 55, 56, 57 -> "09d" // drizzle
            61, 63, 65, 66, 67, 80, 81, 82 -> "10d" // rain
            71, 73, 75, 77, 85, 86 -> "13d" // snow
            95, 96, 99 -> "11d" // thunderstorm
            else -> "03d"
        }
    }

    private fun mapOpenMeteoCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Cloudy"
        }
    }
}
