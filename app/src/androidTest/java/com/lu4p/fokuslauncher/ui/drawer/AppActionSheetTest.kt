package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppActionSheetTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun drawerShortcut_cannotBeAddedToHome() {
        val shortcut = AppInfo("chat", "Chat - Alice", null,
                launcherShortcutId = "alice", isDrawerShortcut = true)
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppActionSheet(app = shortcut, categories = emptyList(), onDismiss = {},
                        onAddToHome = {}, onRename = {}, onSetCategory = {}, onHide = {},
                        onAppInfo = {}, onUninstall = {}, onRemoveShortcut = {})
            }
        }
        assertEquals(0, composeTestRule.onAllNodesWithTag("action_add_to_home").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("action_remove_shortcut").assertIsDisplayed()
    }

    @Test
    fun publishedShortcut_hasAddToDrawerAction() {
        val action = com.lu4p.fokuslauncher.data.model.AppShortcutAction("Chat", "Alice",
                com.lu4p.fokuslauncher.data.model.ShortcutTarget.LauncherShortcut("chat", "alice"))
        composeTestRule.setContent {
            FokusLauncherTheme {
                AppActionSheet(app = AppInfo("chat", "Chat", null), categories = emptyList(),
                        shortcuts = listOf(action), onDismiss = {}, onAddToHome = {},
                        onRename = {}, onSetCategory = {}, onHide = {}, onAppInfo = {}, onUninstall = {})
            }
        }
        composeTestRule.onNodeWithTag("action_add_shortcut_${action.id}").assertIsDisplayed()
    }

    @Test
    fun pwaShortcut_showsRemoveShortcut_notUninstall() {
        val pwa =
                AppInfo(
                        packageName = "org.mozilla.firefox",
                        label = "Twitter",
                        icon = null,
                        launcherShortcutId = "pwa-twitter",
                )

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppActionSheet(
                        app = pwa,
                        categories = emptyList(),
                        onDismiss = {},
                        onAddToHome = {},
                        onRename = {},
                        onSetCategory = {},
                        onHide = {},
                        onAppInfo = {},
                        onUninstall = {},
                        onRemoveShortcut = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("action_remove_shortcut").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("action_uninstall").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun regularApp_showsUninstall_notRemoveShortcut() {
        val app = AppInfo("com.lu4p.chrome", "Chrome", null)

        composeTestRule.setContent {
            FokusLauncherTheme {
                AppActionSheet(
                        app = app,
                        categories = emptyList(),
                        onDismiss = {},
                        onAddToHome = {},
                        onRename = {},
                        onSetCategory = {},
                        onHide = {},
                        onAppInfo = {},
                        onUninstall = {},
                        onRemoveShortcut = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("action_uninstall").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("action_remove_shortcut").fetchSemanticsNodes().also {
            assertEquals(0, it.size)
        }
    }
}
