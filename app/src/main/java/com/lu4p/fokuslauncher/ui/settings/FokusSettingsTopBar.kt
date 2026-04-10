package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.lu4p.fokuslauncher.ui.components.LauncherIcon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.FokusIconButton
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FokusSettingsTopBar(
        title: @Composable () -> Unit,
        onNavigateBack: () -> Unit,
        containerColor: Color,
        modifier: Modifier = Modifier,
        actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
            modifier = modifier,
            title = title,
            navigationIcon = {
                FokusIconButton(onClick = onNavigateBack) {
                    LauncherIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                            iconSize = 24.dp,
                    )
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FokusSettingsTopBar(
        titleText: String,
        onNavigateBack: () -> Unit,
        containerColor: Color,
        modifier: Modifier = Modifier,
        actions: @Composable RowScope.() -> Unit = {},
) {
    FokusSettingsTopBar(
            title = {
                Text(
                        text = titleText,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onNavigateBack = onNavigateBack,
            containerColor = containerColor,
            modifier = modifier,
            actions = actions,
    )
}
