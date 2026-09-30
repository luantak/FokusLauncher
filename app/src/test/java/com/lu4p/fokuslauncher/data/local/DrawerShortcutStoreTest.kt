package com.lu4p.fokuslauncher.data.local

import android.content.Context
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DrawerShortcutStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearPreferences() {
        context.getSharedPreferences("drawer_shortcuts", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `added shortcut survives recreation and removal is profile specific`() {
        val personal = AppShortcutAction("Chat", "Alice", ShortcutTarget.LauncherShortcut("chat", "alice"))
        val work = personal.copy(profileKey = "10")
        val store = DrawerShortcutStore(context)
        store.add(personal)
        store.add(personal)
        store.add(work)
        assertEquals(listOf(personal, work), DrawerShortcutStore(context).shortcuts.value)
        store.remove(personal)
        assertEquals(listOf(work), DrawerShortcutStore(context).shortcuts.value)
    }
}
