package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun OutlinedText(
        text: String,
        style: TextStyle,
        color: Color = LocalContentColor.current,
        modifier: Modifier = Modifier,
        outlineColor: Color = Color.Black.copy(alpha = 0.64f),
        outlineWidth: Float = 2f,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
) {
    Box(modifier = modifier) {
        Text(
                text = text,
                style = style.copy(drawStyle = Stroke(width = outlineWidth)),
                color = outlineColor,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
                text = text,
                style = style,
                color = color,
                maxLines = maxLines,
                overflow = overflow,
        )
    }
}
