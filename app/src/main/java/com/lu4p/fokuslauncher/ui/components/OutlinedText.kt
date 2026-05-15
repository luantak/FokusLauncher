package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.lu4p.fokuslauncher.ui.theme.LocalPhotoWallpaperOutlineWidthDp

@Composable
fun OutlinedText(
        text: String,
        style: TextStyle,
        color: Color = LocalContentColor.current,
        modifier: Modifier = Modifier,
        outlineColor: Color = Color.Black,
        outlineWidth: Float = 2f,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
) {
    val outlineWidthDpSetting = LocalPhotoWallpaperOutlineWidthDp.current
    if (outlineWidthDpSetting > 0f) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                    text = text,
                    style = style,
                    color = Color.Transparent,
                    maxLines = maxLines,
                    overflow = overflow,
                    modifier =
                            Modifier.photoBackdropPill(outlineWidthDpSetting)
                                    .clearAndSetSemantics {},
            )
            Text(
                    text = text,
                    style = style,
                    color = color,
                    maxLines = maxLines,
                    overflow = overflow,
            )
        }
        return
    }

    val effectiveOutlineWidth =
            remember(outlineWidth) { outlineWidth }
    Box(modifier = modifier) {
        Text(
                text = text,
                style =
                        style.copy(
                                drawStyle =
                                        Stroke(
                                                width = effectiveOutlineWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round,
                                        )
                        ),
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
