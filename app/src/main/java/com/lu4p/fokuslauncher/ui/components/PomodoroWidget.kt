package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.ui.home.HomeWidgetAlignment
import com.lu4p.fokuslauncher.ui.home.WidgetControlIconBaseSizeDp
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound

/**
 * Home Pomodoro timer in the media-widget slot.
 *
 * Matches [MediaWidget]: typography-first, left-aligned, flat transport icons — no card, ring,
 * or pill chrome.
 */
@Composable
fun PomodoroWidget(
        remainingText: String,
        isRunning: Boolean,
        awaitingDismiss: Boolean,
        mode: PomodoroMode,
        alignment: HomeWidgetAlignment = HomeWidgetAlignment.START,
        modifier: Modifier = Modifier,
        outlined: Boolean = false,
        onPlayPause: () -> Unit = {},
        onDecrease: () -> Unit = {},
        onIncrease: () -> Unit = {},
        onSelectMode: (PomodoroMode) -> Unit = {},
) {
    val color = MaterialTheme.colorScheme.onBackground
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val modeStyle = MaterialTheme.typography.bodyMedium
    val iconSize = WidgetControlIconBaseSizeDp.dp
    val muted = color.copy(alpha = 0.38f)
    val horizontalAlignment =
            when (alignment) {
                HomeWidgetAlignment.START -> Alignment.Start
                HomeWidgetAlignment.CENTER -> Alignment.CenterHorizontally
                HomeWidgetAlignment.END -> Alignment.End
            }

    Column(
            horizontalAlignment = horizontalAlignment,
            modifier = modifier.testTag("pomodoro_widget"),
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, horizontalAlignment),
                modifier = Modifier.fillMaxWidth(),
        ) {
            PomodoroModeLabel(
                    label = stringResource(R.string.pomodoro_mode_focus),
                    selected = mode == PomodoroMode.FOCUS,
                    style = modeStyle,
                    color = color,
                    outlined = outlined,
                    enabled = !awaitingDismiss,
                    onClick = { onSelectMode(PomodoroMode.FOCUS) },
                    modifier = Modifier.testTag("pomodoro_mode_focus"),
            )
            PomodoroModeLabel(
                    label = stringResource(R.string.pomodoro_mode_break),
                    selected = mode == PomodoroMode.BREAK,
                    style = modeStyle,
                    color = color,
                    outlined = outlined,
                    enabled = !awaitingDismiss,
                    onClick = { onSelectMode(PomodoroMode.BREAK) },
                    modifier = Modifier.testTag("pomodoro_mode_break"),
            )
            PomodoroText(
                    text = remainingText,
                    style = titleStyle,
                    color = color,
                    outlined = outlined,
                    modifier = Modifier.testTag("pomodoro_remaining"),
            )
        }
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp, horizontalAlignment),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            LauncherIcon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.pomodoro_decrease),
                    iconSize = iconSize,
                    tint = if (awaitingDismiss) muted else color,
                    outlined = outlined,
                    modifier =
                            Modifier.testTag("pomodoro_decrease")
                                    .then(
                                            if (awaitingDismiss) {
                                                Modifier
                                            } else {
                                                Modifier.clickableNoRippleWithSystemSound(
                                                        onClick = onDecrease
                                                )
                                            },
                                    ),
            )
            LauncherIcon(
                    imageVector =
                            when {
                                awaitingDismiss -> Icons.Filled.Close
                                isRunning -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                    contentDescription =
                            stringResource(
                                    when {
                                        awaitingDismiss -> R.string.pomodoro_dismiss_alarm
                                        isRunning -> R.string.pomodoro_pause
                                        else -> R.string.pomodoro_play
                                    }
                            ),
                    iconSize = iconSize,
                    tint = color,
                    outlined = outlined,
                    modifier =
                            Modifier.testTag("pomodoro_play_pause")
                                    .clickableNoRippleWithSystemSound(onClick = onPlayPause),
            )
            LauncherIcon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.pomodoro_increase),
                    iconSize = iconSize,
                    tint = if (awaitingDismiss) muted else color,
                    outlined = outlined,
                    modifier =
                            Modifier.testTag("pomodoro_increase")
                                    .then(
                                            if (awaitingDismiss) {
                                                Modifier
                                            } else {
                                                Modifier.clickableNoRippleWithSystemSound(
                                                        onClick = onIncrease
                                                )
                                            },
                                    ),
            )
        }
    }
}

@Composable
private fun PomodoroModeLabel(
        label: String,
        selected: Boolean,
        style: TextStyle,
        color: Color,
        outlined: Boolean,
        enabled: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val labelColor = if (selected) color else color.copy(alpha = 0.38f)
    PomodoroText(
            text = label,
            style = style,
            color = labelColor,
            outlined = outlined,
            modifier =
                    modifier.then(
                            if (enabled) {
                                Modifier.clickableNoRippleWithSystemSound(onClick = onClick)
                            } else {
                                Modifier
                            },
                    ),
    )
}

@Composable
private fun PomodoroText(
        text: String,
        style: TextStyle,
        color: Color,
        outlined: Boolean,
        modifier: Modifier = Modifier,
) {
    if (outlined) {
        OutlinedText(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
        )
    } else {
        Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
        )
    }
}
