package com.lu4p.fokuslauncher

import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetConfigurationLaunchTest {
    @Test
    fun `missing provider configuration is reported as launch failure`() {
        assertEquals(
                WidgetConfigurationLaunchResult.MISSING_ACTIVITY,
                launchWidgetConfiguration { throw ActivityNotFoundException() },
        )
    }

    @Test
    fun `inaccessible provider configuration is reported as launch failure`() {
        assertEquals(
                WidgetConfigurationLaunchResult.ACCESS_DENIED,
                launchWidgetConfiguration { throw SecurityException("not exported") },
        )
    }

    @Test
    fun `stale bound widget is reported as launch failure`() {
        assertEquals(
                WidgetConfigurationLaunchResult.STALE_BINDING,
                launchWidgetConfiguration { throw IllegalArgumentException("Widget not bound") },
        )
    }

    @Test
    fun `successful launch is reported as started`() {
        assertEquals(WidgetConfigurationLaunchResult.STARTED, launchWidgetConfiguration {})
    }
}
