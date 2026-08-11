package com.lu4p.fokuslauncher.data.repository

import com.lu4p.fokuslauncher.data.model.WeatherData
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        var OPEN_METEO_GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
        var OPEN_METEO_AIR_QUALITY_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    private data class WeatherCacheKey(
            val latE2: Int,
            val lonE2: Int,
            val useFahrenheit: Boolean,
            val includeAqi: Boolean,
    )

    private val weatherCache = mutableMapOf<WeatherCacheKey, WeatherData>()
    private val geocodeCache = mutableMapOf<String, Pair<Double, Double>>()

    /**
     * Fetches weather data for the given coordinates. Returns cached data if less than 30 minutes
     * old and the requested temperature unit / AQI option matches the cache for those coordinates.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @param useFahrenheit When true, requests and parses values in Fahrenheit per Open-Meteo.
     * Also selects US AQI instead of European AQI when [includeAqi] is true.
     * @param includeAqi When true, also fetches current AQI from the Open-Meteo air-quality API.
     * @return WeatherData or null if the forecast fetch fails
     */
    suspend fun getWeather(
            lat: Double,
            lon: Double,
            useFahrenheit: Boolean = false,
            includeAqi: Boolean = false,
    ): WeatherData? {
        val key = weatherCacheKey(lat, lon, useFahrenheit, includeAqi)
        synchronized(weatherCache) {
            weatherCache[key]?.let { cached ->
                if (System.currentTimeMillis() - cached.lastUpdated < CACHE_DURATION_MS) {
                    return cached
                }
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                coroutineScope {
                    val forecastDeferred = async { fetchCurrentForecast(lat, lon, useFahrenheit) }
                    val aqiDeferred =
                            if (includeAqi) {
                                async { fetchCurrentAqi(lat, lon, useUsAqi = useFahrenheit) }
                            } else {
                                null
                            }
                    val forecast = forecastDeferred.await()
                    if (forecast == null) {
                        synchronized(weatherCache) { weatherCache[key] }
                    } else {
                        val weather =
                                forecast.copy(
                                        aqi = if (includeAqi) aqiDeferred?.await() else null,
                                )
                        synchronized(weatherCache) { weatherCache[key] = weather }
                        weather
                    }
                }
            } catch (_: Exception) {
                synchronized(weatherCache) { weatherCache[key] }
            }
        }
    }

    /**
     * Resolves [placeName] via Open-Meteo geocoding, then fetches current weather for that point.
     */
    suspend fun getWeatherForPlace(
            placeName: String,
            useFahrenheit: Boolean = false,
            includeAqi: Boolean = false,
    ): WeatherData? {
        val coords = geocode(placeName) ?: return null
        return getWeather(coords.first, coords.second, useFahrenheit, includeAqi)
    }

    /**
     * Looks up latitude/longitude for a place name. Results are cached for the process lifetime.
     */
    suspend fun geocode(placeName: String): Pair<Double, Double>? {
        val query = placeName.trim()
        if (query.isEmpty()) return null
        val cacheKey = query.lowercase()
        synchronized(geocodeCache) { geocodeCache[cacheKey] }?.let {
            return it
        }

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                val url = URL("$OPEN_METEO_GEOCODING_URL?name=$encoded&count=1")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                if (connection.responseCode != 200) return@withContext null
                val response = connection.inputStream.bufferedReader().readText()
                val coords = parseGeocodeResponse(response) ?: return@withContext null
                synchronized(geocodeCache) { geocodeCache[cacheKey] = coords }
                coords
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Clears weather and geocode caches. */
    fun invalidateCache() {
        synchronized(weatherCache) { weatherCache.clear() }
        synchronized(geocodeCache) { geocodeCache.clear() }
    }

    private fun weatherCacheKey(
            lat: Double,
            lon: Double,
            useFahrenheit: Boolean,
            includeAqi: Boolean,
    ): WeatherCacheKey =
            WeatherCacheKey(
                    latE2 = (lat * 100.0).roundToInt(),
                    lonE2 = (lon * 100.0).roundToInt(),
                    useFahrenheit = useFahrenheit,
                    includeAqi = includeAqi,
            )

    private fun fetchCurrentForecast(
            lat: Double,
            lon: Double,
            useFahrenheit: Boolean,
    ): WeatherData? {
        val temperatureUnit = if (useFahrenheit) "fahrenheit" else "celsius"
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
        if (connection.responseCode != 200) return null
        val response = connection.inputStream.bufferedReader().readText()
        return parseOpenMeteoResponse(response)
    }

    private fun fetchCurrentAqi(lat: Double, lon: Double, useUsAqi: Boolean): Int? {
        val aqiField = if (useUsAqi) "us_aqi" else "european_aqi"
        val url =
                URL(
                        OPEN_METEO_AIR_QUALITY_URL +
                                "?latitude=$lat" +
                                "&longitude=$lon" +
                                "&current=$aqiField"
                )
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        if (connection.responseCode != 200) return null
        val response = connection.inputStream.bufferedReader().readText()
        return parseAirQualityResponse(response, aqiField)
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

    private fun parseAirQualityResponse(json: String, aqiField: String): Int? {
        val current = JSONObject(json).optJSONObject("current") ?: return null
        if (!current.has(aqiField) || current.isNull(aqiField)) return null
        return current.getDouble(aqiField).roundToInt()
    }

    private fun parseGeocodeResponse(json: String): Pair<Double, Double>? {
        val results = JSONObject(json).optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.optJSONObject(0) ?: return null
        if (!first.has("latitude") || !first.has("longitude")) return null
        return first.getDouble("latitude") to first.getDouble("longitude")
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
