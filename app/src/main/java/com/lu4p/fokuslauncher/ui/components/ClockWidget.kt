package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Large clock display showing current time in "H:mm" format.
 * Clicking opens the clock / alarm app.
 */
@Composable
fun ClockWidget(
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Text(
        text = time,
        style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Light,
            fontSize = 80.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
    )
}
