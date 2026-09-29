package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMarkdownTest {

    /** Text covered by each span, so assertions don't depend on span ordering or offsets. */
    private fun AnnotatedString.spanTexts(): List<Pair<String, SpanStyle>> =
            spanStyles.map { text.substring(it.start, it.end) to it.item }

    @Test
    fun plain_text_is_unchanged() {
        val rendered = renderNoteMarkdown("milk\neggs")
        assertEquals("milk\neggs", rendered.text)
        assertTrue(rendered.spanStyles.isEmpty())
    }

    @Test
    fun bullets_render_as_dots_and_keep_indent() {
        assertEquals(
                "• milk\n• eggs\n  • free range",
                renderNoteMarkdown("- milk\n* eggs\n  + free range").text,
        )
    }

    @Test
    fun tasks_render_as_checkboxes_and_strike_done_items() {
        val rendered = renderNoteMarkdown("- [ ] call mum\n- [x] buy milk\n* [X] pay rent")
        assertEquals("☐ call mum\n☑ buy milk\n☑ pay rent", rendered.text)
        val struck =
                rendered.spanTexts()
                        .filter { it.second.textDecoration == TextDecoration.LineThrough }
                        .map { it.first }
        assertEquals(listOf("buy milk", "pay rent"), struck)
    }

    @Test
    fun headings_are_bold_and_drop_hashes() {
        val rendered = renderNoteMarkdown("# Today\n### Later")
        assertEquals("Today\nLater", rendered.text)
        val bold =
                rendered.spanTexts()
                        .filter { it.second.fontWeight == FontWeight.Bold }
                        .map { it.first }
        assertEquals(listOf("Today", "Later"), bold)
    }

    @Test
    fun hashtag_without_space_is_not_a_heading() {
        assertEquals("#groceries", renderNoteMarkdown("#groceries").text)
    }

    @Test
    fun inline_emphasis_is_styled_and_markers_removed() {
        val rendered = renderNoteMarkdown("**bold** *italic* ~~gone~~")
        assertEquals("bold italic gone", rendered.text)
        val spans = rendered.spanTexts().toMap()
        assertEquals(FontWeight.Bold, spans.getValue("bold").fontWeight)
        assertEquals(FontStyle.Italic, spans.getValue("italic").fontStyle)
        assertEquals(TextDecoration.LineThrough, spans.getValue("gone").textDecoration)
    }

    @Test
    fun bold_nested_inside_italic() {
        val rendered = renderNoteMarkdown("*a **b** c*")
        assertEquals("a b c", rendered.text)
        val spans = rendered.spanTexts()
        assertTrue(spans.any { it.first == "a b c" && it.second.fontStyle == FontStyle.Italic })
        assertTrue(spans.any { it.first == "b" && it.second.fontWeight == FontWeight.Bold })
    }

    @Test
    fun inline_emphasis_works_inside_list_items() {
        val rendered = renderNoteMarkdown("- [ ] **urgent** call")
        assertEquals("☐ urgent call", rendered.text)
        assertEquals(FontWeight.Bold, rendered.spanTexts().toMap().getValue("urgent").fontWeight)
    }

    @Test
    fun unmatched_or_spaced_markers_stay_literal() {
        assertEquals("2 * 3 * 4", renderNoteMarkdown("2 * 3 * 4").text)
        assertEquals("a ** b", renderNoteMarkdown("a ** b").text)
        assertEquals("*open", renderNoteMarkdown("*open").text)
        assertEquals("~~open", renderNoteMarkdown("~~open").text)
        assertTrue(renderNoteMarkdown("2 * 3 * 4").spanStyles.isEmpty())
    }

    @Test
    fun backslash_escapes_markers() {
        val rendered = renderNoteMarkdown("\\*not italic\\* and \\- not a bullet")
        assertEquals("*not italic* and - not a bullet", rendered.text)
        assertTrue(rendered.spanStyles.isEmpty())
        assertEquals("- literal", renderNoteMarkdown("\\- literal").text)
    }

    @Test
    fun rendered_text_keeps_one_line_per_source_line() {
        val source = "# Title\n- [ ] **a**\n\n\\- b\n  - c\n~~d~~"
        val rendered = renderNoteMarkdown(source).text
        assertEquals(source.split('\n').size, rendered.split('\n').size)
    }

    @Test
    fun source_line_at_offset_counts_preceding_newlines() {
        val rendered = renderNoteMarkdown("milk\n- [ ] eggs\nbread").text
        assertEquals(0, noteSourceLineAt(rendered, 0))
        assertEquals(0, noteSourceLineAt(rendered, 4))
        assertEquals(1, noteSourceLineAt(rendered, 5))
        assertEquals(1, noteSourceLineAt(rendered, rendered.indexOf("eggs")))
        assertEquals(2, noteSourceLineAt(rendered, rendered.length))
        assertEquals(2, noteSourceLineAt(rendered, rendered.length + 10))
    }

    @Test
    fun task_lines_are_detected_by_source_index() {
        val source = "# Today\n- [ ] milk\n- eggs\n  * [x] bread"
        assertEquals(listOf(1, 3), noteTaskLines(source))
        assertTrue(isNoteTaskLine(source, 1))
        assertTrue(!isNoteTaskLine(source, 2))
        assertTrue(!isNoteTaskLine(source, 9))
    }

    @Test
    fun toggle_flips_only_the_target_task() {
        val source = "# Today\n- [ ] milk\n  * [X] bread\n- eggs"
        assertEquals("# Today\n- [x] milk\n  * [X] bread\n- eggs", toggleNoteTask(source, 1))
        assertEquals("# Today\n- [ ] milk\n  * [ ] bread\n- eggs", toggleNoteTask(source, 2))
    }

    @Test
    fun toggle_twice_restores_the_task() {
        val source = "- [ ] milk"
        assertEquals(source, toggleNoteTask(toggleNoteTask(source, 0), 0))
    }

    @Test
    fun toggle_ignores_non_task_and_missing_lines() {
        val source = "# Today\n- eggs"
        assertEquals(source, toggleNoteTask(source, 0))
        assertEquals(source, toggleNoteTask(source, 1))
        assertEquals(source, toggleNoteTask(source, 5))
    }

    @Test
    fun toggle_only_changes_the_checkbox_not_matching_body_text() {
        assertEquals("- [x] fix [ ] parser", toggleNoteTask("- [ ] fix [ ] parser", 0))
    }

    @Test
    fun insert_task_into_empty_note_fills_first_line() {
        assertEquals("- [ ] " to 6, insertNoteTaskLine("", 0))
    }

    @Test
    fun insert_task_at_end_adds_a_new_line() {
        assertEquals("milk\n- [ ] " to 11, insertNoteTaskLine("milk", 4))
    }

    @Test
    fun insert_task_mid_line_goes_after_the_cursor_line() {
        assertEquals(
                "milk\n- [ ] \neggs" to 11,
                insertNoteTaskLine("milk\neggs", 2),
        )
    }

    @Test
    fun insert_task_reuses_a_blank_cursor_line() {
        assertEquals("milk\n- [ ] \neggs" to 11, insertNoteTaskLine("milk\n\neggs", 5))
        assertEquals("milk\n- [ ] " to 11, insertNoteTaskLine("milk\n   ", 7))
    }

    @Test
    fun insert_task_after_an_unfinished_task_starts_another() {
        assertEquals("- [ ] \n- [ ] " to 13, insertNoteTaskLine("- [ ] ", 6))
    }

    @Test
    fun insert_task_clamps_an_out_of_range_cursor() {
        assertEquals("milk\n- [ ] " to 11, insertNoteTaskLine("milk", 99))
    }

    @Test
    fun spans_never_set_a_color() {
        val rendered = renderNoteMarkdown("# H\n- [x] **b** *i* ~~s~~")
        assertTrue(rendered.spanStyles.isNotEmpty())
        rendered.spanStyles.forEach { assertEquals(Color.Unspecified, it.item.color) }
    }
}
