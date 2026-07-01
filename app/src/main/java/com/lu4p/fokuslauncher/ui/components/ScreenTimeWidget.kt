package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.theme.LocalPhotoWallpaperOutlineWidthDp

/**
 * Compact screen time chip showing total device use in the past 24 hours.
 * Styling matches [WeatherWidget] when not prominent.
 */
@Composable
fun ScreenTimeWidget(
    durationText: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    onClick: () -> Unit = {},
) {
    val textStyle =
            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val textColor = MaterialTheme.colorScheme.onBackground
    val backdropStrength = LocalPhotoWallpaperOutlineWidthDp.current
    val useSharedBackdrop = outlined && backdropStrength > 0f
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    modifier
                            .clickableNoRippleWithSystemSound(onClick = onClick)
                            .then(
                                    if (useSharedBackdrop) {
                                        Modifier.photoBackdropPill(
                                                strength = backdropStrength,
                                                horizontalPaddingMin = 5.dp,
                                                horizontalPaddingMax = 18.dp,
                                                verticalPaddingMin = 2.dp,
                                                verticalPaddingMax = 8.dp,
                                        )
                                    } else {
                                        Modifier
                                    },
                            )
                            .testTag("screen_time_widget"),
    ) {
        if (outlined && !useSharedBackdrop) {
            OutlinedText(
                    text = durationText,
                    style = textStyle,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
            )
        } else {
            Text(
                    text = durationText,
                    style = textStyle,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
            )
        }
    }
}
