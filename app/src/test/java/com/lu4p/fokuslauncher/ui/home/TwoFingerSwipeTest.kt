package com.lu4p.fokuslauncher.ui.home

import androidx.compose.ui.geometry.Offset
import com.lu4p.fokuslauncher.data.local.TwoFingerDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwoFingerSwipeTest {
    @Test fun `recognizes four matching directions`() {
        assertEquals(TwoFingerDirection.UP, twoFingerSwipeDirection(Offset(0f, -90f), Offset(2f, -85f), 72f))
        assertEquals(TwoFingerDirection.DOWN, twoFingerSwipeDirection(Offset(0f, 90f), Offset(2f, 85f), 72f))
        assertEquals(TwoFingerDirection.LEFT, twoFingerSwipeDirection(Offset(-90f, 0f), Offset(-85f, 2f), 72f))
        assertEquals(TwoFingerDirection.RIGHT, twoFingerSwipeDirection(Offset(90f, 0f), Offset(85f, 2f), 72f))
    }

    @Test fun `rejects pinch stationary finger and short motion`() {
        assertNull(twoFingerSwipeDirection(Offset(-100f, 0f), Offset(100f, 0f), 72f))
        assertNull(twoFingerSwipeDirection(Offset(120f, 0f), Offset.Zero, 72f))
        assertNull(twoFingerSwipeDirection(Offset(40f, 0f), Offset(40f, 0f), 72f))
        assertNull(twoFingerSwipeDirection(Offset(100f, -100f), Offset(100f, 100f), 72f))
    }
}
