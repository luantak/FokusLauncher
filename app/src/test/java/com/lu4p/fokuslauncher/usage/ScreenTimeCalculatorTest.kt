package com.lu4p.fokuslauncher.usage

import android.app.usage.UsageEvents
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeCalculatorTest {

    @Test
    fun computeScreenOnTimeMs_singleSession() {
        assertEquals(
                3_000L,
                computeScreenOnTimeMs(
                        events(
                                UsageEvents.Event.SCREEN_INTERACTIVE to 1_000L,
                                UsageEvents.Event.SCREEN_NON_INTERACTIVE to 4_000L,
                        ),
                        windowStart = 0L,
                        windowEnd = 10_000L,
                ),
        )
    }

    @Test
    fun computeScreenOnTimeMs_screenAlreadyOnAtWindowStart() {
        assertEquals(
                5_000L,
                computeScreenOnTimeMs(
                        events(
                                UsageEvents.Event.SCREEN_NON_INTERACTIVE to 2_000L,
                                UsageEvents.Event.SCREEN_INTERACTIVE to 5_000L,
                                UsageEvents.Event.SCREEN_NON_INTERACTIVE to 8_000L,
                        ),
                        windowStart = 0L,
                        windowEnd = 10_000L,
                ),
        )
    }

    @Test
    fun computeScreenOnTimeMs_stillOnAtWindowEnd() {
        assertEquals(
                7_000L,
                computeScreenOnTimeMs(
                        events(UsageEvents.Event.SCREEN_INTERACTIVE to 3_000L),
                        windowStart = 0L,
                        windowEnd = 10_000L,
                ),
        )
    }

    @Test
    fun computeScreenOnTimeMs_clampsToWindow() {
        assertEquals(
                9_000L,
                computeScreenOnTimeMs(
                        events(
                                UsageEvents.Event.SCREEN_INTERACTIVE to 500L,
                                UsageEvents.Event.SCREEN_NON_INTERACTIVE to 15_000L,
                        ),
                        windowStart = 1_000L,
                        windowEnd = 10_000L,
                ),
        )
    }

    @Test
    fun computeScreenOnTimeMs_deviceShutdownEndsSession() {
        assertEquals(
                5_000L,
                computeScreenOnTimeMs(
                        events(
                                UsageEvents.Event.SCREEN_INTERACTIVE to 1_000L,
                                UsageEvents.Event.DEVICE_SHUTDOWN to 6_000L,
                        ),
                        windowStart = 0L,
                        windowEnd = 10_000L,
                ),
        )
    }

    private fun events(vararg entries: Pair<Int, Long>) = entries.toList()
}
