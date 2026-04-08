package com.lu4p.fokuslauncher.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * [AlertDialog] with launcher styling defaults ([containerColor] = surfaceVariant). Title and text
 * should use [androidx.compose.material3.MaterialTheme.colorScheme.onBackground] where needed.
 */
@Composable
fun FokusAlertDialog(
        onDismissRequest: () -> Unit,
        title: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        text: @Composable (() -> Unit)? = null,
        confirmButton: @Composable () -> Unit = {},
        dismissButton: @Composable () -> Unit,
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            containerColor = containerColor,
    )
}
