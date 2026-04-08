package com.lu4p.fokuslauncher.ui.home

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.appMetadataKey
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.HomeDateFormatStyle
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.WeatherData
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.repository.WeatherRepository
import com.lu4p.fokuslauncher.utils.LockScreenHelper
import com.lu4p.fokuslauncher.utils.isDefaultHomeApp
import com.lu4p.fokuslauncher.ui.util.formatShortcutTargetDisplay
import com.lu4p.fokuslauncher.data.util.TemperatureUnitHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DateFormat as JavaDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class HomeUiState(
    val showHomeClock: Boolean = true,
    val showHomeDate: Boolean = true,
    val showHomeWeather: Boolean = true,
    val showHomeBattery: Boolean = true,
    val isDefaultLauncher: Boolean = true,
    val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
    val doubleTapEmptyLockEnabled: Boolean = false,
)

data class HomeClockUiState(
    val currentTime: String = "",
    val currentDate: String = "",
    val batteryPercent: Int = 0,
)

data class HomeWeatherUiState(
    val weather: WeatherData? = null,
    /** Matches system regional temperature unit; drives label and Open-Meteo request. */
    val weatherUseFahrenheit: Boolean = false,
    val showWeatherWidget: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _clockUiState = MutableStateFlow(HomeClockUiState())
    val clockUiState: StateFlow<HomeClockUiState> = _clockUiState.asStateFlow()

    private val _homeDateFormatStyle = MutableStateFlow(HomeDateFormatStyle.SYSTEM_DEFAULT)

    private val _weatherUiState = MutableStateFlow(HomeWeatherUiState())
    val weatherUiState: StateFlow<HomeWeatherUiState> = _weatherUiState.asStateFlow()

    /** Serializes home app-list refresh so concurrent loads cannot race and prune favorites. */
    private val installedAppsRefreshMutex = Mutex()

    // Raw favorites from DataStore
    private val rawFavorites: StateFlow<List<FavoriteApp>> =
        preferencesManager.favoritesFlow.stateEagerly(emptyList())

    // Renames from Room
    private val _renameMap = MutableStateFlow<Map<String, String>>(emptyMap())

    // App name lookup keyed by package and profile.
    private val _appNameMap = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * Favorites with resolved display names.
     * Priority: custom rename > PackageManager name > stored label.
     */
    val favorites: StateFlow<List<FavoriteApp>> = combine(
        rawFavorites,
        _renameMap,
        _appNameMap
    ) { favs, renames, appNames ->
        favs.map { fav ->
            val appKey = appMetadataKey(fav.packageName, fav.profileKey)
            val resolvedName = renames[appKey]
                ?: appNames[appKey]
                ?: fav.label
            fav.copy(label = resolvedName)
        }
    }.stateWhileSubscribed(emptyList())

    // ── Dialog state ────────────────────────────────────────────────

    private val _appMenuTarget = MutableStateFlow<FavoriteApp?>(null)
    val appMenuTarget: StateFlow<FavoriteApp?> = _appMenuTarget.asStateFlow()

    private val _showHomeScreenMenu = MutableStateFlow(false)
    val showHomeScreenMenu: StateFlow<Boolean> = _showHomeScreenMenu.asStateFlow()

    private val _showWeatherAppPicker = MutableStateFlow(false)
    val showWeatherAppPicker: StateFlow<Boolean> = _showWeatherAppPicker.asStateFlow()

    private val _requestLockAccessibilitySettings = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestLockAccessibilitySettings = _requestLockAccessibilitySettings.asSharedFlow()

    // ── Edit screen state ───────────────────────────────────────────

    private val _allInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allInstalledApps: StateFlow<List<AppInfo>> = _allInstalledApps.asStateFlow()

    private val _allShortcutActions = MutableStateFlow<List<AppShortcutAction>>(emptyList())
    val allShortcutActions: StateFlow<List<AppShortcutAction>> = _allShortcutActions.asStateFlow()

    private val _editFavorites = MutableStateFlow<List<FavoriteApp>>(emptyList())
    val editFavorites: StateFlow<List<FavoriteApp>> = _editFavorites.asStateFlow()

    private val _editRightShortcuts = MutableStateFlow<List<HomeShortcut>>(emptyList())
    val editRightShortcuts: StateFlow<List<HomeShortcut>> = _editRightShortcuts.asStateFlow()

    // ── Swipe gestures ──────────────────────────────────────────────

    val swipeLeftTarget: StateFlow<ShortcutTarget?> =
        preferencesManager.swipeLeftTargetFlow.stateWhileSubscribed(null)

    val swipeRightTarget: StateFlow<ShortcutTarget?> =
        preferencesManager.swipeRightTargetFlow.stateWhileSubscribed(null)

    val rightSideShortcuts: StateFlow<List<HomeShortcut>> =
        preferencesManager.rightSideShortcutsFlow.stateWhileSubscribed(emptyList())

    private val preferredWeatherAppPackage: StateFlow<String> =
        preferencesManager.preferredWeatherAppFlow.stateEagerly("")

    private val preferredClockAppPackage: StateFlow<String> =
        preferencesManager.preferredClockAppFlow.stateEagerly("")

    private val preferredCalendarAppPackage: StateFlow<String> =
        preferencesManager.preferredCalendarAppFlow.stateEagerly("")

    private var weatherTickerJob: Job? = null

    private val batteryChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            setBatteryPercentFromIntent(intent)
        }
    }

    /**
     * JVM default timezone is fixed at process start and does not track system changes until we
     * handle [Intent.ACTION_TIMEZONE_CHANGED] and refresh cached formatters.
     */
    @Volatile
    private var clockFormatNeedsRefresh = false

    private val timezoneChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_TIMEZONE_CHANGED) return
            applySystemTimeZoneChange(intent.getStringExtra(TIMEZONE_CHANGED_EXTRA_ID))
        }
    }

    /** Apply Android system timezone to the JVM default and drop cached time formatters. */
    internal fun applySystemTimeZoneChange(timeZoneId: String?) {
        timeZoneId?.let { TimeZone.setDefault(TimeZone.getTimeZone(it)) }
        clockFormatNeedsRefresh = true
    }

    init {
        viewModelScope.launch {
            preferencesManager.ensureRightSideShortcutsInitialized()
            preferencesManager.migrateLegacyDialerShortcutTargets()
        }
        startClockTicker()
        registerBatteryReceiver()
        registerTimezoneChangedReceiver()
        updateBattery()
        observeHomeAlignment()
        observeHomeDateFormatStyle()
        observeHomeWidgetItemPreferences()
        observeWeatherRefreshTriggers()
        observeDoubleTapEmptyLock()
        checkDefaultLauncher()
        refreshInstalledAppsAndShortcutActions()
        observeRenames()
        observeInstalledApps()
        observeRemovedPackages()
    }

    override fun onCleared() {
        listOf(batteryChangedReceiver, timezoneChangedReceiver).forEach { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Not registered
            }
        }
        weatherTickerJob?.cancel()
        super.onCleared()
    }

    // ── Name resolution ─────────────────────────────────────────────

    private fun observeRenames() {
        viewModelScope.launch {
            appRepository.getAllRenamedApps().collect { renamedApps ->
                _renameMap.value =
                    renamedApps.associate {
                        appMetadataKey(it.packageName, it.profileKey) to it.customName
                    }
            }
        }
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            appRepository.getInstalledAppsVersion().drop(1).collect {
                refreshInstalledAppsAndShortcutActions(forceReloadApps = false)
            }
        }
    }

    private fun observeRemovedPackages() {
        viewModelScope.launch {
            appRepository.getRemovedPackages().collect { removedApp ->
                _allInstalledApps.value =
                    _allInstalledApps.value.filterNot {
                        it.packageName == removedApp.packageName &&
                            appProfileKey(it.userHandle) == removedApp.profileKey
                    }
                _appNameMap.value =
                    _appNameMap.value
                        .toMutableMap()
                        .apply {
                            val removedKey =
                                appMetadataKey(removedApp.packageName, removedApp.profileKey)
                            remove(removedKey)
                            val hasPrimaryInstall =
                                _allInstalledApps.value.any {
                                    it.packageName == removedApp.packageName && it.userHandle == null
                                }
                            if (!hasPrimaryInstall && removedApp.profileKey == "0") {
                                remove(appMetadataKey(removedApp.packageName, "0"))
                            }
                        }
                _editFavorites.value =
                    _editFavorites.value.filterNot {
                        it.packageName == removedApp.packageName &&
                            it.profileKey == removedApp.profileKey
                    }

                val currentFavorites = rawFavorites.value
                val updatedFavorites =
                    currentFavorites.filterNot {
                        it.packageName == removedApp.packageName &&
                            it.profileKey == removedApp.profileKey
                    }
                if (updatedFavorites.size != currentFavorites.size) {
                    preferencesManager.setFavorites(updatedFavorites)
                }
            }
        }
    }

    /**
     * Pre-warms the app cache and builds the package-name → label map
     * used to resolve real app names for home-screen favorites.
     */
    fun refreshInstalledApps(forceReload: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshInstalledAppsLocked(forceReload)
        }
    }

    private fun refreshInstalledAppsAndShortcutActions(forceReloadApps: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshInstalledAppsLocked(forceReloadApps)
            _allShortcutActions.value = appRepository.getAllShortcutActions()
        }
    }

    private suspend fun refreshInstalledAppsLocked(forceReload: Boolean) {
        installedAppsRefreshMutex.withLock {
            val recoveredAfterFirstPass = runInstalledAppsRefreshPass(forceReload)
            if (recoveredAfterFirstPass) {
                appRepository.invalidateCache()
                runInstalledAppsRefreshPass(forceReload = false)
            }
        }
    }

    /**
     * Reloads installed apps and syncs persisted favorites. Returns true when at least one current
     * favorite was missing from the snapshot but still resolves via a batched launchability scan
     * (partial [LauncherApps] enumeration); callers may invalidate and run another pass.
     */
    private suspend fun runInstalledAppsRefreshPass(forceReload: Boolean): Boolean {
        if (forceReload) {
            appRepository.invalidateCache()
        }
        val apps = appRepository.getInstalledApps()
        if (apps.isEmpty() &&
                        (rawFavorites.value.isNotEmpty() || _allInstalledApps.value.isNotEmpty())
        ) {
            return false
        }
        applyInstalledAppsSnapshot(apps)
        val installedAppKeys = apps.map { appMetadataKey(it.packageName, it.userHandle) }.toSet()
        val currentFavorites = rawFavorites.value
        val nonSentinelFavorites =
                currentFavorites.filterNot { it.isPhoneFavoriteSentinel() }
        val missingFavoriteKeys =
                nonSentinelFavorites
                        .asSequence()
                        .map { appMetadataKey(it.packageName, it.profileKey) }
                        .filterNot(installedAppKeys::contains)
                        .toSet()
        val launchableMissingFavoriteKeys =
                if (missingFavoriteKeys.isEmpty()) {
                    emptySet()
                } else {
                    appRepository.getLaunchableAppKeys(
                            nonSentinelFavorites
                                    .asSequence()
                                    .filter {
                                        appMetadataKey(it.packageName, it.profileKey) in
                                                missingFavoriteKeys
                                    }
                                    .map(FavoriteApp::profileKey)
                                    .toSet()
                    ).intersect(missingFavoriteKeys)
                }
        val updatedFavorites =
                currentFavorites.filter {
                    it.packageName == ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE ||
                            appMetadataKey(it.packageName, it.profileKey) in installedAppKeys ||
                            appMetadataKey(it.packageName, it.profileKey) in
                                    launchableMissingFavoriteKeys
                }
        if (updatedFavorites.size != currentFavorites.size) {
            preferencesManager.setFavorites(updatedFavorites)
        }
        val recoveredViaLaunchCheck = launchableMissingFavoriteKeys.isNotEmpty()
        return recoveredViaLaunchCheck
    }

    private fun applyInstalledAppsSnapshot(apps: List<AppInfo>) {
        _allInstalledApps.value = apps
        _appNameMap.value = apps.associate { appMetadataKey(it.packageName, it.userHandle) to it.label }
    }

    private suspend fun loadInstalledAppsForEditing(
        includeShortcutActions: Boolean = false
    ) {
        val apps = appRepository.getInstalledApps()
        applyInstalledAppsSnapshot(apps)
        if (includeShortcutActions) {
            _allShortcutActions.value = appRepository.getAllShortcutActions()
        }
    }

    // ── Long-press → open app menu directly ────────────────────────

    fun onFavoriteLongPress(fav: FavoriteApp) {
        _appMenuTarget.value = fav
    }

    fun onHomeScreenLongPress() {
        _showHomeScreenMenu.value = true
    }

    fun dismissHomeScreenMenu() {
        _showHomeScreenMenu.value = false
    }

    // ── Edit flows ──────────────────────────────────────────────────

    fun startEditingHomeApps() = startEditingHome(includeShortcutActions = false)

    fun startEditingShortcuts() = startEditingHome(includeShortcutActions = true)

    private fun startEditingHome(includeShortcutActions: Boolean) {
        _appMenuTarget.value = null
        if (includeShortcutActions) {
            _editRightShortcuts.value = rightSideShortcuts.value
        } else {
            _editFavorites.value = favorites.value
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadInstalledAppsForEditing(includeShortcutActions)
        }
    }

    fun toggleAppOnHomeScreen(app: AppInfo) {
        val current = _editFavorites.value.toMutableList()
        val profileKey = appProfileKey(app.userHandle)
        val existing = current.indexOfFirst { it.packageName == app.packageName && it.profileKey == profileKey }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            val resolvedName =
                _renameMap.value[appMetadataKey(app.packageName, profileKey)] ?: app.label
            current.add(
                FavoriteApp(
                    label = resolvedName,
                    packageName = app.packageName,
                    iconName = "circle",
                    iconPackage = "",
                    profileKey = profileKey,
                )
            )
        }
        _editFavorites.value = current
    }

    private fun <T> reorderInList(items: List<T>, from: Int, to: Int): List<T>? {
        if (from !in items.indices || to !in items.indices) return null
        val next = items.toMutableList()
        val item = next.removeAt(from)
        next.add(to, item)
        return next
    }

    fun reorderFavorite(from: Int, to: Int) {
        _editFavorites.value = reorderInList(_editFavorites.value, from, to) ?: return
    }

    fun saveEditedFavorites() {
        viewModelScope.launch {
            preferencesManager.setFavorites(_editFavorites.value)
        }
    }

    fun toggleRightShortcut(action: AppShortcutAction) {
        val current = _editRightShortcuts.value.toMutableList()
        val existing =
                current.indexOfFirst {
                    it.target == action.target && it.profileKey == action.profileKey
                }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(
                HomeShortcut(
                    iconName = inferIconNameForAction(action),
                    target = action.target,
                    profileKey = action.profileKey,
                )
            )
        }
        _editRightShortcuts.value = current
    }

    fun reorderRightShortcut(from: Int, to: Int) {
        _editRightShortcuts.value = reorderInList(_editRightShortcuts.value, from, to) ?: return
    }

    fun updateShortcutIcon(index: Int, iconName: String) {
        val current = _editRightShortcuts.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(iconName = iconName)
            _editRightShortcuts.value = current
        }
    }

    fun saveEditedRightShortcuts() {
        viewModelScope.launch {
            preferencesManager.setRightSideShortcuts(_editRightShortcuts.value)
        }
    }

    // ── Remove ──────────────────────────────────────────────────────

    fun removeFavorite(fav: FavoriteApp) {
        viewModelScope.launch {
            val current = rawFavorites.value.toMutableList()
            current.removeAll { it.packageName == fav.packageName && it.profileKey == fav.profileKey }
            preferencesManager.setFavorites(current)
        }
        _appMenuTarget.value = null
    }

    fun dismissAppMenu() {
        _appMenuTarget.value = null
    }

    fun openWeatherAppPicker() {
        val preferredPackage = preferredWeatherAppPackage.value
        if (preferredPackage.isBlank()) {
            _showWeatherAppPicker.value = true
            return
        }

        val launched = appRepository.launchApp(preferredPackage)
        if (!launched) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_weather_app_launch_failed),
                Toast.LENGTH_SHORT
            ).show()
            _showWeatherAppPicker.value = true
        }
    }

    fun closeWeatherAppPicker() {
        _showWeatherAppPicker.value = false
    }

    fun setPreferredWeatherApp(packageName: String) {
        viewModelScope.launch {
            preferencesManager.setPreferredWeatherApp(packageName)
            _showWeatherAppPicker.value = false
        }
    }

    fun renameApp(favorite: FavoriteApp, newName: String) {
        viewModelScope.launch {
            if (favorite.packageName == ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE) {
                val current = preferencesManager.favoritesFlow.first().toMutableList()
                val idx =
                        current.indexOfFirst {
                            it.packageName == favorite.packageName &&
                                it.profileKey == favorite.profileKey
                        }
                if (idx >= 0) {
                    current[idx] = current[idx].copy(label = newName.trim())
                    preferencesManager.setFavorites(current)
                }
            } else {
                appRepository.renameApp(favorite.packageName, favorite.profileKey, newName)
            }
            _appMenuTarget.value = null
        }
    }

    fun hideApp(favorite: FavoriteApp) {
        if (favorite.isPhoneFavoriteSentinel()) {
            _appMenuTarget.value = null
            return
        }
        viewModelScope.launch {
            appRepository.hideApp(favorite.packageName, favorite.profileKey)
            // Also remove from home-screen favorites
            val current = rawFavorites.value.toMutableList()
            current.removeAll {
                it.packageName == favorite.packageName && it.profileKey == favorite.profileKey
            }
            preferencesManager.setFavorites(current)
        }
        _appMenuTarget.value = null
    }

    fun openAppInfo(favorite: FavoriteApp) {
        if (favorite.isPhoneFavoriteSentinel()) {
            _appMenuTarget.value = null
            return
        }
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${favorite.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
        _appMenuTarget.value = null
    }

    fun uninstallApp(favorite: FavoriteApp) {
        if (favorite.isPhoneFavoriteSentinel()) {
            _appMenuTarget.value = null
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = "package:${favorite.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
        _appMenuTarget.value = null
    }

    // ── Clock / Battery / Weather ───────────────────────────────────

    private fun startClockTicker() {
        viewModelScope.launch {
            var lastLocale: Locale? = null
            var lastIs24Hour: Boolean? = null
            var timeFormat: JavaDateFormat? = null
            while (true) {
                if (clockFormatNeedsRefresh) {
                    clockFormatNeedsRefresh = false
                    timeFormat = null
                }
                val now = Date()
                val locale = Locale.getDefault()
                val is24Hour = DateFormat.is24HourFormat(context)
                if (
                    locale != lastLocale ||
                        is24Hour != lastIs24Hour ||
                        timeFormat == null
                ) {
                    lastLocale = locale
                    lastIs24Hour = is24Hour
                    timeFormat = DateFormat.getTimeFormat(context)
                }
                val current = _clockUiState.value
                val updated =
                    current.copy(
                        currentTime = timeFormat.format(now),
                        currentDate =
                                formatHomeDate(now, locale, _homeDateFormatStyle.value)
                    )
                if (updated != current) {
                    _clockUiState.value = updated
                }
                delay(1_000)
            }
        }
    }

    private fun registerPrivateNotExportedReceiver(
        receiver: BroadcastReceiver,
        filter: IntentFilter
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) { }
    }

    private fun registerBatteryReceiver() {
        registerPrivateNotExportedReceiver(
            batteryChangedReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    private fun registerTimezoneChangedReceiver() {
        registerPrivateNotExportedReceiver(
            timezoneChangedReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        )
    }

    private fun setBatteryPercentFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val current = _clockUiState.value
        if (current.batteryPercent != percent) {
            _clockUiState.value = current.copy(batteryPercent = percent)
        }
    }

    private fun updateBattery() {
        try {
            val batteryIntent =
                    ContextCompat.registerReceiver(
                            context,
                            null,
                            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                            ContextCompat.RECEIVER_NOT_EXPORTED
                    )
            if (batteryIntent != null) {
                setBatteryPercentFromIntent(batteryIntent)
            } else {
                _clockUiState.value = _clockUiState.value.copy(batteryPercent = 0)
            }
        } catch (_: Exception) {
            _clockUiState.value = _clockUiState.value.copy(batteryPercent = 0)
        }
    }

    private fun startWeatherTicker() {
        if (weatherTickerJob?.isActive == true) return
        weatherTickerJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchWeatherOnce()
                delay(30 * 60 * 1000L)
            }
        }
    }

    private fun stopWeatherTicker() {
        weatherTickerJob?.cancel()
        weatherTickerJob = null
    }

    private fun observeHomeAlignment() {
        observeFlow(preferencesManager.homeAlignmentFlow) { alignment ->
            _uiState.value = _uiState.value.copy(homeAlignment = alignment)
        }
    }

    private fun observeHomeDateFormatStyle() {
        observeFlow(preferencesManager.homeDateFormatStyleFlow) { style ->
            _homeDateFormatStyle.value = style
            val now = Date()
            val locale = Locale.getDefault()
            val current = _clockUiState.value
            _clockUiState.value = current.copy(currentDate = formatHomeDate(now, locale, style))
        }
    }

    private fun observeHomeWidgetItemPreferences() {
        viewModelScope.launch {
            combine(
                    preferencesManager.showHomeClockFlow,
                    preferencesManager.showHomeDateFlow,
                    preferencesManager.showHomeWeatherFlow,
                    preferencesManager.showHomeBatteryFlow,
            ) { showClock, showDate, showWeather, showBattery ->
                _uiState.value =
                        _uiState.value.copy(
                                showHomeClock = showClock,
                                showHomeDate = showDate,
                                showHomeWeather = showWeather,
                                showHomeBattery = showBattery,
                        )
            }.collect { }
        }
    }

    private fun observeWeatherRefreshTriggers() {
        observeFlow(preferencesManager.showHomeWeatherFlow.distinctUntilChanged()) { showWeather ->
            if (showWeather) {
                refreshWeather()
                startWeatherTicker()
            } else {
                stopWeatherTicker()
                applyWeatherUiState(hiddenWeatherState())
            }
        }
    }

    private fun observeDoubleTapEmptyLock() {
        observeFlow(preferencesManager.doubleTapEmptyLockFlow, ::recomputeDoubleTapEmptyLockUi)
    }

    fun refreshDoubleTapLockEffective() {
        viewModelScope.launch {
            recomputeDoubleTapEmptyLockUi(preferencesManager.doubleTapEmptyLockFlow.first())
        }
    }

    private fun recomputeDoubleTapEmptyLockUi(prefEnabled: Boolean) {
        val svcEnabled = LockScreenHelper.isLockAccessibilityServiceEnabled(context)
        _uiState.value =
            _uiState.value.copy(doubleTapEmptyLockEnabled = prefEnabled && svcEnabled)
    }

    private fun checkDefaultLauncher() {
        try {
            _uiState.value =
                    _uiState.value.copy(isDefaultLauncher = context.isDefaultHomeApp())
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(isDefaultLauncher = false)
        }
    }

    fun recheckDefaultLauncher() = checkDefaultLauncher()

    /** Double-tap on the empty region above home screen apps; locks via accessibility if enabled. */
    fun onDoubleTapEmptyLock() {
        viewModelScope.launch {
            if (!preferencesManager.doubleTapEmptyLockFlow.first()) return@launch
            if (!LockScreenHelper.isLockAccessibilityServiceEnabled(context)) return@launch
            if (LockScreenHelper.lockScreenIfPossible()) return@launch
            Toast.makeText(context, R.string.double_tap_lock_failed, Toast.LENGTH_SHORT).show()
            _requestLockAccessibilitySettings.emit(Unit)
        }
    }

    fun openDefaultLauncherSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) { }
        }
    }

    fun refreshBattery() = updateBattery()
    fun refreshWeather() {
        viewModelScope.launch(Dispatchers.IO) { fetchWeatherOnce() }
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
    }

    fun launchFavorite(fav: FavoriteApp) {
        launchShortcutTarget(fav.resolvedIconTarget, fav.profileKey)
    }

    fun launchShortcut(shortcut: HomeShortcut) {
        launchShortcutTarget(shortcut.target, shortcut.profileKey)
    }

    private fun launchShortcutTarget(target: ShortcutTarget, profileKey: String) {
        when (target) {
            is ShortcutTarget.PhoneDial -> launchDefaultDialer()
            is ShortcutTarget.DeepLink -> launchDeepLink(target.intentUri)
            is ShortcutTarget.LauncherShortcut -> {
                val user = resolveUserHandleForShortcut(profileKey, target.packageName)
                appRepository.launchLauncherShortcut(
                    target.packageName,
                    target.shortcutId,
                    user,
                )
            }
            is ShortcutTarget.App -> launchAppTarget(target.packageName, profileKey)
        }
    }

    private fun launchAppTarget(packageName: String, profileKey: String): Boolean {
        if (profileKey != "0") {
            val app =
                    _allInstalledApps.value.firstOrNull {
                        it.packageName == packageName &&
                                appProfileKey(it.userHandle) == profileKey
                    }
            val componentName = app?.componentName
            val userHandle = app?.userHandle
            if (componentName != null &&
                            userHandle != null &&
                            appRepository.launchMainActivity(componentName, userHandle)
            ) {
                return true
            }
        }
        return appRepository.launchApp(packageName)
    }

    private fun launchDeepLink(intentUri: String) {
        val intent =
                try {
                    Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW, intentUri.toUri())
                }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // ignore malformed/unresolvable deep links
        }
    }

    private fun launchDefaultDialer() {
        try {
            context.startActivity(
                    Intent(Intent.ACTION_DIAL, "tel:".toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            )
        } catch (_: Exception) { }
    }

    private fun resolveUserHandleForShortcut(profileKey: String, packageName: String): UserHandle {
        if (profileKey == "0") return Process.myUserHandle()
        val app =
                _allInstalledApps.value.firstOrNull {
                    it.packageName == packageName && appProfileKey(it.userHandle) == profileKey
                }
        return app?.userHandle ?: Process.myUserHandle()
    }

    fun formatShortcutTarget(target: ShortcutTarget, profileKey: String = "0"): String {
        val apps = _allInstalledApps.value
        val resolvedLabel =
                if (target is ShortcutTarget.LauncherShortcut) {
                    _allShortcutActions.value
                        .firstOrNull { it.target == target && it.profileKey == profileKey }
                        ?.actionLabel
                } else {
                    null
                }
        return formatShortcutTargetDisplay(
                context = context,
                target = target,
                allApps = apps,
                notSetLabel = context.getString(R.string.shortcut_target_not_set),
                resolvedLauncherActionLabel = resolvedLabel,
                profileKey = profileKey,
        )
    }

    private fun inferIconNameForAction(action: AppShortcutAction): String {
        val value = "${action.appLabel} ${action.actionLabel}".lowercase()
        return when {
            value.contains("music") -> "music"
            value.contains("work") || value.contains("mail") -> "work"
            value.contains("chat") || value.contains("message") -> "chat"
            value.contains("call") || value.contains("dial") || value.contains("dialer") || value.contains("phone") -> "call"
            value.contains("camera") -> "camera"
            value.contains("photo") || value.contains("gallery") -> "gallery"
            value.contains("video") -> "video"
            value.contains("map") || value.contains("direction") -> "map"
            else -> "circle"
        }
    }

    /**
     * Opens the default clock / alarm app.
     */
    fun openClockApp() {
        val overridePkg = preferredClockAppPackage.value
        if (overridePkg.isNotBlank() && appRepository.launchApp(overridePkg)) {
            return
        }
        val showAlarms =
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        try {
            context.startActivity(showAlarms)
            return
        } catch (_: Exception) { }

        val pm = context.packageManager
        for (pkg in CLOCK_LAUNCH_PACKAGES) {
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            try {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return
            } catch (_: Exception) { }
        }
    }

    /**
     * Opens the default calendar app.
     */
    fun openCalendarApp() {
        val overridePkg = preferredCalendarAppPackage.value
        if (overridePkg.isNotBlank() && appRepository.launchApp(overridePkg)) {
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon()
                    .appendPath("time")
                    .appendPath(System.currentTimeMillis().toString())
                    .build()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    private suspend fun fetchWeatherOnce() {
        try {
            if (!_uiState.value.showHomeWeather) {
                applyWeatherUiState(hiddenWeatherState())
                return
            }
            val useFahrenheit = TemperatureUnitHelper.useFahrenheit(context)
            val hasCoarsePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasCoarsePermission) {
                applyWeatherUiState(hiddenWeatherState(useFahrenheit))
                return
            }
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @Suppress("MissingPermission")
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val updated = HomeWeatherUiState(
                weather =
                    location?.let {
                        weatherRepository.getWeather(
                            it.latitude,
                            it.longitude,
                            useFahrenheit = useFahrenheit
                        )
                    },
                weatherUseFahrenheit = useFahrenheit,
                showWeatherWidget = true
            )
            applyWeatherUiState(updated)
        } catch (_: Exception) { }
    }

    private fun hiddenWeatherState(
        useFahrenheit: Boolean = TemperatureUnitHelper.useFahrenheit(context)
    ): HomeWeatherUiState =
        HomeWeatherUiState(
            weather = null,
            weatherUseFahrenheit = useFahrenheit,
            showWeatherWidget = false
        )

    private fun applyWeatherUiState(updated: HomeWeatherUiState) {
        if (_weatherUiState.value != updated) {
            _weatherUiState.value = updated
        }
    }

    private fun <T> Flow<T>.stateWhileSubscribed(initial: T): StateFlow<T> =
            stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    private fun <T> Flow<T>.stateEagerly(initial: T): StateFlow<T> =
            stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    private inline fun <T> observeFlow(
            flow: Flow<T>,
            crossinline onEach: (T) -> Unit,
    ) {
        viewModelScope.launch {
            flow.collect { onEach(it) }
        }
    }

    private fun FavoriteApp.isPhoneFavoriteSentinel(): Boolean =
            packageName == ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE

    private companion object {
        /**
         * [Intent.EXTRA_TIMEZONE] documents this key but the constant is not inlined below API 30;
         * [Intent.ACTION_TIMEZONE_CHANGED] has used `"time-zone"` since early Android.
         */
        private const val TIMEZONE_CHANGED_EXTRA_ID = "time-zone"

        /** OEM / AOSP clock packages as fallback when [AlarmClock.ACTION_SHOW_ALARMS] is unavailable. */
        val CLOCK_LAUNCH_PACKAGES =
                listOf(
                        "com.sec.android.app.clockpackage",
                        "com.google.android.deskclock",
                        "com.android.deskclock",
                )
    }
}
