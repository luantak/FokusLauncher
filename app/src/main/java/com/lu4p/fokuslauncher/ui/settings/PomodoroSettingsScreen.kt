package com.lu4p.fokuslauncher.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.POMODORO_BREAK_MINUTE_OPTIONS
import com.lu4p.fokuslauncher.data.model.POMODORO_FOCUS_MINUTE_OPTIONS
import com.lu4p.fokuslauncher.pomodoro.PomodoroCompletionAlerter
import com.lu4p.fokuslauncher.pomodoro.pomodoroAlarmSoundDisplayName
import com.lu4p.fokuslauncher.pomodoro.pomodoroAlarmSoundPickerIntent
import com.lu4p.fokuslauncher.pomodoro.pomodoroAlarmSoundUriFromPickerResult
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDropdown
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pomodoroNotificationTick by remember { mutableIntStateOf(0) }
    var pendingPomodoroEnable by remember { mutableStateOf(false) }
    val pomodoroNotificationPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                pomodoroNotificationTick++
                if (granted && pendingPomodoroEnable) {
                    pendingPomodoroEnable = false
                    viewModel.setShowHomePomodoro(true)
                } else if (!granted) {
                    pendingPomodoroEnable = false
                }
            }
    val alarmSoundPickerLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
                viewModel.setPomodoroAlarmSoundUri(
                        pomodoroAlarmSoundUriFromPickerResult(result.data),
                )
            }
    OnResumeEffect(lifecycleOwner) { pomodoroNotificationTick++ }
    val pomodoroNotificationsEnabled =
            remember(pomodoroNotificationTick) {
                PomodoroCompletionAlerter.canPostNotifications(context)
            }
    LaunchedEffect(
            pomodoroNotificationTick,
            uiState.showHomePomodoro,
            pendingPomodoroEnable,
            pomodoroNotificationsEnabled,
    ) {
        if (pendingPomodoroEnable && pomodoroNotificationsEnabled) {
            pendingPomodoroEnable = false
            viewModel.setShowHomePomodoro(true)
        } else if (uiState.showHomePomodoro && !pomodoroNotificationsEnabled) {
            viewModel.setShowHomePomodoro(false)
        }
    }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("pomodoro_settings_screen"),
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_pomodoro_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_pomodoro),
                        subtitle =
                                when {
                                    !pomodoroNotificationsEnabled ->
                                            stringResource(
                                                    R.string
                                                            .settings_show_home_pomodoro_subtitle_grant_notifications
                                            )
                                    uiState.showHomeMedia ->
                                            stringResource(
                                                    R.string
                                                            .settings_show_home_media_or_pomodoro_exclusive
                                            )
                                    else ->
                                            stringResource(
                                                    R.string.settings_show_home_pomodoro_subtitle,
                                                    stringResource(R.string.pomodoro_mode_focus),
                                            )
                                },
                        checked = uiState.showHomePomodoro,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (pomodoroNotificationsEnabled) {
                                    viewModel.setShowHomePomodoro(true)
                                } else {
                                    pendingPomodoroEnable = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        pomodoroNotificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    } else {
                                        context.startActivity(
                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                        .apply {
                                                            putExtra(
                                                                    Settings.EXTRA_APP_PACKAGE,
                                                                    context.packageName,
                                                            )
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        },
                                        )
                                    }
                                }
                            } else {
                                pendingPomodoroEnable = false
                                viewModel.setShowHomePomodoro(false)
                            }
                        },
                )
            }
            item {
                val fokusLabel = stringResource(R.string.pomodoro_mode_focus)
                PomodoroDurationDropdown(
                        title =
                                stringResource(R.string.settings_pomodoro_focus_duration, fokusLabel),
                        subtitle =
                                stringResource(
                                        R.string.settings_pomodoro_focus_duration_subtitle,
                                        fokusLabel,
                                ),
                        options = POMODORO_FOCUS_MINUTE_OPTIONS,
                        currentMinutes = uiState.pomodoroFocusMinutes,
                        onMinutesSelected = viewModel::setPomodoroFocusMinutes,
                        enabled = uiState.showHomePomodoro,
                )
            }
            item {
                PomodoroDurationDropdown(
                        title = stringResource(R.string.settings_pomodoro_break_duration),
                        subtitle = stringResource(R.string.settings_pomodoro_break_duration_subtitle),
                        options = POMODORO_BREAK_MINUTE_OPTIONS,
                        currentMinutes = uiState.pomodoroBreakMinutes,
                        onMinutesSelected = viewModel::setPomodoroBreakMinutes,
                        enabled = uiState.showHomePomodoro,
                )
            }
            item {
                val soundName =
                        remember(uiState.pomodoroAlarmSoundUri, context) {
                            pomodoroAlarmSoundDisplayName(context, uiState.pomodoroAlarmSoundUri)
                        }
                SettingsRow(
                        label = stringResource(R.string.settings_pomodoro_alarm_sound),
                        subtitle = soundName,
                        clickableEnabled = uiState.showHomePomodoro,
                        onClick =
                                if (uiState.showHomePomodoro) {
                                    {
                                        alarmSoundPickerLauncher.launch(
                                                pomodoroAlarmSoundPickerIntent(
                                                        uiState.pomodoroAlarmSoundUri,
                                                ),
                                        )
                                    }
                                } else {
                                    null
                                },
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PomodoroDurationDropdown(
        title: String,
        subtitle: String,
        options: List<Int>,
        currentMinutes: Int,
        onMinutesSelected: (Int) -> Unit,
        enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val onExpandedChange =
            rememberBooleanChangeWithSystemSound { newExpanded ->
                if (enabled) expanded = newExpanded
            }
    val selectedLabel =
            pluralStringResource(
                    R.plurals.settings_pomodoro_duration_minutes,
                    currentMinutes,
                    currentMinutes,
            )
    SettingsDropdown(
            title = title,
            subtitle = subtitle,
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            selectedDisplayText = selectedLabel,
            fieldEnabled = enabled,
            menuExpanded = expanded && enabled,
            itemContent = { minutes ->
                Text(
                        text =
                                pluralStringResource(
                                        R.plurals.settings_pomodoro_duration_minutes,
                                        minutes,
                                        minutes,
                                ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = onMinutesSelected,
    )
}
