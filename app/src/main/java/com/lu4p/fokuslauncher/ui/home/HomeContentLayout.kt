package com.lu4p.fokuslauncher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp

internal enum class HomeContentSlot { Note, Favorites, Gap }

/** Reserve favorites and other widgets before measuring the note's flexible preview. */
@Composable
internal fun HomeContentLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        val gaps = measurables.indices.filter { measurables[it].layoutId == HomeContentSlot.Gap }
        val note = measurables.indexOfFirst { it.layoutId == HomeContentSlot.Note }
        val favorites = measurables.indexOfFirst { it.layoutId == HomeContentSlot.Favorites }
        var remainingHeight = constraints.maxHeight

        fun measure(index: Int, maxHeight: Int = remainingHeight) {
            val placeable = measurables[index].measure(
                    constraints.copy(minHeight = 0, maxHeight = maxHeight),
            )
            placeables[index] = placeable
            remainingHeight = (remainingHeight - placeable.height).coerceAtLeast(0)
        }

        if (favorites >= 0) measure(favorites)
        measurables.indices.forEach { index ->
            if (index != favorites && index != note && index !in gaps) measure(index)
        }
        if (note >= 0) {
            // Keep some breathing room between the preview/widgets and the app list.
            val reservedGap = (16.dp.roundToPx() * gaps.size).coerceAtMost(remainingHeight)
            measure(note, remainingHeight - reservedGap)
        }

        val gapHeights = IntArray(measurables.size)
        gaps.forEachIndexed { index, gap ->
            gapHeights[gap] = remainingHeight / gaps.size +
                    if (index < remainingHeight % gaps.size) 1 else 0
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                if (placeable != null) {
                    placeable.placeRelative(0, y)
                    y += placeable.height
                } else {
                    y += gapHeights[index]
                }
            }
        }
    }
}
