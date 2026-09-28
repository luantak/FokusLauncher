package com.lu4p.fokuslauncher.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.lu4p.fokuslauncher.data.local.TwoFingerDirection
import kotlin.math.abs

/** A swipe requires both fingers to travel together. Pinches and single-finger drags are ignored. */
internal fun twoFingerSwipeDirection(first: Offset, second: Offset, threshold: Float): TwoFingerDirection? {
    val dx = (first.x + second.x) / 2f
    val dy = (first.y + second.y) / 2f
    val horizontal = abs(dx) > abs(dy)
    val primary = if (horizontal) dx else dy
    val a = if (horizontal) first.x else first.y
    val b = if (horizontal) second.x else second.y
    if (abs(primary) < threshold || a * b <= 0f ||
        abs(a) < threshold / 2f || abs(b) < threshold / 2f ||
        abs(if (horizontal) first.y - second.y else first.x - second.x) > threshold
    ) return null
    return if (horizontal) {
        if (primary > 0f) TwoFingerDirection.RIGHT else TwoFingerDirection.LEFT
    } else {
        if (primary > 0f) TwoFingerDirection.DOWN else TwoFingerDirection.UP
    }
}

@Composable
internal fun Modifier.detectTwoFingerSwipes(
    onTwoFingersActive: (Boolean) -> Unit,
    onSwipe: (TwoFingerDirection) -> Unit,
): Modifier {
    val activeCallback = rememberUpdatedState(onTwoFingersActive)
    val swipeCallback = rememberUpdatedState(onSwipe)
    return pointerInput(Unit) {
    val threshold = 72f * density
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var starts: Map<androidx.compose.ui.input.pointer.PointerId, Offset>? = null
        var fired = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                if (pressed.size > 2 || (starts != null && pressed.size != 2)) break
                if (starts == null && pressed.size == 2) {
                    starts = pressed.associate { it.id to it.position }
                    activeCallback.value(true)
                }
                val origin = starts ?: continue
                if (!fired && pressed.size == 2 && pressed.all { it.id in origin }) {
                    val first = pressed[0].position - origin.getValue(pressed[0].id)
                    val second = pressed[1].position - origin.getValue(pressed[1].id)
                    val direction = twoFingerSwipeDirection(first, second, threshold)
                    if (direction != null) {
                        fired = true
                        pressed.forEach { it.consume() }
                        swipeCallback.value(direction)
                    }
                }
                if (fired) pressed.forEach { it.consume() }
            }
        } finally {
            activeCallback.value(false)
        }
    }
}
}
