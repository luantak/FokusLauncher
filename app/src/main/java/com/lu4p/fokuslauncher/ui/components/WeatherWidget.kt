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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.data.model.WeatherData

/**
 * Compact weather widget showing temperature and icon.
 * Displayed in the top-right of the home screen.
 */
@Composable
fun WeatherWidget(
    weather: WeatherData?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val weatherEmoji = weather?.weatherEmoji ?: "☁️"
    val temperatureText = weather?.let { "${it.temperature}°C" } ?: "--°C"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .testTag("weather_widget")
    ) {
        Text(
            text = weatherEmoji,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = temperatureText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
