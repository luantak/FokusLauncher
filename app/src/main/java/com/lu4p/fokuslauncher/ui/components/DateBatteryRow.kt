package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Row displaying the current date and battery percentage.
 * Tapping the date opens the calendar app.
 */
@Composable
fun DateBatteryRow(
    date: String,
    batteryPercent: Int,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDateClick)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$batteryPercent%",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
