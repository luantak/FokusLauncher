package com.lu4p.fokuslauncher.utils

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PrivateSpaceManagerTest {

    @Test
    @Config(sdk = [34])
    fun `isSupported is false on pre Android 15`() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        val manager = PrivateSpaceManager(context)
        assertFalse(manager.isSupported)
    }

    @Test
    @Config(sdk = [35])
    fun `isSupported is true on Android 15 and above`() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        val manager = PrivateSpaceManager(context)
        assertTrue(manager.isSupported)
    }

    @Test
    @Config(sdk = [34])
    fun `private space operations are safe no-ops when unsupported`() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        val manager = PrivateSpaceManager(context)

        assertTrue(manager.getPrivateSpaceProfile() == null)
        assertTrue(manager.getPrivateSpaceApps().isEmpty())
        assertFalse(manager.isPrivateSpaceUnlocked())
        assertFalse(manager.requestUnlock())
        assertFalse(manager.lock())
    }
}
