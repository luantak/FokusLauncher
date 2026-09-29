package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditorTest {
    private fun enter(text: String, cursor: Int = text.length): TextFieldValue =
            updateNoteEditorValue(
                    TextFieldValue(text, TextRange(cursor)),
                    TextFieldValue(text.replaceRange(cursor, cursor, "\n"), TextRange(cursor + 1)),
            )

    @Test
    fun enterContinuesBulletsAndIndentation() {
        for (marker in listOf("-", "*", "+")) {
            val result = enter("  $marker milk")
            assertEquals("  $marker milk\n  $marker ", result.text)
            assertEquals(TextRange(result.text.length), result.selection)
        }
    }

    @Test
    fun enterContinuesTasksAsUnchecked() {
        for (mark in listOf(" ", "x", "X")) {
            assertEquals("- [$mark] milk\n- [ ] ", enter("- [$mark] milk").text)
        }
    }

    @Test
    fun enterEndsAnEmptyListItem() {
        for (prefix in listOf("- ", "  * ", "- [ ] ", "+ [x] ")) {
            assertEquals("milk\n", enter("milk\n$prefix").text)
            assertEquals("", enter(prefix).text)
        }
    }

    @Test
    fun enterInMiddleOfItemKeepsRemainingText() {
        assertEquals("- milk\n-  and eggs", enter("- milk and eggs", 6).text)
    }

    @Test
    fun pasteDoesNotContinueLists() {
        val previous = TextFieldValue("- milk", TextRange(6))
        val pasted = TextFieldValue("- milk\neggs\nbread", TextRange(17))
        assertEquals(pasted, updateNoteEditorValue(previous, pasted))
    }

    @Test
    fun longNotesAreNotTruncated() {
        val text = "a".repeat(100_000)
        val pasted = TextFieldValue(text, TextRange(text.length))
        assertEquals(pasted, updateNoteEditorValue(TextFieldValue(""), pasted))
        val inserted = TextFieldValue(text.replaceRange(10, 10, "b"), TextRange(11))
        assertEquals(inserted, updateNoteEditorValue(pasted, inserted))
    }
}
