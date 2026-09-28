package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.TwoFingerDirection
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop

@Composable
fun GesturesSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val picker = remember { mutableStateOf<String?>(null) }
    val directions = TwoFingerDirection.entries.associateWith { direction ->
        val target by viewModel.twoFingerTargets.getValue(direction).collectAsStateWithLifecycle()
        target
    }

    Column(Modifier.fillMaxSize().background(backgroundScrim).navigationBarsPadding()) {
        FokusSettingsTopBar(
            titleText = stringResource(R.string.settings_gestures),
            onNavigateBack = onNavigateBack,
            containerColor = MaterialTheme.colorScheme.background,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item { SectionHeader(stringResource(R.string.settings_gestures_subtitle)) }
            item {
                GestureTargetRow(R.string.settings_swipe_left, state.swipeLeftTarget,
                    state, { picker.value = "swipeLeft" }, { viewModel.setSwipeLeftTarget(null) })
            }
            item {
                GestureTargetRow(R.string.settings_swipe_right, state.swipeRightTarget,
                    state, { picker.value = "swipeRight" }, { viewModel.setSwipeRightTarget(null) })
            }
            item {
                ShortcutTargetRow(
                    label = stringResource(R.string.settings_double_tap),
                    currentTarget = formatWidgetTapTarget(
                        context, resources, state.doubleTapEmptyTarget, state.allApps,
                        state.allShortcutActions,
                        emptyLabel = { _, res -> res.getString(R.string.shortcut_target_not_set) },
                    ),
                    onPickApp = { picker.value = "doubleTap" },
                    onClear = { viewModel.setDoubleTapEmptyTarget(null) },
                    enabled = !state.doubleTapEmptyLock,
                )
            }
            item { SectionHeader(stringResource(R.string.settings_two_finger_gestures)) }
            TwoFingerDirection.entries.forEach { direction ->
                item(key = direction.name) {
                    val label = when (direction) {
                        TwoFingerDirection.UP -> R.string.settings_two_finger_up
                        TwoFingerDirection.DOWN -> R.string.settings_two_finger_down
                        TwoFingerDirection.LEFT -> R.string.settings_two_finger_left
                        TwoFingerDirection.RIGHT -> R.string.settings_two_finger_right
                    }
                    GestureTargetRow(label, directions[direction], state,
                        { picker.value = direction.name },
                        { viewModel.setTwoFingerTarget(direction, null) })
                }
            }
        }
    }

    picker.value?.let { key ->
        val doubleTap = key == "doubleTap"
        ShortcutActionPickerDialog(
            allActions = if (doubleTap) state.allShortcutActions.filter {
                it.actionLabel == AppShortcutAction.OPEN_APP_LABEL
            } else state.allShortcutActions,
            allApps = state.allApps,
            title = stringResource(if (doubleTap) R.string.settings_double_tap_open_app
                else R.string.edit_shortcuts_section_all_actions),
            onSelect = { action ->
                when (key) {
                    "swipeLeft" -> viewModel.setSwipeLeftTarget(action.target)
                    "swipeRight" -> viewModel.setSwipeRightTarget(action.target)
                    "doubleTap" -> viewModel.setDoubleTapEmptyTarget(action)
                    else -> viewModel.setTwoFingerTarget(TwoFingerDirection.valueOf(key), action.target)
                }
                picker.value = null
            },
            onDismiss = { picker.value = null },
            includeWidgetPageTarget = !doubleTap,
            profileDisplayNameOverrides = state.profileDisplayNameOverrides,
        )
    }
}

@Composable
private fun GestureTargetRow(
    label: Int,
    target: ShortcutTarget?,
    state: SettingsUiState,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    ShortcutTargetRow(
        label = stringResource(label),
        currentTarget = formatShortcutTarget(LocalContext.current, LocalResources.current,
            target, state.allApps),
        onPickApp = onPick,
        onClear = onClear,
    )
}
