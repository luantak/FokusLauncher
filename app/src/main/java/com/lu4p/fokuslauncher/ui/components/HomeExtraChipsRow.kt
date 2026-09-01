package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.home.HomeExtraChipUi
import com.lu4p.fokuslauncher.ui.home.HomeWidgetAlignment
import com.lu4p.fokuslauncher.ui.theme.LocalPhotoWallpaperOutlineWidthDp
import kotlin.math.max

/**
 * Ordered home extras (world-clock cities + countdown) delimited by `|`.
 * Wraps onto additional lines when needed; delimiters only appear between chips
 * on the same line (never at the start of a wrapped line).
 */
@Composable
fun HomeExtraChipsRow(
        chips: List<HomeExtraChipUi>,
        alignment: HomeWidgetAlignment = HomeWidgetAlignment.START,
        modifier: Modifier = Modifier,
        outlined: Boolean = false,
) {
    if (chips.isEmpty()) return
    val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val color = MaterialTheme.colorScheme.onBackground
    val dividerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val backdropStrength = LocalPhotoWallpaperOutlineWidthDp.current
    val useSharedBackdrop = outlined && backdropStrength > 0f
    val outlineIndividually = outlined && !useSharedBackdrop

    val labels =
            remember(chips) {
                chips.map { chip ->
                    when (chip) {
                        is HomeExtraChipUi.WorldClock ->
                                buildString {
                                    append(chip.city.label)
                                    append(' ')
                                    append(chip.city.timeText)
                                    chip.city.weatherText?.takeIf { it.isNotBlank() }?.let {
                                        append(' ')
                                        append(it)
                                    }
                                }
                        is HomeExtraChipUi.Countdown ->
                                "${chip.title} ${chip.remainingText}"
                    }
                }
            }
    val dividerSlots = (labels.size - 1).coerceAtLeast(0)

    Layout(
            content = {
                labels.forEach { label ->
                    ChipLabel(
                            text = label,
                            style = style,
                            color = color,
                            outlined = outlineIndividually,
                    )
                }
                repeat(dividerSlots) {
                    ChipLabel(
                            text = "|",
                            style = style,
                            color = dividerColor,
                            outlined = outlineIndividually,
                    )
                }
            },
            modifier =
                    modifier
                            .fillMaxWidth()
                            .then(
                                    if (useSharedBackdrop) {
                                        Modifier.photoBackdropPill(
                                                strength = backdropStrength,
                                                horizontalPaddingMin = 5.dp,
                                                horizontalPaddingMax = 18.dp,
                                                verticalPaddingMin = 2.dp,
                                                verticalPaddingMax = 8.dp,
                                        )
                                    } else {
                                        Modifier
                                    },
                            )
                            .testTag("home_extra_chips_row"),
    ) { measurables, constraints ->
        val chipCount = labels.size
        val chipPlaceables =
                measurables.take(chipCount).map { measurable ->
                    measurable.measure(Constraints())
                }
        val dividerPlaceables =
                measurables.drop(chipCount).map { measurable ->
                    measurable.measure(Constraints())
                }
        val dividerPad = 6.dp.roundToPx()
        val dividerWidth = dividerPlaceables.firstOrNull()?.width ?: 0
        val dividerSlotWidth = dividerPad + dividerWidth + dividerPad
        val maxWidth =
                if (constraints.hasBoundedWidth) constraints.maxWidth
                else
                        chipPlaceables.sumOf { it.width } +
                                dividerSlots * dividerSlotWidth

        data class Placed(val index: Int, val x: Int, val y: Int, val withLeadingDivider: Boolean)

        val placed = ArrayList<Placed>(chipCount)
        var x = 0
        var y = 0
        var rowHeight = 0
        var contentWidth = 0

        chipPlaceables.forEachIndexed { index, placeable ->
            val needsDivider = index > 0 && x > 0
            val totalWidth = placeable.width + if (needsDivider) dividerSlotWidth else 0
            if (needsDivider && x + totalWidth > maxWidth) {
                x = 0
                y += rowHeight
                rowHeight = 0
            }
            val withLeadingDivider = index > 0 && x > 0
            val chipX = if (withLeadingDivider) x + dividerSlotWidth else x
            placed += Placed(index, chipX, y, withLeadingDivider)
            x = chipX + placeable.width
            rowHeight = max(rowHeight, placeable.height)
            contentWidth = max(contentWidth, x)
        }

        val layoutWidth =
                if (constraints.hasBoundedWidth) {
                    constraints.maxWidth.coerceAtLeast(contentWidth)
                } else {
                    contentWidth.coerceAtLeast(constraints.minWidth)
                }
        val layoutHeight = (y + rowHeight).coerceAtLeast(constraints.minHeight)
        val rowWidths =
                placed.groupBy { it.y }.mapValues { (_, row) ->
                    row.maxOf { item -> item.x + chipPlaceables[item.index].width }
                }

        layout(layoutWidth, layoutHeight) {
            var dividerIndex = 0
            placed.forEach { item ->
                val placeable = chipPlaceables[item.index]
                val rowWidth = rowWidths.getValue(item.y)
                val rowOffset =
                        when (alignment) {
                            HomeWidgetAlignment.START -> 0
                            HomeWidgetAlignment.CENTER -> (layoutWidth - rowWidth) / 2
                            HomeWidgetAlignment.END -> layoutWidth - rowWidth
                        }
                if (item.withLeadingDivider) {
                    val divider = dividerPlaceables[dividerIndex++]
                    divider.placeRelative(
                            x = rowOffset + item.x - dividerPad - divider.width,
                            y = item.y + (placeable.height - divider.height) / 2,
                    )
                }
                placeable.placeRelative(rowOffset + item.x, item.y)
            }
        }
    }
}

@Composable
private fun ChipLabel(
        text: String,
        style: TextStyle,
        color: Color,
        outlined: Boolean,
) {
    if (outlined) {
        OutlinedText(text = text, style = style, color = color, maxLines = 1)
    } else {
        Text(text = text, style = style, color = color, maxLines = 1)
    }
}
