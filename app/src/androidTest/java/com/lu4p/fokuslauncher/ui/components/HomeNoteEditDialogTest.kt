package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
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
    fun clearStartsFreshNoteAndKeepsEditorOpen() {
        var saved: String? = null
        showDialog("old note") { saved = it }
        composeTestRule.onNodeWithTag("note_clear").performClick()
        composeTestRule.onNodeWithTag("note_edit_field").performTextInput("new note")
        composeTestRule.onNodeWithTag("note_edit_back").performClick()
        assertEquals("new note", saved)
    }

    @Test
    fun clearIsBottomLeftAndTaskButtonIsBottomRight() {
        showDialog("old note") {}
        val clear = composeTestRule.onNodeWithTag("note_clear").fetchSemanticsNode().boundsInRoot
        val task = composeTestRule.onNodeWithTag("note_add_task").fetchSemanticsNode().boundsInRoot
        val field = composeTestRule.onNodeWithTag("note_edit_field").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(clear.right < task.left)
        assertEquals(clear.top, task.top, 1f)
        org.junit.Assert.assertTrue(clear.top >= field.bottom)
    }

    @Test
    fun emptyNoteOpensWithSingleTap() {
        var opened = false
        composeTestRule.setContent {
            FokusLauncherTheme { NoteWidget(text = "   ", onClick = { opened = true }) }
        }
        composeTestRule.onNodeWithTag("note_widget").performClick()
        org.junit.Assert.assertTrue(opened)
    }

    @Test
    fun nonEmptyNoteStillNeedsLongPress() {
        var opened = false
        composeTestRule.setContent {
            FokusLauncherTheme { NoteWidget(text = "old note", onClick = { opened = true }) }
        }
        composeTestRule.onNodeWithTag("note_widget").performClick()
        org.junit.Assert.assertFalse(opened)
    }

    @Test
    fun failedClearKeepsDraftAndEditorOpen() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                HomeNoteEditor(initialText = "draft", onClear = { false }, onDismiss = {})
            }
        }
        composeTestRule.onNodeWithTag("note_clear").performClick()
        composeTestRule.onNodeWithTag("note_edit_field").assertTextEquals("draft")
    }

    @Test
    fun titleAndNoteTextRenderInTheSameColor() {
        showDialog("Note") {}
        fun ink(tag: String): Int {
            val pixels = composeTestRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            val background = pixels[0, 0].toArgb()
            val counts = mutableMapOf<Int, Int>()
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val color = pixels[x, y].toArgb()
                if (color != background) counts[color] = (counts[color] ?: 0) + 1
            }
            return counts.maxBy { it.value }.key
        }
        assertEquals(ink("note_edit_field"), ink("note_edit_title"))
    }

    @Test
    fun clearingAnInitiallyEmptyNoteDiscardsTextTypedSinceOpening() {
        var saved: String? = null
        showDialog("") { saved = it }
        composeTestRule.onNodeWithTag("note_edit_field").performTextInput("unsaved")
        composeTestRule.onNodeWithTag("note_clear").performClick()
        composeTestRule.onNodeWithTag("note_edit_field").assertTextEquals("")
        composeTestRule.onNodeWithTag("note_edit_back").performClick()
        assertEquals("", saved)
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
