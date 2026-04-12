package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

enum class IconPickerGridVariant {
    Dialog,
    Drawer,
}

@Composable
fun IconPickerSectionsLazyGrid(
        sections: List<MinimalIcons.IconPickerSection>,
        columns: GridCells,
        modifier: Modifier = Modifier,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
        verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
        variant: IconPickerGridVariant = IconPickerGridVariant.Dialog,
        isSelected: (String) -> Boolean,
        onSelect: (String) -> Unit,
) {
    LazyVerticalGrid(
            columns = columns,
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
    ) {
        sections.forEach { section ->
            val headerKey = "${section.titleRes}\u0000${section.titleLiteral ?: ""}"
            item(key = "hdr_$headerKey", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                        text =
                                section.titleLiteral
                                        ?: stringResource(section.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(section.names, key = { it }) { name ->
                when (variant) {
                    IconPickerGridVariant.Dialog ->
                        LauncherIcon(
                                imageVector = MinimalIcons.iconFor(name),
                                contentDescription = name,
                                tint =
                                        if (isSelected(name)) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                iconSize = 40.dp,
                                modifier =
                                        Modifier.clickableWithSystemSound { onSelect(name) },
                        )
                    IconPickerGridVariant.Drawer ->
                        FokusIconButton(
                                onClick = { onSelect(name) },
                                modifier = Modifier.size(48.dp),
                        ) {
                            LauncherIcon(
                                    imageVector = MinimalIcons.iconFor(name),
                                    contentDescription = name,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    iconSize = 24.dp,
                            )
                        }
                }
            }
        }
    }
}
