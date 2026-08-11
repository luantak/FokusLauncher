package com.lu4p.fokuslauncher.ui.util

import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemAnimationsTest {

    @Test
    fun `areSystemAnimationsEnabled is true by default`() {
        val context = RuntimeEnvironment.getApplication()
        assertTrue(context.areSystemAnimationsEnabled())
    }

    @Test
    fun `areSystemAnimationsEnabled is false when animator duration scale is zero`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                0f,
        )
        assertFalse(context.areSystemAnimationsEnabled())
    }

    @Test
    fun `areSystemAnimationsEnabled is true when animator duration scale is non-zero`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                0.5f,
        )
        assertTrue(context.areSystemAnimationsEnabled())
    }
}
