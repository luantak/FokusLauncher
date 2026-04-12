package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                content = content,
        )
    }
}
