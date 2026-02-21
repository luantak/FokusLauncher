package com.lu4p.fokuslauncher.data.repository

import com.lu4p.fokuslauncher.data.model.WeatherData
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection

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
        val jsonResponse = """
            {
              "current": {
                "temperature_2m": 22.5,
                "weather_code": 3
              }
            }
        """.trimIndent()
        
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
        val jsonResponse = """
            {
              "current": {
                "temperature_2m": 22.5,
                "weather_code": 3
              }
            }
        """.trimIndent()
        
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
        val jsonResponse1 = """
            {
              "current": {
                "temperature_2m": 22.5,
                "weather_code": 3
              }
            }
        """.trimIndent()
        
        val jsonResponse2 = """
            {
              "current": {
                "temperature_2m": 15.0,
                "weather_code": 0
              }
            }
        """.trimIndent()
        
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
    fun `invalidateCache clears cached data`() {
        repository.invalidateCache()
        // No crash = success; cache is internal state
    }
}

class WeatherDataTest {

    @Test
    fun `weatherEmoji returns sun for clear sky`() {
        val data = WeatherData(temperature = 25, iconCode = "01d")
        assertEquals("☀️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji returns cloud for broken clouds`() {
        val data = WeatherData(temperature = 15, iconCode = "04d")
        assertEquals("☁️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji returns snow for snow`() {
        val data = WeatherData(temperature = -2, iconCode = "13n")
        assertEquals("❄️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji returns rain for rain`() {
        val data = WeatherData(temperature = 10, iconCode = "10d")
        assertEquals("🌦️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji returns thunderstorm`() {
        val data = WeatherData(temperature = 18, iconCode = "11d")
        assertEquals("⛈️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji returns fog for mist`() {
        val data = WeatherData(temperature = 5, iconCode = "50d")
        assertEquals("🌫️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji returns default cloud for unknown code`() {
        val data = WeatherData(temperature = 20, iconCode = "99x")
        assertEquals("☁️", data.weatherEmoji)
    }

    @Test
    fun `weatherEmoji handles empty icon code`() {
        val data = WeatherData(temperature = 20, iconCode = "")
        assertEquals("☁️", data.weatherEmoji)
    }
}
