package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.util.categoryChipDisplayLabel
import com.lu4p.fokuslauncher.ui.util.rememberClickWithSystemSound
import com.lu4p.fokuslauncher.ui.theme.ChipBackground

@Composable
fun CategoryChips(
        categories: List<String>,
        selectedCategory: String,
        onCategorySelected: (String) -> Unit,
        modifier: Modifier = Modifier,
        onCategoryLongPress: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedCategory, categories.size) {
        val index = categories.indexOfFirst { it.equals(selectedCategory, ignoreCase = true) }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            modifier = modifier
    ) {
        items(items = categories, key = { it }) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            val (chipTypography, chipWeight) =
                    if (isSelected) {
                        MaterialTheme.typography.labelLarge to FontWeight.SemiBold
                    } else {
                        MaterialTheme.typography.labelMedium to FontWeight.Normal
                    }
            Box(
                    modifier =
                            Modifier.pointerInput(category) {
                                detectTapGestures(onLongPress = { onCategoryLongPress(category) })
                            }
            ) {
                FilterChip(
                        selected = isSelected,
                        onClick =
                                rememberClickWithSystemSound {
                                    onCategorySelected(category)
                                },
                        label = {
                            Text(
                                    text = categoryChipDisplayLabel(context, category),
                                    style = chipTypography.copy(shadow = null),
                                    color = LocalContentColor.current,
                                    fontWeight = chipWeight,
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors =
                                FilterChipDefaults.filterChipColors(
                                        containerColor = ChipBackground,
                                        selectedContainerColor =
                                                MaterialTheme.colorScheme.primary,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        iconColor = MaterialTheme.colorScheme.onSurface,
                                        selectedLeadingIconColor =
                                                MaterialTheme.colorScheme.onPrimary
                                ),
                        border =
                                FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent,
                                        enabled = true,
                                        selected = isSelected
                                )
                )
            }
        }
    }
}
