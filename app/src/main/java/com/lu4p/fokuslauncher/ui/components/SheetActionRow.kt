package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import androidx.compose.ui.unit.dp

/**
 * Icon + label row for bottom sheets and similar menus (system sound on click by default).
 * Provide [icon] or [leadingContent].
 */
@Composable
fun SheetActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconContentDescription: String? = label,
    testTag: String? = null,
    destructive: Boolean = false,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    labelModifier: Modifier = Modifier,
) {
    require(icon != null || leadingContent != null) {
        "SheetActionRow requires icon or leadingContent"
    }
    val tint =
        if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickableWithSystemSound(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        when {
            leadingContent != null -> leadingContent()
            else ->
                LauncherIcon(
                    imageVector = icon!!,
                    contentDescription = iconContentDescription,
                    tint = tint,
                    iconSize = 24.dp,
                )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = labelModifier,
        )
    }
}
