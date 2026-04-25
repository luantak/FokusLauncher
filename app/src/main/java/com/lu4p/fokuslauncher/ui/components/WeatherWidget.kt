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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.WeatherData

/**
 * Compact weather widget showing temperature and a Material Symbols weather glyph.
 * [prominent] uses [bodyLarge] for the icon and temperature so the chip reads larger while staying
 * one line (matches [DateBatteryRow] when not prominent: both use [titleMedium]).
 */
@Composable
fun WeatherWidget(
    weather: WeatherData?,
    modifier: Modifier = Modifier,
    useFahrenheit: Boolean = false,
    prominent: Boolean = false,
    outlined: Boolean = false,
    onClick: () -> Unit = {}
) {
    val suffix = if (useFahrenheit) "\u00B0F" else "\u00B0C"
    val temperatureText = weather?.let { "${it.temperature}$suffix" } ?: "--$suffix"
    val tempStyle =
            (if (prominent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium)
                    .copy(fontWeight = FontWeight.SemiBold)
    val textColor = MaterialTheme.colorScheme.onBackground
    // Slightly larger than the temperature text so the symbol reads clearly at a glance.
    val iconSize = with(LocalDensity.current) { (tempStyle.fontSize * 1.22f).toDp() }
    val iconCode = weather?.iconCode.orEmpty()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickableNoRippleWithSystemSound(onClick = onClick)
            .testTag("weather_widget")
    ) {
        LauncherIcon(
                painter = painterResource(weatherMaterialSymbolDrawableRes(iconCode)),
                contentDescription = null,
                iconSize = iconSize,
                tint = textColor,
                outlined = outlined,
        )
        Spacer(modifier = Modifier.width(if (prominent) 8.dp else 4.dp))
        if (outlined) {
            OutlinedText(
                    text = temperatureText,
                    style = tempStyle,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
            )
        } else {
            Text(
                text = temperatureText,
                style = tempStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
