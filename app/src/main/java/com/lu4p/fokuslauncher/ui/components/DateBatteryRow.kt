package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    isCharging: Boolean = false,
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
    val batteryPercentText = "$batteryPercent%"

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (showDate) {
            Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                            Modifier.clickableNoRippleWithSystemSound(onClick = onDateClick),
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
                        BatteryDisplay(
                            percentText = batteryPercentText,
                            isCharging = isCharging,
                            style = style,
                            color = color,
                            outlined = outlined,
                            useSharedBackdrop = useSharedBackdrop
                        )
                    }
                }
            }
        } else {
            BatteryDisplay(
                percentText = batteryPercentText,
                isCharging = isCharging,
                style = style,
                color = color,
                outlined = outlined,
                useSharedBackdrop = useSharedBackdrop,
                modifier = if (useSharedBackdrop) Modifier.photoBackdropPill(backdropStrength) else Modifier
            )
        }
    }
}

@Composable
private fun BatteryDisplay(
    percentText: String,
    isCharging: Boolean,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    outlined: Boolean,
    useSharedBackdrop: Boolean,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (outlined && !useSharedBackdrop) {
            OutlinedText(text = percentText, style = style, color = color)
            if (isCharging) {
                Spacer(modifier = Modifier.width(2.dp))
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp).offset(x = 1.dp, y = 1.dp)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Text(text = percentText, style = style, color = color)
            if (isCharging) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
