package com.lu4p.fokuslauncher.ui.widgets

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import android.util.SizeF
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.HostedWidget
import com.lu4p.fokuslauncher.data.model.clampHostedWidgetHeightDp
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HostedWidgetView(
        widget: HostedWidget,
        editMode: Boolean,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        createView: (Int) -> android.view.View?,
        onLongPress: () -> Unit,
        onRemove: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        onResize: (Int) -> Unit,
        modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    var localHeightDp by remember(widget.id) { mutableIntStateOf(widget.heightDp) }
    var widthDp by remember(widget.id) { mutableIntStateOf(0) }

    LaunchedEffect(widget.heightDp) {
        localHeightDp = widget.heightDp
    }

    Column(
            modifier = modifier.fillMaxWidth(),
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(localHeightDp.dp)
                                .onSizeChanged { size ->
                                    widthDp = with(density) { size.width.toDp().value.toInt() }
                                }
                                .observeLongPress(
                                        key = widget.id,
                                        timeoutMillis = viewConfiguration.longPressTimeoutMillis,
                                        touchSlop = viewConfiguration.touchSlop,
                                        onLongPress = onLongPress,
                                )
                                .then(
                                        if (editMode) {
                                            Modifier.clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                            MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.14f
                                                            )
                                                    )
                                                    .padding(2.dp)
                                        } else {
                                            Modifier
                                        }
                                )
        ) {
            AndroidView(
                    factory = {
                        createView(widget.appWidgetId)
                                ?: TextView(context).apply {
                                    text = widget.label
                                    gravity = Gravity.CENTER
                                    setTextColor(android.graphics.Color.WHITE)
                                    layoutParams =
                                            FrameLayout.LayoutParams(
                                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                            )
                                }
                    },
                    update = { view ->
                        applyWidgetSize(
                                view = view,
                                widthDp = widthDp,
                                heightDp = localHeightDp,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(localHeightDp.dp),
            )

            if (editMode) {
                Box(
                        modifier =
                                Modifier.matchParentSize()
                                        .pointerInput(widget.id) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    // Keep the AndroidView out of the hit path while still
                                                    // leaving scroll deltas available to the LazyColumn.
                                                    awaitPointerEvent()
                                                }
                                            }
                                        }
                )
            }
        }

        if (editMode) {
            Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .padding(top = 8.dp),
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    EditBarIconButton(onClick = onRemove, enabled = true) {
                        Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.remove_widget),
                                tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                    Modifier.weight(1f)
                                            .height(44.dp)
                                            .pointerInput(widget.id) {
                                                var dragHeightDp = widget.heightDp.toFloat()
                                                detectVerticalDragGestures(
                                                        onDragStart = {
                                                            dragHeightDp = localHeightDp.toFloat()
                                                        },
                                                        onVerticalDrag = { change, dragAmount ->
                                                            change.consume()
                                                            val deltaDp =
                                                                    with(density) {
                                                                        dragAmount.toDp().value
                                                                    }
                                                            dragHeightDp += deltaDp
                                                            localHeightDp =
                                                                    clampHostedWidgetHeightDp(
                                                                            dragHeightDp.toInt()
                                                                    )
                                                        },
                                                        onDragEnd = { onResize(localHeightDp) },
                                                        onDragCancel = {
                                                            localHeightDp = widget.heightDp
                                                        },
                                                )
                                            },
                    ) {
                        Icon(
                                Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.resize_widget),
                        )
                    }
                    EditBarIconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                    EditBarIconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditBarIconButton(
        onClick: () -> Unit,
        enabled: Boolean,
        content: @Composable () -> Unit,
) {
    IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(44.dp),
            content = content,
    )
}

private fun applyWidgetSize(view: android.view.View, widthDp: Int, heightDp: Int) {
    if (view !is AppWidgetHostView || widthDp <= 0 || heightDp <= 0) return
    view.updateAppWidgetSize(
            Bundle(),
            listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
    )
}

private fun Modifier.observeLongPress(
        key: String,
        timeoutMillis: Long,
        touchSlop: Float,
        onLongPress: () -> Unit,
): Modifier =
        pointerInput(key, timeoutMillis, touchSlop) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent(PointerEventPass.Initial)
                            .changes
                            .firstOrNull { it.pressed }
                            ?: continue
                    val pointerId = down.id
                    val startPosition = down.position
                    var cancelled = false
                    val completedBeforeTimeout =
                            withTimeoutOrNull(timeoutMillis) {
                                while (!cancelled) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change =
                                            event.changes.firstOrNull { it.id == pointerId }
                                                    ?: run {
                                                        cancelled = true
                                                        break
                                                    }
                                    if (change.changedToUpIgnoreConsumed() ||
                                                    change.isOutOfBounds(size, extendedTouchPadding)
                                    ) {
                                        cancelled = true
                                        break
                                    }
                                    if ((change.position - startPosition).getDistance() > touchSlop ||
                                                    change.positionChange().getDistance() > touchSlop
                                    ) {
                                        cancelled = true
                                        break
                                    }
                                }
                                Unit
                            }
                    if (completedBeforeTimeout == null && !cancelled) {
                        onLongPress()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
        }
