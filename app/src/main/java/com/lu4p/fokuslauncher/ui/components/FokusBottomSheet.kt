package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private val BottomSheetScrimAlpha = 0.56f
private val BottomSheetDragHandleAlpha = 0.5f

/** Shared [ModalBottomSheet] chrome: surfaceVariant container + full-width column with bottom padding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FokusBottomSheet(
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        sheetState: SheetState = rememberModalBottomSheetState(),
        /**
         * When set, the sheet opens at this fraction of the screen height. If content is taller,
         * the sheet can be swiped up to full height; otherwise height stays fixed at the peek.
         * When null, height follows content.
         */
        contentHeightFraction: Float? = null,
        content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            scrimColor = Color.Black.copy(alpha = BottomSheetScrimAlpha),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                        color =
                                MaterialTheme.colorScheme.primary.copy(
                                        alpha = BottomSheetDragHandleAlpha,
                                ),
                )
            },
            modifier = modifier,
    ) {
        if (contentHeightFraction != null) {
            PeekExpandableSheetContent(
                    peekHeightFraction = contentHeightFraction,
                    content = content,
            )
        } else {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 32.dp),
                    content = content,
            )
        }
    }
}

/**
 * Peek-height sheet body: fixed open height, swipe-to-expand only when content overflows the peek.
 *
 * Overflowing content requests the full available height so [ModalBottomSheet] exposes its
 * partially-expanded (open) and fully-expanded anchors.
 */
@Composable
private fun PeekExpandableSheetContent(
        peekHeightFraction: Float,
        content: @Composable ColumnScope.() -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val peekHeight = screenHeight * peekHeightFraction
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val fullHeight = if (constraints.hasBoundedHeight) maxHeight else screenHeight

        SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
            val maxBodyPx =
                    if (constraints.hasBoundedHeight) constraints.maxHeight
                    else fullHeight.roundToPx()
            val peekPx = peekHeight.roundToPx().coerceAtMost(maxBodyPx)
            val fullPx = fullHeight.roundToPx().coerceAtMost(maxBodyPx)

            val contentHeight =
                    subcompose("measure") {
                                Column(modifier = Modifier.fillMaxWidth(), content = content)
                            }
                            .sumOf {
                                it.measure(
                                                constraints.copy(
                                                        minHeight = 0,
                                                        maxHeight = Constraints.Infinity,
                                                ),
                                        )
                                        .height
                            }

            val overflowsPeek = contentHeight > peekPx
            val bodyHeight = if (overflowsPeek) fullPx else peekPx

            val placeable =
                    subcompose("body") {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(bodyHeight.toDp())
                                                        .verticalScroll(scrollState)
                                                        .padding(bottom = 32.dp),
                                        content = content,
                                )
                            }
                            .first()
                            .measure(
                                    constraints.copy(
                                            minHeight = bodyHeight,
                                            maxHeight = bodyHeight,
                                    ),
                            )

            layout(placeable.width, bodyHeight) { placeable.place(0, 0) }
        }
    }
}
