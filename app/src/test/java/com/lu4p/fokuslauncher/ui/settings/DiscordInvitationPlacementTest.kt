package com.lu4p.fokuslauncher.ui.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural placement regression; rendered navigation is covered by Android instrumentation. */
class DiscordInvitationPlacementTest {
    private fun source(path: String): String = sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .map { File(it, "com/lu4p/fokuslauncher/$path") }.first { it.isFile }.readText()

    @Test fun `invitation host belongs only to top level settings not home or the global nav graph`() {
        assertTrue(source("ui/settings/SettingsScreen.kt").contains("DiscordInvitationHost("))
        assertFalse(source("ui/home/HomeScreen.kt").contains("DiscordInvitationHost("))
        assertFalse(source("ui/navigation/NavGraph.kt").contains("DiscordInvitationHost("))
        assertFalse(source("ui/settings/AppearanceSettingsScreen.kt").contains("DiscordInvitationHost("))
    }
}
