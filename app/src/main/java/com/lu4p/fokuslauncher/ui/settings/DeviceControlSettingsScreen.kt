package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.AccessibilityProminentDisclosureOverlay
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.utils.LockScreenHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accessibilityDisclosureAccepted by viewModel.accessibilityProminentDisclosureAccepted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAccessibilityProminentDisclosure by remember { mutableStateOf(false) }
    var accessibilityResumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    OnResumeEffect(lifecycleOwner) { accessibilityResumeTick++ }

    val lockAccessibilityOn =
            remember(accessibilityResumeTick) {
                LockScreenHelper.isLockAccessibilityServiceEnabled(context)
            }

    LaunchedEffect(lockAccessibilityOn, uiState.longLockReturnHome) {
        if (uiState.longLockReturnHome && !lockAccessibilityOn) {
            viewModel.setLongLockReturnHome(false)
        }
    }

    val deviceControlToggleRows =
            listOf(
                    DeviceControlToggleRow(
                            R.string.settings_double_tap_to_lock,
                            stringResource(R.string.settings_double_tap_to_lock_subtitle),
                            uiState.doubleTapEmptyLock,
                            viewModel::setDoubleTapEmptyLock,
                    ),
                    DeviceControlToggleRow(
                            R.string.settings_return_home_after_long_lock,
                            stringResource(R.string.settings_return_home_after_long_lock_subtitle),
                            uiState.longLockReturnHome,
                            viewModel::setLongLockReturnHome,
                    ),
            )

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
                    .testTag("device_control_settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FokusSettingsTopBar(
                    titleText = stringResource(R.string.settings_accessibility_page_title),
                    onNavigateBack = onNavigateBack,
                    containerColor = MaterialTheme.colorScheme.background,
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                item {
                    SettingsToggleRow(
                            label = stringResource(R.string.settings_accessibility_permission),
                            subtitle =
                                    stringResource(
                                            if (lockAccessibilityOn) {
                                                R.string.settings_accessibility_permission_enabled
                                            } else {
                                                R.string.settings_accessibility_permission_disabled
                                            }
                                    ),
                            checked = lockAccessibilityOn,
                            onCheckedChange = {
                                when {
                                    !lockAccessibilityOn && !accessibilityDisclosureAccepted ->
                                            showAccessibilityProminentDisclosure = true
                                    else ->
                                            LockScreenHelper.openAccessibilitySettings(context)
                                }
                            }
                    )
                }

                items(
                        deviceControlToggleRows,
                        key = { it.labelRes },
                ) { row ->
                    SettingsToggleRow(
                            label = stringResource(row.labelRes),
                            subtitle = row.subtitle,
                            checked = row.checked,
                            onCheckedChange = row.onCheckedChange,
                            enabled = lockAccessibilityOn,
                    )
                }

                if (lockAccessibilityOn && uiState.longLockReturnHome) {
                    item {
                        LongLockThresholdRow(
                                currentMinutes = uiState.longLockReturnHomeThresholdMinutes,
                                onMinutesSelected = viewModel::setLongLockReturnHomeThresholdMinutes
                        )
                    }
                }
            }
        }

        if (showAccessibilityProminentDisclosure) {
            AccessibilityProminentDisclosureOverlay(
                    onAccept = {
                        showAccessibilityProminentDisclosure = false
                        viewModel.acceptAccessibilityProminentDisclosureAndOpenSettings()
                    },
                    onNotNow = { showAccessibilityProminentDisclosure = false },
            )
        }
    }
}
