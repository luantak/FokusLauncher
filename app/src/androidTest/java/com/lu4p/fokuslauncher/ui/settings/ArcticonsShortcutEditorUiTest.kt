package com.lu4p.fokuslauncher.ui.settings

import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.lu4p.fokuslauncher.data.iconpack.ArcticonsIconPackRepository
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.home.HomeShortcutIcon
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ArcticonsShortcutEditorUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun realPackCustomChoiceSurvivesModeSwitchAndUsesOnlyArcticonsPicker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ArcticonsIconPackRepository(context)
        assertTrue("Install Arcticons before running this acceptance test", repository.isArcticonsInstalled())
        val names = runBlocking { repository.getIconNames() }
        assertTrue(names.size > 100)
        assertTrue(names.contains("calendar_1"))
        val app = AppInfo("com.android.settings", "Settings", null)
        val shortcut = mutableStateOf(HomeShortcut("star", ShortcutTarget.App(app.packageName)))
        val pack = mutableStateOf(repository.installedPackage.value)
        val picker = mutableStateOf(false)
        val namedRequests = java.util.concurrent.CopyOnWriteArrayList<String>()
        val loadNamed: suspend (String) -> Drawable? = {
            namedRequests.add(it)
            repository.getIconByName(it)
        }
        val loadApp: suspend (AppInfo) -> Drawable? = { repository.getIcon(it) }
        compose.setContent {
            FokusLauncherTheme {
                if (picker.value) {
                    ArcticonsIconPickerScreen("settings", names, loadNamed,
                        onSelect = { shortcut.value = shortcut.value.copy(arcticonName = it); picker.value = false },
                        onNavigateBack = { picker.value = false })
                } else {
                    HomeShortcutIcon(shortcut.value, listOf(app), pack.value, 24.dp,
                        loadIcon = loadApp, loadNamedIcon = loadNamed)
                }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("shortcut_arcticon", true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("shortcut_arcticon", true).assertWidthIsEqualTo(32.dp)
        compose.runOnIdle { picker.value = true }
        compose.onNodeWithTag("arcticons_icon_picker").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("calendar_1")
        compose.onNodeWithContentDescription("calendar 1").performClick()
        compose.waitUntil(10_000) { namedRequests.contains("calendar_1") && !picker.value }
        compose.onNodeWithTag("shortcut_arcticon", true).assertWidthIsEqualTo(32.dp)
        compose.runOnIdle { pack.value = null }
        compose.onNodeWithTag("shortcut_custom_icon", true).assertWidthIsEqualTo(24.dp)
        assertEquals("star", shortcut.value.iconName)
        assertEquals("calendar_1", shortcut.value.arcticonName)
        compose.runOnIdle { pack.value = repository.installedPackage.value }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("shortcut_arcticon", true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("shortcut_arcticon", true).assertWidthIsEqualTo(32.dp)
        assertEquals("calendar_1", namedRequests.last())
    }
}
