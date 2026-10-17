package com.lu4p.fokuslauncher.data.local

import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShortcutIconPersistenceTest {
    @Test
    fun legacyChoicesRemainReadable() {
        val manager = PreferencesManager(RuntimeEnvironment.getApplication())
        val parse = PreferencesManager::class.java.getDeclaredMethod("parseRightSideShortcuts", String::class.java)
        parse.isAccessible = true
        val target = ShortcutTarget.App("com.example.app")
        val raw = "star;${ShortcutTarget.encode(target)};10"
        assertEquals(listOf(HomeShortcut("star", target, "10")), parse.invoke(manager, raw))
    }

    @Test
    fun deepLinksAndProfilesSurviveRoundTrip() {
        val manager = PreferencesManager(RuntimeEnvironment.getApplication())
        val shortcuts = listOf(
            HomeShortcut("star", ShortcutTarget.DeepLink("intent:#Intent;action=test;S.value=a|b;end"), "10", "calendar_1"),
            HomeShortcut("phone", ShortcutTarget.PhoneDial, "0", "phone"),
        )
        val serialize = PreferencesManager::class.java.getDeclaredMethod("serializeRightSideShortcuts", List::class.java)
        val parse = PreferencesManager::class.java.getDeclaredMethod("parseRightSideShortcuts", String::class.java)
        serialize.isAccessible = true
        parse.isAccessible = true
        assertEquals(shortcuts, parse.invoke(manager, serialize.invoke(manager, shortcuts)))
    }

    @Test
    fun bothIconChoicesSurviveRoundTrip() {
        val manager = PreferencesManager(RuntimeEnvironment.getApplication())
        val shortcut = HomeShortcut("star", ShortcutTarget.App("com.example.app"), arcticonName = "calendar")
        val serialize = PreferencesManager::class.java.getDeclaredMethod("serializeRightSideShortcuts", List::class.java)
        val parse = PreferencesManager::class.java.getDeclaredMethod("parseRightSideShortcuts", String::class.java)
        serialize.isAccessible = true
        parse.isAccessible = true
        val saved = serialize.invoke(manager, listOf(shortcut)) as String
        assertEquals(listOf(shortcut), parse.invoke(manager, saved))
    }
}
