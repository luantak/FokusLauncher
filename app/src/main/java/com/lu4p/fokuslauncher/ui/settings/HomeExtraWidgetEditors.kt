package com.lu4p.fokuslauncher.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.COUNTDOWN_TITLE_MAX_LENGTH
import com.lu4p.fokuslauncher.data.model.WORLD_CLOCK_LABEL_MAX_LENGTH
import com.lu4p.fokuslauncher.data.model.defaultLabelForTimeZoneId
import com.lu4p.fokuslauncher.data.model.displayNameForTimeZoneId
import com.lu4p.fokuslauncher.data.model.filterTimeZonePickerEntries
import com.lu4p.fokuslauncher.data.model.formatUtcOffsetLabel
import com.lu4p.fokuslauncher.data.model.listSystemTimeZonePickerEntries
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.home.epochDayToYearMonthDay
import com.lu4p.fokuslauncher.ui.home.epochMillisToLocalParts
import com.lu4p.fokuslauncher.ui.home.localDateTimeToEpochMillis
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun WorldClockCityEditDialog(
        title: String,
        initialLabel: String,
        initialTimeZoneId: String,
        onDismiss: () -> Unit,
        onSave: (label: String, timeZoneId: String) -> Unit,
) {
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    var selectedZone by remember(initialTimeZoneId) { mutableStateOf(initialTimeZoneId) }
    var showZonePicker by remember { mutableStateOf(false) }
    val offset = remember(selectedZone) { formatUtcOffsetLabel(selectedZone) }
    val zoneName = remember(selectedZone) { displayNameForTimeZoneId(selectedZone) }

    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column {
                    OutlinedTextField(
                            value = label,
                            onValueChange = { label = it.take(WORLD_CLOCK_LABEL_MAX_LENGTH) },
                            label = { Text(stringResource(R.string.settings_world_clock_city_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsRow(
                            label = stringResource(R.string.settings_world_clock_city_zone),
                            subtitle = "$zoneName · $offset",
                            horizontalPadding = 0.dp,
                            onClick = { showZonePicker = true },
                    )
                }
            },
            confirmButton = {
                FokusTextButton(
                        onClick = { onSave(label, selectedZone) },
                        enabled = label.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
    )

    if (showZonePicker) {
        TimeZonePickerDialog(
                onDismiss = { showZonePicker = false },
                onSelect = { zoneId ->
                    selectedZone = zoneId
                    if (label.isBlank()) {
                        label =
                                defaultLabelForTimeZoneId(zoneId)
                                        .take(WORLD_CLOCK_LABEL_MAX_LENGTH)
                    }
                    showZonePicker = false
                },
        )
    }
}

@Composable
fun TimeZonePickerDialog(
        onDismiss: () -> Unit,
        onSelect: (timeZoneId: String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val allZones = remember { listSystemTimeZonePickerEntries() }
    val filtered =
            remember(query, allZones) { filterTimeZonePickerEntries(allZones, query) }

    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_world_clock_city_zone)) },
            text = {
                Column {
                    OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = {
                                Text(stringResource(R.string.settings_world_clock_search_zones))
                            },
                            singleLine = true,
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .testTag("world_clock_zone_search"),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        Text(
                                text = stringResource(R.string.settings_world_clock_no_zones),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                        )
                    } else {
                        LazyColumn(
                                modifier =
                                        Modifier.height(320.dp)
                                                .testTag("world_clock_zone_list"),
                        ) {
                            items(filtered, key = { it.timeZoneId }) { entry ->
                                SettingsRow(
                                        label = entry.displayName,
                                        subtitle = "${entry.utcOffsetLabel} · ${entry.timeZoneId}",
                                        horizontalPadding = 0.dp,
                                        onClick = { onSelect(entry.timeZoneId) },
                                )
                            }
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownEditDialog(
        initialTitle: String,
        initialEpochMillis: Long,
        onDismiss: () -> Unit,
        onSave: (title: String, targetEpochMillis: Long) -> Unit,
) {
    val context = LocalContext.current
    val initialParts = remember(initialEpochMillis) { epochMillisToLocalParts(initialEpochMillis) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var epochDay by remember(initialParts.epochDay) { mutableLongStateOf(initialParts.epochDay) }
    var hourOfDay by remember(initialParts.hourOfDay) { mutableIntStateOf(initialParts.hourOfDay) }
    var minute by remember(initialParts.minute) { mutableIntStateOf(initialParts.minute) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val targetMillis =
            remember(epochDay, hourOfDay, minute) {
                val (year, month, day) = epochDayToYearMonthDay(epochDay)
                localDateTimeToEpochMillis(year, month, day, hourOfDay, minute)
            }
    val timeOnlyLabel =
            remember(hourOfDay, minute) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                DateFormat.getTimeFormat(context).format(cal.time)
            }
    val dateOnlyLabel =
            remember(epochDay) {
                val (year, month, day) = epochDayToYearMonthDay(epochDay)
                val millis = localDateTimeToEpochMillis(year, month, day, 0, 0)
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
                        .format(java.util.Date(millis))
            }

    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_countdown_edit)) },
            text = {
                Column {
                    OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(COUNTDOWN_TITLE_MAX_LENGTH) },
                            label = { Text(stringResource(R.string.settings_countdown_title)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsRow(
                            label = stringResource(R.string.settings_countdown_date),
                            subtitle = dateOnlyLabel,
                            horizontalPadding = 0.dp,
                            onClick = { showDatePicker = true },
                    )
                    SettingsRow(
                            label = stringResource(R.string.settings_countdown_time),
                            subtitle = timeOnlyLabel,
                            horizontalPadding = 0.dp,
                            onClick = { showTimePicker = true },
                    )
                }
            },
            confirmButton = {
                FokusTextButton(
                        onClick = { onSave(title, targetMillis) },
                        enabled = title.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
    )

    if (showDatePicker) {
        val pickerState =
                rememberDatePickerState(
                        initialSelectedDateMillis = TimeUnit.DAYS.toMillis(epochDay),
                )
        DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    FokusTextButton(
                            onClick = {
                                val millis = pickerState.selectedDateMillis
                                if (millis != null) {
                                    epochDay = TimeUnit.MILLISECONDS.toDays(millis)
                                }
                                showDatePicker = false
                            },
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                },
                dismissButton = {
                    FokusTextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                colors =
                        androidx.compose.material3.DatePickerDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val is24Hour = DateFormat.is24HourFormat(context)
        val timePickerState =
                rememberTimePickerState(
                        initialHour = hourOfDay,
                        initialMinute = minute,
                        is24Hour = is24Hour,
                )
        AlertDialog(
                onDismissRequest = { showTimePicker = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(stringResource(R.string.settings_countdown_time)) },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    FokusTextButton(
                            onClick = {
                                hourOfDay = timePickerState.hour
                                minute = timePickerState.minute
                                showTimePicker = false
                            },
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                },
                dismissButton = {
                    FokusTextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
        )
    }
}
