package com.lu4p.fokuslauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutTargetTest {

    @Test
    fun `decode supports legacy package string`() {
        val target = ShortcutTarget.decode("com.lu4p.chrome")
        assertEquals(ShortcutTarget.App("com.lu4p.chrome"), target)
    }

    @Test
    fun `decode supports app prefixed value`() {
        val target = ShortcutTarget.decode("app:com.lu4p.gmail")
        assertEquals(ShortcutTarget.App("com.lu4p.gmail"), target)
    }

    @Test
    fun `decode supports deep link prefixed value`() {
        val target = ShortcutTarget.decode("intent:intent://chat/123#Intent;scheme=whatsapp;end")
        assertEquals(
            ShortcutTarget.DeepLink("intent://chat/123#Intent;scheme=whatsapp;end"),
            target
        )
    }

    @Test
    fun `decode blank returns null`() {
        assertNull(ShortcutTarget.decode(""))
        assertNull(ShortcutTarget.decode("   "))
    }

    @Test
    fun `encode and decode launcher shortcut`() {
        val target = ShortcutTarget.LauncherShortcut(
            packageName = "com.lu4p.chat",
            shortcutId = "new_message"
        )

        assertEquals(target, ShortcutTarget.decode(ShortcutTarget.encode(target)))
    }

    @Test
    fun `encode round trips app and deep link`() {
        val app = ShortcutTarget.App("com.lu4p.maps")
        val deepLink = ShortcutTarget.DeepLink("https://example.com/path")

        assertEquals(app, ShortcutTarget.decode(ShortcutTarget.encode(app)))
        assertEquals(deepLink, ShortcutTarget.decode(ShortcutTarget.encode(deepLink)))
    }

    @Test
    fun `encode and decode phone dial internal token`() {
        val dial = ShortcutTarget.PhoneDial
        assertEquals("internal:phone", ShortcutTarget.encode(dial))
        assertEquals(dial, ShortcutTarget.decode("internal:phone"))
    }

    @Test
    fun `encode and decode widget page internal token`() {
        val target = ShortcutTarget.WidgetPage
        assertEquals("internal:widget_page", ShortcutTarget.encode(target))
        assertEquals(target, ShortcutTarget.decode("internal:widget_page"))
    }

    @Test
    fun `decode unknown internal token returns null`() {
        assertNull(ShortcutTarget.decode("internal:unknown"))
    }
}
