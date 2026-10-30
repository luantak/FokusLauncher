package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShortcutActionPickerDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun unlockOptionTracksProfileAvailabilityAndDispatchesInternalTarget() {
        val available = mutableStateOf(false)
        var selected: ShortcutTarget? = null
        val label = context.getString(R.string.drawer_private_space_unlock)
        compose.setContent {
            FokusLauncherTheme {
                ShortcutActionPickerDialog(
                    allActions = emptyList(), allApps = emptyList(), title = "Gesture action",
                    onSelect = { selected = it.target }, onDismiss = {},
                    includePrivateSpaceUnlockTarget = available.value,
                )
            }
        }
        compose.onNodeWithText(label).assertDoesNotExist()
        compose.runOnIdle { available.value = true }
        compose.onNodeWithText(label).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(ShortcutTarget.UnlockPrivateSpace, selected) }
        compose.runOnIdle { available.value = false }
        compose.onNodeWithText(label).assertDoesNotExist()
    }

    @Test
    fun unlockOptionRespectsSearch() {
        val label = context.getString(R.string.drawer_private_space_unlock)
        compose.setContent {
            FokusLauncherTheme {
                ShortcutActionPickerDialog(
                    allActions = emptyList(), allApps = emptyList(), title = "Gesture action",
                    onSelect = {}, onDismiss = {}, includePrivateSpaceUnlockTarget = true,
                )
            }
        }
        compose.onNodeWithText(label).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.search)).performTextInput(label.uppercase())
        compose.onNodeWithText(label).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.search)).performTextReplacement("zz-no-match")
        compose.onNodeWithText(label).assertDoesNotExist()
    }
}
