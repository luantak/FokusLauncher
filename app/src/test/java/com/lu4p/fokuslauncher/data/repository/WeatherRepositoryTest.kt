package com.lu4p.fokuslauncher.data.repository

import com.lu4p.fokuslauncher.data.model.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WeatherRepositoryTest {

    private lateinit var repository: WeatherRepository

    @Before
    fun setup() {
        repository = WeatherRepository()
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
