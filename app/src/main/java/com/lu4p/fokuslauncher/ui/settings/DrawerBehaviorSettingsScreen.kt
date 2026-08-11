package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerBehaviorSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("drawer_behavior_settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_drawer_behavior_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_search_auto_launch),
                        subtitle =
                                stringResource(R.string.settings_drawer_search_auto_launch_subtitle),
                        checked = uiState.drawerSearchAutoLaunch,
                        onCheckedChange = viewModel::setDrawerSearchAutoLaunch,
                )
            }
            if (!uiState.drawerSidebarCategories) {
                item {
                    SettingsToggleRow(
                            label =
                                    stringResource(
                                            R.string.settings_drawer_scroll_to_top_auto_keyboard
                                    ),
                            subtitle =
                                    stringResource(
                                            R.string
                                                    .settings_drawer_scroll_to_top_auto_keyboard_subtitle
                                    ),
                            checked = uiState.drawerScrollToTopAutoKeyboard,
                            onCheckedChange = viewModel::setDrawerScrollToTopAutoKeyboard,
                    )
                }
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_sidebar_categories),
                        subtitle =
                                stringResource(
                                        R.string.settings_drawer_sidebar_categories_subtitle
                                ),
                        checked = uiState.drawerSidebarCategories,
                        onCheckedChange = viewModel::setDrawerSidebarCategories,
                )
            }
            if (uiState.drawerSidebarCategories) {
                item {
                    DrawerCategoryRailSideRow(
                            railOnLeft = uiState.drawerCategorySidebarOnLeft,
                            onRailOnLeftChanged = viewModel::setDrawerCategorySidebarOnLeft,
                    )
                }
            }
            item {
                DrawerAppSortRow(
                        currentMode = uiState.drawerAppSortMode,
                        showCustomSortOption = uiState.drawerSidebarCategories,
                        onModeChanged = viewModel::setDrawerAppSortMode,
                )
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
