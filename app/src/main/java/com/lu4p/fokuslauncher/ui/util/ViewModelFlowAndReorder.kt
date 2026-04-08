package com.lu4p.fokuslauncher.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

fun <T> Flow<T>.stateWhileSubscribedIn(
        scope: CoroutineScope,
        initial: T,
        stopTimeoutMs: Long = 5_000L,
): StateFlow<T> = stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMs), initial)

fun <T> Flow<T>.stateEagerlyIn(scope: CoroutineScope, initial: T): StateFlow<T> =
        stateIn(scope, SharingStarted.Eagerly, initial)

/**
 * Coerces vertical drag into ±[itemHeightPx] steps; [swap] runs for each adjacent move.
 *
 * @param lastIndex inclusive last valid index (typically `list.lastIndex`)
 */
inline fun applyVerticalSlotReorder(
        itemHeightPx: Float,
        dragOffset: Float,
        draggedIndex: Int,
        lastIndex: Int,
        crossinline swap: (from: Int, to: Int) -> Unit,
): Pair<Float, Int> {
    var off = dragOffset
    var idx = draggedIndex
    while (off >= itemHeightPx && idx < lastIndex) {
        val from = idx
        val to = idx + 1
        swap(from, to)
        idx = to
        off -= itemHeightPx
    }
    while (off <= -itemHeightPx && idx > 0) {
        val from = idx
        val to = idx - 1
        swap(from, to)
        idx = to
        off += itemHeightPx
    }
    return off to idx
}
