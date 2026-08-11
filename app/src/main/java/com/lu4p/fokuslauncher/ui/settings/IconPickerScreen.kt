package com.lu4p.fokuslauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.IconPickerGridVariant
import com.lu4p.fokuslauncher.ui.components.IconPickerSectionsLazyGrid
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.resolvedCategoryDrawerIconName
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

/**
 * Full-page Material icon picker (home shortcuts, category rail icons). Replaces the old dialog so
 * the curated catalog is easier to browse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerScreen(
        storedIconKey: String,
        titleText: String,
        onSelect: (String) -> Unit,
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    BackHandler(onBack = onNavigateBack)
    var iconSearchQuery by remember { mutableStateOf("") }
    val filteredIconNames =
            remember(iconSearchQuery) {
                val all = MinimalIcons.names
                if (iconSearchQuery.isBlank()) all
                else
                        all.filter { name ->
                            name.containsNormalizedSearch(iconSearchQuery) ||
                                    MinimalIcons.materialOutlinedSearchHaystack(name)
                                            .containsNormalizedSearch(iconSearchQuery)
                        }
            }
    val iconPickerSections =
            remember(iconSearchQuery, filteredIconNames) {
                if (iconSearchQuery.isBlank()) MinimalIcons.iconPickerSections
                else MinimalIcons.iconPickerSearchSections(filteredIconNames)
            }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
    ) {
        FokusSettingsTopBar(
                titleText = titleText,
                onNavigateBack = onNavigateBack,
                containerColor = Color.Transparent,
        )
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LauncherIcon(
                    imageVector = MinimalIcons.iconFor(storedIconKey),
                    contentDescription = stringResource(R.string.icon_picker_current_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    iconSize = 48.dp,
            )
            Text(
                    text = stringResource(R.string.icon_picker_current_icon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                    value = iconSearchQuery,
                    onValueChange = { iconSearchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_icons)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        IconPickerSectionsLazyGrid(
                sections = iconPickerSections,
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier =
                        Modifier.fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                variant = IconPickerGridVariant.Page,
                isSelected = { MinimalIcons.iconKeyMatchesStoredIcon(it, storedIconKey) },
                onSelect = onSelect,
        )
    }
}

@Composable
fun CategoryIconPickerScreen(
        category: String,
        iconOverrides: Map<String, String>,
        onSelect: (String) -> Unit,
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val context = LocalContext.current
    val resolved = resolvedCategoryDrawerIconName(context, category, iconOverrides)
    IconPickerScreen(
            storedIconKey = resolved,
            titleText = stringResource(R.string.category_icon_picker_title),
            onSelect = onSelect,
            onNavigateBack = onNavigateBack,
            backgroundScrim = backgroundScrim,
    )
}
