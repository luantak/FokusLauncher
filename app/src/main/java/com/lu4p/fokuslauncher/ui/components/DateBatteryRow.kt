package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.theme.LocalPhotoWallpaperOutlineWidthDp

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
    val backdropStrength = LocalPhotoWallpaperOutlineWidthDp.current
    val useSharedBackdrop = outlined && backdropStrength > 0f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (showDate) {
            Box(
                    contentAlignment = Alignment.TopStart,
                    modifier =
                            Modifier.heightIn(min = 56.dp)
                                    .clickableNoRippleWithSystemSound(onClick = onDateClick),
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                if (useSharedBackdrop) {
                                    Modifier.photoBackdropPill(backdropStrength)
                                } else {
                                    Modifier
                                },
                ) {
                    if (outlined && !useSharedBackdrop) {
                        OutlinedText(
                                text = date,
                                style = style,
                                color = color,
                        )
                    } else {
                        Text(
                                text = date,
                                style = style,
                                color = color,
                        )
                    }
                    if (showBattery) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (outlined && !useSharedBackdrop) {
                            OutlinedText(text = "$batteryPercent%", style = style, color = color)
                        } else {
                            Text(text = "$batteryPercent%", style = style, color = color)
                        }
                    }
                }
            }
        } else if (showBattery) {
            if (useSharedBackdrop) {
                Text(
                        text = "$batteryPercent%",
                        style = style,
                        color = color,
                        modifier = Modifier.photoBackdropPill(backdropStrength)
                )
            } else if (outlined) {
                OutlinedText(text = "$batteryPercent%", style = style, color = color)
            } else {
                Text(text = "$batteryPercent%", style = style, color = color)
            }
        }
    }
}
