package com.lu4p.fokuslauncher.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.HomeExtraWidgetAddType
import com.lu4p.fokuslauncher.data.model.HomeExtraWidgetEntry
import com.lu4p.fokuslauncher.data.model.displayNameForTimeZoneId
import com.lu4p.fokuslauncher.data.model.formatUtcOffsetLabel
import com.lu4p.fokuslauncher.media.MediaNotificationHelper
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.home.formatCountdownDateTimeLabel
import com.lu4p.fokuslauncher.ui.settings.components.EditorDragHandleReorderIcon
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.OnResumeEffect
import com.lu4p.fokuslauncher.ui.util.clickableWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberLocallyReorderedList
import com.lu4p.fokuslauncher.ui.util.rememberVerticalSlotReorderState
import com.lu4p.fokuslauncher.usage.UsageStatsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWidgetsSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val showAppPickerFor = remember { mutableStateOf<String?>(null) }

    val (hasCoarseLocationPermission, requestCoarseLocation) =
            rememberCoarseLocationPermission(context, activity)

    var mediaNotificationAccessTick by remember { mutableIntStateOf(0) }
    var pendingMediaEnable by remember { mutableStateOf(false) }
    var usageAccessTick by remember { mutableIntStateOf(0) }
    var pendingScreenTimeEnable by remember { mutableStateOf(false) }
    var showAddOtherWidget by remember { mutableStateOf(false) }
    var editingCityId by remember { mutableStateOf<String?>(null) }
    var pendingEditNewestCity by remember { mutableStateOf(false) }
    var editingCountdownId by remember { mutableStateOf<String?>(null) }
    var pendingEditNewestCountdown by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val extraWidgetsSource = uiState.homeExtraWidgets
    val citiesById = remember(uiState.worldClockCities) { uiState.worldClockCities.associateBy { it.id } }
    val eventsById = remember(uiState.countdownEvents) { uiState.countdownEvents.associateBy { it.id } }
    val localExtras = rememberLocallyReorderedList(extraWidgetsSource)
    val extraWidgets = localExtras.items
    val reorderState = rememberVerticalSlotReorderState()
    val onExtraCommit by rememberUpdatedState(viewModel::setHomeExtraWidgets)
    OnResumeEffect(lifecycleOwner) {
        mediaNotificationAccessTick++
        usageAccessTick++
    }
    val mediaNotificationAccessEnabled =
            remember(mediaNotificationAccessTick) {
                MediaNotificationHelper.isListenerEnabled(context)
            }
    val usageAccessEnabled =
            remember(usageAccessTick) { UsageStatsHelper.hasUsageAccess(context) }
    LaunchedEffect(mediaNotificationAccessTick, uiState.showHomeMedia, pendingMediaEnable) {
        if (pendingMediaEnable && mediaNotificationAccessEnabled) {
            pendingMediaEnable = false
            viewModel.setShowHomeMedia(true)
        } else if (uiState.showHomeMedia && !mediaNotificationAccessEnabled) {
            viewModel.setShowHomeMedia(false)
        }
    }
    LaunchedEffect(usageAccessTick, uiState.showHomeScreenTime, pendingScreenTimeEnable) {
        if (pendingScreenTimeEnable && usageAccessEnabled) {
            pendingScreenTimeEnable = false
            viewModel.setShowHomeScreenTime(true)
        } else if (uiState.showHomeScreenTime && !usageAccessEnabled) {
            viewModel.setShowHomeScreenTime(false)
        }
    }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("home_widgets_settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_home_widgets),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                    listOf(
                            Triple(R.string.settings_show_home_clock, uiState.showHomeClock, viewModel::setShowHomeClock),
                            Triple(R.string.settings_show_home_date, uiState.showHomeDate, viewModel::setShowHomeDate),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                HomeDateFormatDropdown(
                        currentStyle = uiState.homeDateFormatStyle,
                        enabled = uiState.showHomeDate,
                        onStyleSelected = viewModel::setHomeDateFormatStyle,
                )
            }
            items(
                    listOf(
                            Triple(R.string.settings_show_home_weather, uiState.showHomeWeather, viewModel::setShowHomeWeather),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_air_quality),
                        subtitle =
                                stringResource(R.string.settings_show_home_air_quality_subtitle),
                        checked = uiState.showHomeAirQuality,
                        enabled = uiState.showHomeWeather,
                        onCheckedChange = viewModel::setShowHomeAirQuality,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_world_clock_weather),
                        subtitle =
                                stringResource(R.string.settings_show_world_clock_weather_subtitle),
                        checked = uiState.showWorldClockWeather,
                        onCheckedChange = viewModel::setShowWorldClockWeather,
                )
            }
            item {
                TemperatureUnitDropdown(
                        currentUnit = uiState.temperatureUnit,
                        enabled = uiState.showHomeWeather || uiState.showWorldClockWeather,
                        onUnitSelected = viewModel::setTemperatureUnit,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_screen_time),
                        subtitle =
                                if (usageAccessEnabled) {
                                    stringResource(R.string.settings_show_home_screen_time_subtitle)
                                } else {
                                    stringResource(
                                            R.string.settings_show_home_screen_time_subtitle_grant_access
                                    )
                                },
                        checked = uiState.showHomeScreenTime,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (usageAccessEnabled) {
                                    viewModel.setShowHomeScreenTime(true)
                                } else {
                                    pendingScreenTimeEnable = true
                                    UsageStatsHelper.openUsageAccessSettings(context)
                                }
                            } else {
                                pendingScreenTimeEnable = false
                                viewModel.setShowHomeScreenTime(false)
                            }
                        },
                )
            }
            items(
                    listOf(
                            Triple(R.string.settings_show_home_battery, uiState.showHomeBattery, viewModel::setShowHomeBattery),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_media),
                        subtitle =
                                if (mediaNotificationAccessEnabled) {
                                    stringResource(R.string.settings_show_home_media_subtitle)
                                } else {
                                    stringResource(R.string.settings_show_home_media_subtitle_grant_access)
                                },
                        checked = uiState.showHomeMedia,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (mediaNotificationAccessEnabled) {
                                    viewModel.setShowHomeMedia(true)
                                } else {
                                    pendingMediaEnable = true
                                    MediaNotificationHelper.openListenerSettings(context)
                                }
                            } else {
                                pendingMediaEnable = false
                                viewModel.setShowHomeMedia(false)
                            }
                        },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_home_extra_widgets),
                        subtitle =
                                if (extraWidgets.isEmpty()) {
                                    stringResource(R.string.settings_home_extra_widgets_empty)
                                } else {
                                    null
                                },
                )
            }
            items(
                    count = extraWidgets.size,
                    key = { extraWidgets[it].stableKey },
            ) { index ->
                val entry = extraWidgets[index]
                val title: String
                val subtitle: String
                when (entry) {
                    is HomeExtraWidgetEntry.WorldClock -> {
                        val city = citiesById[entry.cityId]
                        title = city?.label ?: stringResource(R.string.settings_world_clock_cities)
                        subtitle =
                                if (city == null) {
                                    stringResource(R.string.settings_world_clock_cities_empty)
                                } else {
                                    val zone = displayNameForTimeZoneId(city.timeZoneId)
                                    val offset = formatUtcOffsetLabel(city.timeZoneId)
                                    "$zone · $offset"
                                }
                    }
                    is HomeExtraWidgetEntry.Countdown -> {
                        val event = eventsById[entry.eventId]
                        title = event?.title ?: stringResource(R.string.settings_countdown_event)
                        subtitle =
                                if (event == null) {
                                    stringResource(R.string.settings_countdown_event_empty)
                                } else {
                                    formatCountdownDateTimeLabel(context, event)
                                }
                    }
                }
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .graphicsLayer {
                                            translationY = reorderState.translationYForIndex(index)
                                        }
                                        .clickableWithSystemSound {
                                            when (entry) {
                                                is HomeExtraWidgetEntry.WorldClock ->
                                                        editingCityId = entry.cityId
                                                is HomeExtraWidgetEntry.Countdown ->
                                                        editingCountdownId = entry.eventId
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    EditorDragHandleReorderIcon(
                            reorderState = reorderState,
                            index = index,
                            lastIndex = extraWidgets.lastIndex,
                            onReorder = localExtras::reorder,
                            onReset = {
                                reorderState.reset {
                                    localExtras.onDragEnd(onExtraCommit)
                                }
                            },
                            entry.stableKey,
                            extraWidgets.size,
                            onDragGestureStart = localExtras::onDragStart,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    FokusTextButton(onClick = { viewModel.removeHomeExtraWidget(entry) }) {
                        Text(stringResource(R.string.settings_remove_other_widget))
                    }
                }
            }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_add_other_widget),
                        onClick = { showAddOtherWidget = true },
                )
            }
            item { SettingsDivider() }
            item {
                WeatherAppSettingRow(
                        hasCoarseLocationPermission = hasCoarseLocationPermission,
                        onRequestLocationPermission = requestCoarseLocation,
                        context = context,
                        resources = resources,
                        preferredWeatherTap = uiState.preferredWeatherTap,
                        allApps = uiState.allApps,
                        allShortcutActions = uiState.allShortcutActions,
                        onPickApp = { showAppPickerFor.value = "weather" },
                        onClear = { viewModel.setPreferredWeatherTap(null) },
                )
            }
            items(
                    listOf(
                            WidgetTapPickerRow(
                                    labelRes = R.string.settings_widget_clock_app,
                                    tapTarget = uiState.preferredClockTap,
                                    pickerKey = "clock",
                                    onClear = { viewModel.setPreferredClockTap(null) },
                            ),
                            WidgetTapPickerRow(
                                    labelRes = R.string.settings_widget_calendar_app,
                                    tapTarget = uiState.preferredCalendarTap,
                                    pickerKey = "calendar",
                                    onClear = { viewModel.setPreferredCalendarTap(null) },
                            ),
                    ),
                    key = { it.labelRes },
            ) { row ->
                ShortcutTargetRow(
                        label = stringResource(row.labelRes),
                        currentTarget =
                                formatWidgetTapTarget(
                                        context = context,
                                        resources = resources,
                                        binding = row.tapTarget,
                                        allApps = uiState.allApps,
                                        allActions = uiState.allShortcutActions,
                                        emptyLabel = row.emptyLabel,
                                ),
                        onPickApp = { showAppPickerFor.value = row.pickerKey },
                        onClear = row.onClear,
                )
            }
        }
    }

    showAppPickerFor.value?.let { pickerTarget ->
        ShortcutActionPickerDialog(
                allActions = uiState.allShortcutActions,
                allApps = uiState.allApps,
                title = stringResource(R.string.edit_shortcuts_section_all_actions),
                onSelect = { action ->
                    when (pickerTarget) {
                        "weather" -> viewModel.setPreferredWeatherTap(action)
                        "clock" -> viewModel.setPreferredClockTap(action)
                        "calendar" -> viewModel.setPreferredCalendarTap(action)
                    }
                    showAppPickerFor.value = null
                },
                onDismiss = { showAppPickerFor.value = null },
                profileDisplayNameOverrides = uiState.profileDisplayNameOverrides,
        )
    }

    if (showAddOtherWidget) {
        FokusAlertDialog(
                onDismissRequest = { showAddOtherWidget = false },
                title = { Text(stringResource(R.string.settings_add_other_widget_title)) },
                text = {
                    Column {
                        HomeExtraWidgetAddType.entries.forEach { type ->
                            SettingsRow(
                                    label = stringResource(type.labelRes),
                                    subtitle =
                                            stringResource(
                                                    when (type) {
                                                        HomeExtraWidgetAddType.WORLD_CLOCK ->
                                                                R.string.settings_show_home_world_clock_subtitle
                                                        HomeExtraWidgetAddType.COUNTDOWN ->
                                                                R.string.settings_show_home_countdown_subtitle
                                                    }
                                            ),
                                    horizontalPadding = 0.dp,
                                    onClick = {
                                        viewModel.addHomeExtraWidget(type)
                                        showAddOtherWidget = false
                                        when (type) {
                                            HomeExtraWidgetAddType.WORLD_CLOCK ->
                                                    pendingEditNewestCity = true
                                            HomeExtraWidgetAddType.COUNTDOWN ->
                                                    pendingEditNewestCountdown = true
                                        }
                                    },
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    FokusTextButton(onClick = { showAddOtherWidget = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
        )
    }

    // After adding a world clock, open the newest city for editing.
    LaunchedEffect(uiState.worldClockCities, pendingEditNewestCity) {
        if (pendingEditNewestCity) {
            val newest = uiState.worldClockCities.maxByOrNull { it.position }
            if (newest != null) {
                editingCityId = newest.id
                pendingEditNewestCity = false
            }
        }
    }

    LaunchedEffect(uiState.countdownEvents, pendingEditNewestCountdown) {
        if (pendingEditNewestCountdown) {
            val newest = uiState.countdownEvents.lastOrNull()
            if (newest != null) {
                editingCountdownId = newest.id
                pendingEditNewestCountdown = false
            }
        }
    }

    editingCityId?.let { cityId ->
        val city = citiesById[cityId]
        if (city != null) {
            WorldClockCityEditDialog(
                    title = stringResource(R.string.settings_world_clock_edit_city),
                    initialLabel = city.label,
                    initialTimeZoneId = city.timeZoneId,
                    onDismiss = { editingCityId = null },
                    onSave = { label, zone ->
                        if (viewModel.updateWorldClockCity(city.id, label, zone)) {
                            editingCityId = null
                        }
                    },
            )
        }
    }

    editingCountdownId?.let { eventId ->
        val event = eventsById[eventId]
        if (event != null) {
            CountdownEditDialog(
                    initialTitle = event.title,
                    initialEpochMillis = event.targetEpochMillis,
                    onDismiss = { editingCountdownId = null },
                    onSave = { title, epochMillis ->
                        if (viewModel.saveCountdownEvent(event.id, title, epochMillis)) {
                            editingCountdownId = null
                        }
                    },
            )
        }
    }
}
