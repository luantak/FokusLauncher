package com.lu4p.fokuslauncher.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel
import com.lu4p.fokuslauncher.ui.util.resolvedCategoryDrawerIconName
private const val LONG_PRESS_MS = 450L

/**
 * Vertical category strip: equal-height slots, full-height sidebar gestures
 * (tap / long-press / vertical drag), indicator bar on the active slot, and nested-scroll
 * consumption so pull-down-to-close does not activate from this strip.
 *
 * Icons are purely visual; a transparent overlay on top receives all touches (children would
 * otherwise win hit-testing and a plain [pointerInput] on the outer [Box] would never run).
 */
@Composable
fun DrawerCategorySidebar(
        categories: List<String>,
        selectedCategory: String,
        onCategorySelected: (String) -> Unit,
        onCategoryLongPress: (String) -> Unit,
        modifier: Modifier = Modifier,
        /** When true, the rail is along the start edge; selection bar sits on the end (toward list). */
        sidebarOnLeft: Boolean = false,
        categoryIconOverrides: Map<String, String> = emptyMap(),
) {
    val context = LocalContext.current
    val viewConfig = LocalViewConfiguration.current
    val slop = viewConfig.touchSlop
    val latestCategories = rememberUpdatedState(categories)
    val latestSelectedCategory = rememberUpdatedState(selectedCategory)
    val latestOnCategorySelected = rememberUpdatedState(onCategorySelected)
    val latestOnCategoryLongPress = rememberUpdatedState(onCategoryLongPress)

    val consumeDrawerPullDown =
            remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                    ): Offset {
                        if (source == NestedScrollSource.UserInput && available.y > 0f) {
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }
                }
            }

    val sidebarBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val iconTint = MaterialTheme.colorScheme.onSurface
    val indicator = MaterialTheme.colorScheme.primary
    val indicatorAlignment = if (sidebarOnLeft) Alignment.CenterEnd else Alignment.CenterStart

    Box(
            modifier =
                    modifier.fillMaxHeight()
                            .nestedScroll(consumeDrawerPullDown)
                            .background(sidebarBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            categories.forEach { category ->
                val selected = category.equals(selectedCategory, ignoreCase = true)
                Box(
                        Modifier.weight(1f).fillMaxWidth()
                ) {
                    Icon(
                            imageVector =
                                    MinimalIcons.iconFor(
                                            resolvedCategoryDrawerIconName(
                                                    context,
                                                    category,
                                                    categoryIconOverrides
                                            )
                                    ),
                            contentDescription = categoryChipDisplayLabel(context, category),
                            tint = iconTint,
                            modifier =
                                    Modifier.align(Alignment.Center)
                                            .padding(vertical = 4.dp)
                    )
                    if (selected) {
                        Box(
                                Modifier.align(indicatorAlignment)
                                        .fillMaxHeight()
                                        .width(3.dp)
                                        .background(indicator)
                        )
                    }
                }
            }
        }

        Box(
                Modifier.matchParentSize()
                        .pointerInput(categories, slop) {
                            val count = latestCategories.value.size
                            if (count == 0) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val pointerId = down.id
                                val startY = down.position.y
                                val startTime = SystemClock.uptimeMillis()
                                var dragAccum = Offset.Zero
                                val hInitial = size.height.toFloat()
                                val initialIdx = indexFromY(startY, hInitial, count)
                                var lastTrackedIdx = initialIdx
                                val initialCategory = latestCategories.value[initialIdx]
                                if (!initialCategory.equals(
                                                latestSelectedCategory.value,
                                                ignoreCase = true
                                        )
                                ) {
                                    latestOnCategorySelected.value(initialCategory)
                                }
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change =
                                            event.changes.firstOrNull { it.id == pointerId }
                                                    ?: break
                                    dragAccum += change.positionChange()
                                    change.consume()
                                    val h = size.height.toFloat()
                                    if (change.pressed) {
                                        val idx = indexFromY(change.position.y, h, count)
                                        if (idx != lastTrackedIdx) {
                                            lastTrackedIdx = idx
                                            latestOnCategorySelected.value(
                                                    latestCategories.value[idx]
                                            )
                                        }
                                    } else {
                                        val elapsed =
                                                SystemClock.uptimeMillis() - startTime
                                        val dist = dragAccum.getDistance()
                                        when {
                                            dist < slop && elapsed >= LONG_PRESS_MS -> {
                                                val idx = indexFromY(startY, h, count)
                                                latestOnCategoryLongPress.value(
                                                        latestCategories.value[idx]
                                                )
                                            }
                                            dist < slop -> {
                                                val idx = indexFromY(startY, h, count)
                                                latestOnCategorySelected.value(
                                                        latestCategories.value[idx]
                                                )
                                            }
                                            else -> {
                                                val idx =
                                                        indexFromY(change.position.y, h, count)
                                                if (idx != lastTrackedIdx) {
                                                    latestOnCategorySelected.value(
                                                            latestCategories.value[idx]
                                                    )
                                                }
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                        .testTag("category_rail")
        )
    }
}

private fun indexFromY(y: Float, height: Float, count: Int): Int {
    if (height <= 0f || count <= 0) return 0
    return ((y / height) * count).toInt().coerceIn(0, count - 1)
}
