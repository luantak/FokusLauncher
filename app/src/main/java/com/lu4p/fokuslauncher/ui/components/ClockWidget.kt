package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * Large clock display showing the current time using the system time format (12h with AM/PM or 24h).
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
        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.clickableWithSystemSound(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
    )
}
