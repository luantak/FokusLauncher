package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.theme.ChipBackground
import com.lu4p.fokuslauncher.ui.theme.ChipSelectedBackground
import java.util.Locale

/**
 * Horizontal scrollable row of category filter chips.
 */
@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onCategoryLongPress: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier.pointerInput(category) {
                    detectTapGestures(onLongPress = { onCategoryLongPress(category) })
                }
            ) {
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ChipBackground,
                        selectedContainerColor = ChipSelectedBackground,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        selectedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
