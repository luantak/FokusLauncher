package com.lu4p.fokuslauncher.utils

import android.view.Display
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrozenRendererRecoveryTest {

    @Test
    fun `shouldRestart when viewRoot stuck OFF while display ON and interval elapsed`() {
        assertTrue(
                FrozenRendererRecovery.shouldRestartForStuckDisplayState(
                        viewRootDisplayState = Display.STATE_OFF,
                        actualDisplayState = Display.STATE_ON,
                        nowElapsedMs = 10_000L,
                        lastRestartElapsedMs = 0L,
                )
        )
    }

    @Test
    fun `shouldRestart when display is ON_SUSPEND`() {
        assertTrue(
                FrozenRendererRecovery.shouldRestartForStuckDisplayState(
                        viewRootDisplayState = Display.STATE_OFF,
                        actualDisplayState = Display.STATE_ON_SUSPEND,
                        nowElapsedMs = 10_000L,
                        lastRestartElapsedMs = 0L,
                )
        )
    }

    @Test
    fun `should not restart when viewRoot already ON`() {
        assertFalse(
                FrozenRendererRecovery.shouldRestartForStuckDisplayState(
                        viewRootDisplayState = Display.STATE_ON,
                        actualDisplayState = Display.STATE_ON,
                        nowElapsedMs = 10_000L,
                        lastRestartElapsedMs = 0L,
                )
        )
    }

    @Test
    fun `should not restart when actual display is still OFF`() {
        assertFalse(
                FrozenRendererRecovery.shouldRestartForStuckDisplayState(
                        viewRootDisplayState = Display.STATE_OFF,
                        actualDisplayState = Display.STATE_OFF,
                        nowElapsedMs = 10_000L,
                        lastRestartElapsedMs = 0L,
                )
        )
    }

    @Test
    fun `should not restart when viewRoot state unavailable`() {
        assertFalse(
                FrozenRendererRecovery.shouldRestartForStuckDisplayState(
                        viewRootDisplayState = -1,
                        actualDisplayState = Display.STATE_ON,
                        nowElapsedMs = 10_000L,
                        lastRestartElapsedMs = 0L,
                )
        )
    }

    @Test
    fun `should not restart within min interval`() {
        assertFalse(
                FrozenRendererRecovery.shouldRestartForStuckDisplayState(
                        viewRootDisplayState = Display.STATE_OFF,
                        actualDisplayState = Display.STATE_ON,
                        nowElapsedMs = 4_000L,
                        lastRestartElapsedMs = 0L,
                        minRestartIntervalMs = 5_000L,
                )
        )
    }

    @Test
    fun `isDisplayVisiblyOn covers ON and ON_SUSPEND only`() {
        assertTrue(FrozenRendererRecovery.isDisplayVisiblyOn(Display.STATE_ON))
        assertTrue(FrozenRendererRecovery.isDisplayVisiblyOn(Display.STATE_ON_SUSPEND))
        assertFalse(FrozenRendererRecovery.isDisplayVisiblyOn(Display.STATE_OFF))
        assertFalse(FrozenRendererRecovery.isDisplayVisiblyOn(Display.STATE_DOZE))
        assertFalse(FrozenRendererRecovery.isDisplayVisiblyOn(Display.STATE_UNKNOWN))
    }
}
