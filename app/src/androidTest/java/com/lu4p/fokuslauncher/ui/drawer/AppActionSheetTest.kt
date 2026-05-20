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
    fun pwaShortcut_showsRemoveShortcut_notUninstall() {
        var removedShortcut: AppInfo? = null
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
                        onRemoveShortcut = { removedShortcut = it },
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
