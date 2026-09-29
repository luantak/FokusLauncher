package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNoteEditDialogTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun showDialog(initialText: String, onSave: (String) -> Unit) {
        var draft = initialText
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeNoteEditor(
                        initialText = initialText,
                        onDraftChange = { draft = it },
                        onDismiss = { onSave(draft) },
                )
            }
        }
    }

    @Test
    fun addTaskButton_startsTaskOnNewLineAndKeepsTyping() {
        var saved: String? = null
        showDialog("milk") { saved = it }

        composeTestRule.onNodeWithTag("note_add_task").performClick()
        composeTestRule.onNodeWithTag("note_edit_field").performTextInput("eggs")
        composeTestRule.onNodeWithTag("note_add_task").performClick()
        composeTestRule.onNodeWithTag("note_edit_field").performTextInput("bread")
        composeTestRule.onNodeWithTag("note_edit_back").performClick()

        assertEquals("milk\n- [ ] eggs\n- [ ] bread", saved)
    }

    @Test
    fun addTaskButton_onEmptyNote_doesNotLeaveBlankFirstLine() {
        var saved: String? = null
        showDialog("") { saved = it }

        composeTestRule.onNodeWithTag("note_add_task").performClick()
        composeTestRule.onNodeWithTag("note_edit_field").performTextInput("call mum")
        composeTestRule.onNodeWithTag("note_edit_back").performClick()

        assertEquals("- [ ] call mum", saved)
    }
    @Test
    fun leavingSavesTheEditedNote() {
        var saved: String? = null
        showDialog("milk") { saved = it }
        composeTestRule.onNodeWithTag("note_edit_field").performTextInput(" and eggs")
        composeTestRule.onNodeWithTag("note_edit_back").performClick()
        assertEquals("milk and eggs", saved)
    }
}
