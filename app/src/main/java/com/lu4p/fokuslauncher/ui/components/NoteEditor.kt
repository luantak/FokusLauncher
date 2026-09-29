package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Continue lists only for a single Enter edit, never for pasted multiline text. */
internal fun updateNoteEditorValue(previous: TextFieldValue, next: TextFieldValue): TextFieldValue {
    var result = next
    val cursor = previous.selection.start
    if (previous.selection.collapsed && next.selection.collapsed &&
            next.selection.start == cursor + 1 &&
            next.text == previous.text.replaceRange(cursor, cursor, "\n")) {
        val lineStart = previous.text.lastIndexOf('\n', cursor - 1) + 1
        val line = previous.text.substring(lineStart, cursor)
        val match = Regex("""^([ \t]*)([-*+] )(?:\[([ xX])] )?(.*)$""").matchEntire(line)
        if (match != null) {
            val (indent, bullet, checkbox, body) = match.destructured
            if (body.isBlank() &&
                    (cursor == previous.text.length || previous.text[cursor] == '\n')) {
                // Replace the empty item with a blank line and leave the cursor on it.
                result = TextFieldValue(
                        next.text.removeRange(lineStart, cursor + 1), TextRange(lineStart),
                )
            } else if (body.isNotBlank()) {
                val prefix = indent + bullet + if (checkbox.isNotEmpty()) "[ ] " else ""
                val continued = next.text.replaceRange(cursor + 1, cursor + 1, prefix)
                result = TextFieldValue(continued, TextRange(cursor + 1 + prefix.length))
            }
        }
    }
    return result
}
