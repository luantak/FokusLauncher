package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    showDate: Boolean = true,
    showBattery: Boolean = true,
    outlined: Boolean = false,
    onDateClick: () -> Unit = {}
) {
    if (!showDate && !showBattery) return
    val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val color = MaterialTheme.colorScheme.onBackground
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (showDate) {
            if (outlined) {
                OutlinedText(
                        text = date,
                        style = style,
                        color = color,
                        modifier = Modifier.clickableNoRippleWithSystemSound(onClick = onDateClick),
                )
            } else {
                Text(
                    text = date,
                    style = style,
                    color = color,
                    modifier = Modifier.clickableNoRippleWithSystemSound(onClick = onDateClick)
                )
            }
        }
        if (showDate && showBattery) {
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showBattery) {
            if (outlined) {
                OutlinedText(text = "$batteryPercent%", style = style, color = color)
            } else {
                Text(text = "$batteryPercent%", style = style, color = color)
            }
        }
    }
}
