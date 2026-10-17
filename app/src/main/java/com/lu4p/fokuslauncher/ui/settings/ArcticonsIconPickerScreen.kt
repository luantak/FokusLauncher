package com.lu4p.fokuslauncher.ui.settings

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

@Composable
internal fun ArcticonsIconPickerScreen(
    storedIconKey: String,
    names: List<String>,
    loadIcon: suspend (String) -> Drawable?,
    onSelect: (String) -> Unit,
    onNavigateBack: () -> Unit,
    backgroundScrim: Color = Color.Black,
) {
    BackHandler(onBack = onNavigateBack)
    var query by remember { mutableStateOf("") }
    val filtered = remember(names, query) {
        names.filter { it.replace('_', ' ').containsNormalizedSearch(query.replace('_', ' ')) }
    }
    Column(Modifier.fillMaxSize().background(backgroundScrim).navigationBarsPadding()
        .testTag("arcticons_icon_picker")) {
        FokusSettingsTopBar(
            titleText = stringResource(R.string.edit_shortcuts_choose_icon),
            onNavigateBack = onNavigateBack,
            containerColor = Color.Transparent,
        )
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArcticonsPickerIcon(storedIconKey, loadIcon, 48.dp,
                stringResource(R.string.icon_picker_current_icon))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_icons)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filtered, key = { it }) { name ->
                ArcticonsPickerIcon(name, loadIcon, 32.dp, name.replace('_', ' '),
                    Modifier.padding(8.dp).clickableWithSystemSound { onSelect(name) })
            }
        }
    }
}

@Composable
private fun ArcticonsPickerIcon(
    name: String,
    loadIcon: suspend (String) -> Drawable?,
    size: androidx.compose.ui.unit.Dp,
    description: String,
    modifier: Modifier = Modifier,
) {
    val drawable by produceState<Drawable?>(null, name, loadIcon) { value = loadIcon(name) }
    LauncherIcon(
        drawable = drawable,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onBackground,
        forceTint = true,
        iconSize = size,
        modifier = modifier,
    )
}
