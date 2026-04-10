package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.theme.launcherIconDp

/**
 * [Icon] with size in **dp** scaled by [com.lu4p.fokuslauncher.ui.theme.LocalLauncherFontScale] so
 * icons follow the launcher font size setting.
 */
@Composable
fun LauncherIcon(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = LocalContentColor.current,
        iconSize: Dp = 24.dp,
) {
    Icon(
            painter = rememberVectorPainter(imageVector),
            contentDescription = contentDescription,
            modifier = modifier.size(iconSize.launcherIconDp()),
            tint = tint,
    )
}
