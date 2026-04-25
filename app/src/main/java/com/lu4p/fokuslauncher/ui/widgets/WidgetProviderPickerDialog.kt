package com.lu4p.fokuslauncher.ui.widgets

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.widget.WidgetProviderInfo
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetProviderPickerDialog(
        providers: List<WidgetProviderInfo>,
        onSelect: (WidgetProviderInfo) -> Unit,
        onDismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val filtered =
            remember(filter, providers) {
                if (filter.isBlank()) providers
                else {
                    providers.filter {
                        it.label.containsNormalizedSearch(filter) ||
                                it.appLabel.containsNormalizedSearch(filter)
                    }
                }
            }

    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text = stringResource(R.string.widget_picker_title),
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                            value = filter,
                            onValueChange = { filter = it },
                            label = { Text(stringResource(R.string.search)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 156.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 520.dp),
                    ) {
                        items(filtered, key = { it.stableKey }) { provider ->
                            WidgetProviderTile(
                                    provider = provider,
                                    onClick = { onSelect(provider) },
                                    modifier = Modifier.padding(6.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun WidgetProviderTile(
        provider: WidgetProviderInfo,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                    modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                            .clickableWithSystemSound(onClick = onClick)
                            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Box(
                contentAlignment = Alignment.Center,
                modifier =
                        Modifier.fillMaxWidth()
                                .height(104.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
        ) {
            if (provider.preview != null) {
                AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setImageDrawable(provider.preview)
                            }
                        },
                        update = { it.setImageDrawable(provider.preview) },
                        modifier = Modifier.fillMaxWidth().height(96.dp).padding(8.dp),
                )
            } else if (provider.appIcon != null) {
                AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setImageDrawable(provider.appIcon)
                            }
                        },
                        update = { it.setImageDrawable(provider.appIcon) },
                        modifier = Modifier.size(48.dp),
                )
            } else {
                Text(
                        text = provider.appLabel.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
                text = provider.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
        )
        Text(
                text = provider.appLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
                text = stringResource(R.string.widget_initial_height, provider.defaultHeightDp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier =
                        Modifier.clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
