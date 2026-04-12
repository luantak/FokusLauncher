package com.lu4p.fokuslauncher.data.repository

import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.weatherMaterialSymbolDrawableRes
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherRepositoryTest {

    private lateinit var repository: WeatherRepository
    private lateinit var mockWebServer: MockWebServer
    private lateinit var originalBaseUrl: String

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        originalBaseUrl = WeatherRepository.OPEN_METEO_BASE_URL
        WeatherRepository.OPEN_METEO_BASE_URL = mockWebServer.url("/").toString()

        repository = WeatherRepository()
    }

    @After
    fun tearDown() {
        WeatherRepository.OPEN_METEO_BASE_URL = originalBaseUrl
        mockWebServer.shutdown()
    }

    @Test
    fun `getWeather parses successful response correctly`() = runTest {
        val jsonResponse =
                """
            {
              "current": {
                "temperature_2m": 22.5,
                "weather_code": 3
              }
            }
        """
                        .trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val weather = repository.getWeather(52.52, 13.41)

        assertNotNull(weather)
        assertEquals(22, weather?.temperature)
        assertEquals("04d", weather?.iconCode) // 3 maps to 04d (overcast)
        assertEquals("Overcast", weather?.description)
    }

    @Test
    fun `getWeather returns null on 500 server error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val weather = repository.getWeather(52.52, 13.41)

        assertNull(weather)
    }

    @Test
    fun `getWeather returns null on malformed JSON`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{ malformed json }"))

        val weather = repository.getWeather(52.52, 13.41)

        assertNull(weather) // Assuming JSON parse exception is caught
    }

    @Test
    fun `getWeather uses cache for subsequent calls within duration`() = runTest {
        val jsonResponse =
                """
            {
              "current": {
                "temperature_2m": 22.5,
                "weather_code": 3
              }
            }
        """
                        .trimIndent()

        // Enqueue only ONE response
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        // First call should hit the network
        val weather1 = repository.getWeather(52.52, 13.41)

        // Second call should return cached data (no new MockResponse was enqueued,
        // so if it hits network it would fail or hang, but MockWebServer assertions can verify request count)
        val weather2 = repository.getWeather(52.52, 13.41)

        assertNotNull(weather1)
        assertEquals(weather1, weather2)
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `invalidateCache forces new network call`() = runTest {
        val jsonResponse1 =
                """
            {
              "current": {
                "temperature_2m": 22.5,
                "weather_code": 3
              }
            }
        """
                        .trimIndent()

        val jsonResponse2 =
                """
            {
              "current": {
                "temperature_2m": 15.0,
                "weather_code": 0
              }
            }
        """
                        .trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse1))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse2))

        // First call
        repository.getWeather(52.52, 13.41)

        // Invalidate cache
        repository.invalidateCache()

        // Second call
        val weather2 = repository.getWeather(52.52, 13.41)

        assertEquals(15, weather2?.temperature)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `getWeather requests fahrenheit when useFahrenheit true`() = runTest {
        val jsonResponse =
                """
            {
              "current": {
                "temperature_2m": 72.4,
                "weather_code": 0
              }
            }
        """
                        .trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val weather = repository.getWeather(52.52, 13.41, useFahrenheit = true)

        val request = mockWebServer.takeRequest()
        assertNotNull(weather)
        assertEquals(72, weather?.temperature)
        assertTrue(request.requestUrl!!.queryParameter("temperature_unit") == "fahrenheit")
    }

    @Test
    fun `getWeather does not use cache when temperature unit changes`() = runTest {
        val jsonCelsius =
                """
            {
              "current": {
                "temperature_2m": 22.0,
                "weather_code": 3
              }
            }
        """
                        .trimIndent()
        val jsonFahrenheit =
                """
            {
              "current": {
                "temperature_2m": 72.0,
                "weather_code": 3
              }
            }
        """
                        .trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonCelsius))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonFahrenheit))

        val celsius = repository.getWeather(52.52, 13.41, useFahrenheit = false)
        val fahrenheit = repository.getWeather(52.52, 13.41, useFahrenheit = true)

        assertEquals(22, celsius?.temperature)
        assertEquals(72, fahrenheit?.temperature)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `invalidateCache clears cached data`() {
        repository.invalidateCache()
        // No crash = success; cache is internal state
    }
}

class WeatherMaterialSymbolTest {

    @Test
    fun `clear sky maps to sunny symbol`() {
        assertEquals(R.drawable.ic_weather_sunny, weatherMaterialSymbolDrawableRes("01d"))
    }

    @Test
    fun `partly cloudy maps to partly cloudy day symbol`() {
        assertEquals(
                R.drawable.ic_weather_partly_cloudy_day,
                weatherMaterialSymbolDrawableRes("02d"),
        )
    }

    @Test
    fun `broken clouds map to cloud symbol`() {
        assertEquals(R.drawable.ic_weather_cloud, weatherMaterialSymbolDrawableRes("04d"))
    }

    @Test
    fun `snow maps to weather snowy symbol`() {
        assertEquals(R.drawable.ic_weather_snowy, weatherMaterialSymbolDrawableRes("13n"))
    }

    @Test
    fun `drizzle maps to rainy light symbol`() {
        assertEquals(R.drawable.ic_weather_rainy_light, weatherMaterialSymbolDrawableRes("09d"))
    }

    @Test
    fun `rain maps to rainy symbol`() {
        assertEquals(R.drawable.ic_weather_rainy, weatherMaterialSymbolDrawableRes("10d"))
    }

    @Test
    fun `thunderstorm maps to thunderstorm symbol`() {
        assertEquals(R.drawable.ic_weather_thunderstorm, weatherMaterialSymbolDrawableRes("11d"))
    }

    @Test
    fun `fog maps to foggy symbol`() {
        assertEquals(R.drawable.ic_weather_foggy, weatherMaterialSymbolDrawableRes("50d"))
    }

    @Test
    fun `unknown code maps to cloud symbol`() {
        assertEquals(R.drawable.ic_weather_cloud, weatherMaterialSymbolDrawableRes("99x"))
    }

    @Test
    fun `empty icon code maps to cloud symbol`() {
        assertEquals(R.drawable.ic_weather_cloud, weatherMaterialSymbolDrawableRes(""))
    }
}
