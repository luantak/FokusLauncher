package com.lu4p.fokuslauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetTapTargetTest {

    @Test
    fun `decode blank returns null`() {
        assertNull(decodeWidgetTapTarget(""))
        assertNull(decodeWidgetTapTarget("   "))
    }

    @Test
    fun `legacy package name decodes as owner profile app target`() {
        val decoded = decodeWidgetTapTarget("com.example.calendar")
        assertEquals(ShortcutTarget.App("com.example.calendar"), decoded?.target)
        assertEquals("0", decoded?.profileKey)
    }

    @Test
    fun `encode and decode round trip preserves target and profile`() {
        val binding =
                WidgetTapTarget(
                        target =
                                ShortcutTarget.LauncherShortcut(
                                        packageName = "com.example",
                                        shortcutId = "month_view",
                                ),
                        profileKey = "10",
                )
        assertEquals(binding, decodeWidgetTapTarget(encodeWidgetTapTarget(binding)))
    }

    @Test
    fun `encode null returns empty string`() {
        assertEquals("", encodeWidgetTapTarget(null))
    }
}
