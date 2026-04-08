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
    useFahrenheit: Boolean = false,
    onClick: () -> Unit = {}
) {
    val suffix = if (useFahrenheit) "°F" else "°C"
    val weatherEmoji = weather?.weatherEmoji ?: "☁️"
    val temperatureText = weather?.let { "${it.temperature}$suffix" } ?: "--$suffix"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickableNoRippleWithSystemSound(onClick = onClick)
            .testTag("weather_widget")
    ) {
        Text(text = weatherEmoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = temperatureText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
