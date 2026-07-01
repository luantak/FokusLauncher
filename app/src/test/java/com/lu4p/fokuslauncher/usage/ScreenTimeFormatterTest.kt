package com.lu4p.fokuslauncher.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeFormatterTest {

    @Test
    fun formatScreenTimeDuration_zeroOrNegative_returnsZeroMinutes() {
        assertEquals("0m", formatScreenTimeDuration(0L))
        assertEquals("0m", formatScreenTimeDuration(-1L))
    }

    @Test
    fun formatScreenTimeDuration_underOneMinute_returnsLessThanOneMinute() {
        assertEquals("< 1m", formatScreenTimeDuration(30_000L))
    }

    @Test
    fun formatScreenTimeDuration_minutesOnly() {
        assertEquals("12m", formatScreenTimeDuration(12 * 60_000L))
    }

    @Test
    fun formatScreenTimeDuration_hoursOnly() {
        assertEquals("2h", formatScreenTimeDuration(2 * 60 * 60_000L))
    }

    @Test
    fun formatScreenTimeDuration_hoursAndMinutes() {
        assertEquals("4h 32m", formatScreenTimeDuration((4 * 60 + 32) * 60_000L))
    }
}
