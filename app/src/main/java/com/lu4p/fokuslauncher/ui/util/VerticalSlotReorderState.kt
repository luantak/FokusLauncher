package com.lu4p.fokuslauncher.ui.util

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Stable
class VerticalSlotReorderState internal constructor(
        private val draggedIndexState: MutableIntState,
        private val dragOffsetState: MutableFloatState,
        val itemHeightPx: Float,
) {
    val draggedIndex: Int
        get() = draggedIndexState.intValue

    val dragOffset: Float
        get() = dragOffsetState.floatValue

    fun translationYForIndex(index: Int): Float {
        val d = draggedIndexState.intValue
        val off = dragOffsetState.floatValue
        return if (index == d) off.coerceIn(-itemHeightPx, itemHeightPx) else 0f
    }

    fun onDragStart(index: Int) {
        draggedIndexState.intValue = index
        dragOffsetState.floatValue = 0f
    }

    fun onVerticalDrag(amount: Float, lastIndex: Int, onReorder: (from: Int, to: Int) -> Unit) {
        val d = draggedIndexState.intValue
        if (d !in 0..lastIndex) return
        var off = dragOffsetState.floatValue + amount
        val (newOff, newIdx) =
                applyVerticalSlotReorder(
                        itemHeightPx,
                        off,
                        d,
                        lastIndex,
                ) { from, to -> onReorder(from, to) }
        dragOffsetState.floatValue = newOff
        draggedIndexState.intValue = newIdx
    }

    fun reset(onBeforeClear: (draggedIndex: Int) -> Unit = { _ -> }) {
        onBeforeClear(draggedIndexState.intValue)
        draggedIndexState.intValue = -1
        dragOffsetState.floatValue = 0f
    }
}

@Composable
fun rememberVerticalSlotReorderState(): VerticalSlotReorderState {
    val draggedIndexState = remember { mutableIntStateOf(-1) }
    val dragOffsetState = remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    return remember(itemHeightPx) {
        VerticalSlotReorderState(draggedIndexState, dragOffsetState, itemHeightPx)
    }
}

fun Modifier.verticalReorderDragHandle(
        state: VerticalSlotReorderState,
        index: Int,
        lastIndex: Int,
        onReorder: (from: Int, to: Int) -> Unit,
        onReset: () -> Unit,
        vararg pointerInputKeys: Any?,
): Modifier =
        this.pointerInput(*pointerInputKeys) {
            detectVerticalDragGestures(
                    onDragStart = { state.onDragStart(index) },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        state.onVerticalDrag(amount, lastIndex, onReorder)
                    },
                    onDragEnd = onReset,
                    onDragCancel = onReset,
            )
        }
