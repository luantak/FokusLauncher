package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import androidx.compose.ui.unit.dp

/** Icon + label row for bottom sheets and similar menus (system sound on click by default). */
@Composable
fun SheetActionRow(
        icon: ImageVector,
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        iconContentDescription: String? = label,
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    modifier
                            .fillMaxWidth()
                            .clickableWithSystemSound(onClick = onClick)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
        )
    }
}
