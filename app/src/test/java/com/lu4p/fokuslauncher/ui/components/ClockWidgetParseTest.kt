package com.lu4p.fokuslauncher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockWidgetParseTest {

    @Test
    fun resolve_24h_returns_null() {
        assertNull(resolveClockMainAndPeriod("15:58", is24HourFormat = true))
        assertNull(resolveClockMainAndPeriod("3:58 PM", is24HourFormat = true))
    }

    @Test
    fun resolve_newline_split() {
        assertEquals(
                "3:58" to "PM",
                resolveClockMainAndPeriod("3:58\nPM", is24HourFormat = false),
        )
    }

    @Test
    fun resolve_single_line_with_space() {
        assertEquals(
                "3:58" to "PM",
                resolveClockMainAndPeriod("3:58 PM", is24HourFormat = false),
        )
        assertEquals(
                "11:59" to "am",
                resolveClockMainAndPeriod("11:59 am", is24HourFormat = false),
        )
    }

    @Test
    fun resolve_narrow_no_break_space_before_pm() {
        assertEquals(
                "3:58" to "PM",
                resolveClockMainAndPeriod("3:58\u202fPM", is24HourFormat = false),
        )
    }
}
