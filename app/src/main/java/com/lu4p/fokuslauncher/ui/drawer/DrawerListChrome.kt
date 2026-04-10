package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun DrawerListSectionDivider(
        modifier: Modifier = Modifier,
        horizontalPadding: Dp = 24.dp,
) {
    HorizontalDivider(
            modifier = modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun DrawerListSectionHeader(
        text: String,
        modifier: Modifier = Modifier,
        horizontalPadding: Dp = 24.dp,
) {
    Text(
            text = text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
    )
}
