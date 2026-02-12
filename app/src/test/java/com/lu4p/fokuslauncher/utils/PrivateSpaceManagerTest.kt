package com.lu4p.fokuslauncher.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrivateSpaceManagerTest {

    private lateinit var context: Context
    private lateinit var userManager: UserManager
    private lateinit var launcherApps: LauncherApps
    private lateinit var manager: PrivateSpaceManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        userManager = mockk(relaxed = true)
        launcherApps = mockk(relaxed = true)

        every { context.getSystemService(Context.USER_SERVICE) } returns userManager
        every { context.getSystemService(Context.LAUNCHER_APPS_SERVICE) } returns launcherApps

        manager = PrivateSpaceManager(context)
    }

    @Test
    fun `isSupported returns based on SDK version`() {
        // This will depend on the test environment's SDK level
        // On a standard JVM test, Build.VERSION.SDK_INT is 0
        // so isSupported should return false
        assertFalse(manager.isSupported)
    }

    @Test
    fun `getPrivateSpaceProfile returns null when not supported`() {
        val profile = manager.getPrivateSpaceProfile()
        // On test JVM, SDK_INT < 35, so should return null
        assertFalse(manager.isSupported)
        assertTrue(profile == null)
    }

    @Test
    fun `getPrivateSpaceApps returns empty list when not supported`() {
        val apps = manager.getPrivateSpaceApps()
        assertTrue(apps.isEmpty())
    }

    @Test
    fun `isPrivateSpaceUnlocked returns false when not supported`() {
        assertFalse(manager.isPrivateSpaceUnlocked())
    }

    @Test
    fun `requestUnlock returns false when not supported`() {
        assertFalse(manager.requestUnlock())
    }

    @Test
    fun `lock returns false when not supported`() {
        assertFalse(manager.lock())
    }
}
