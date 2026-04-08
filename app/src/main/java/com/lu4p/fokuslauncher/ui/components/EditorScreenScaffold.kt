package com.lu4p.fokuslauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreenScaffold(
        titleText: String,
        searchPlaceholderResId: Int,
        backgroundScrim: Color,
        listReadyToScroll: Boolean,
        onNavigateBack: () -> Unit,
        onDone: () -> Unit,
        content: @Composable ColumnScope.(searchQuery: String, listState: LazyListState) -> Unit,
) {
    EditorScreenScaffold(
            title = {
                Text(
                        text = titleText,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            searchPlaceholderResId = searchPlaceholderResId,
            backgroundScrim = backgroundScrim,
            listReadyToScroll = listReadyToScroll,
            onNavigateBack = onNavigateBack,
            onDone = onDone,
            content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreenScaffold(
        title: @Composable () -> Unit,
        searchPlaceholderResId: Int,
        backgroundScrim: Color,
        listReadyToScroll: Boolean,
        onNavigateBack: () -> Unit,
        onDone: () -> Unit,
        content: @Composable ColumnScope.(searchQuery: String, listState: LazyListState) -> Unit,
) {
    EditorScreenScaffoldLayout(
            topBar = {
                FokusSettingsTopBar(
                        title = title,
                        onNavigateBack = onNavigateBack,
                        containerColor = MaterialTheme.colorScheme.surface,
                        actions = {
                            FokusIconButton(onClick = onDone) {
                                Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription =
                                                stringResource(R.string.action_done),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        },
                )
            },
            searchPlaceholderResId = searchPlaceholderResId,
            backgroundScrim = backgroundScrim,
            listReadyToScroll = listReadyToScroll,
            onNavigateBack = onNavigateBack,
            content = content,
    )
}

@Composable
private fun EditorScreenScaffoldLayout(
        topBar: @Composable () -> Unit,
        searchPlaceholderResId: Int,
        backgroundScrim: Color,
        listReadyToScroll: Boolean,
        onNavigateBack: () -> Unit,
        content: @Composable ColumnScope.(String, LazyListState) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var didSnapListTop by remember { mutableStateOf(false) }
    LaunchedEffect(listReadyToScroll) {
        if (didSnapListTop || !listReadyToScroll) return@LaunchedEffect
        listState.scrollToItem(0, 0)
        didSnapListTop = true
    }

    BackHandler(onBack = onNavigateBack)

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
    ) {
        topBar()

        OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(searchPlaceholderResId)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )

        content(searchQuery, listState)
    }
}
