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
import com.lu4p.fokuslauncher.data.model.appProfileKey
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private val _weatherUiState = MutableStateFlow(HomeWeatherUiState())
    val weatherUiState: StateFlow<HomeWeatherUiState> = _weatherUiState.asStateFlow()

    // Raw favorites from DataStore
    private val rawFavorites: StateFlow<List<FavoriteApp>> = preferencesManager.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Renames from Room
    private val _renameMap = MutableStateFlow<Map<String, String>>(emptyMap())

    // App name lookup (packageName -> real label from PackageManager)
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
            val resolvedName = renames[fav.packageName]
                ?: appNames[fav.packageName]
                ?: fav.label
            fav.copy(label = resolvedName)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    val swipeLeftTarget: StateFlow<ShortcutTarget?> = preferencesManager.swipeLeftTargetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val swipeRightTarget: StateFlow<ShortcutTarget?> = preferencesManager.swipeRightTargetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rightSideShortcuts: StateFlow<List<HomeShortcut>> = preferencesManager.rightSideShortcutsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val preferredWeatherAppPackage: StateFlow<String> =
        preferencesManager.preferredWeatherAppFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val preferredClockAppPackage: StateFlow<String> =
        preferencesManager.preferredClockAppFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val preferredCalendarAppPackage: StateFlow<String> =
        preferencesManager.preferredCalendarAppFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val batteryChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            setBatteryPercentFromIntent(intent)
        }
    }

    init {
        viewModelScope.launch {
            preferencesManager.ensureRightSideShortcutsInitialized()
        }
        startClockTicker()
        registerBatteryReceiver()
        updateBattery()
        startWeatherTicker()
        observeHomeAlignment()
        observeHomeWidgetItemPreferences()
        observeWeatherRefreshTriggers()
        observeDoubleTapEmptyLock()
        checkDefaultLauncher()
        refreshInstalledApps()
        loadShortcutActions()
        observeRenames()
        observeInstalledApps()
        observeRemovedPackages()
    }

    override fun onCleared() {
        try {
            context.unregisterReceiver(batteryChangedReceiver)
        } catch (_: IllegalArgumentException) {
            // Not registered
        }
        super.onCleared()
    }

    // ── Name resolution ─────────────────────────────────────────────

    private fun observeRenames() {
        viewModelScope.launch {
            appRepository.getAllRenamedApps().collect { renamedApps ->
                _renameMap.value = renamedApps.associate { it.packageName to it.customName }
            }
        }
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            appRepository.getInstalledAppsVersion().drop(1).collect {
                refreshInstalledApps(forceReload = false)
                loadShortcutActions()
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
                            val hasPrimaryInstall =
                                _allInstalledApps.value.any {
                                    it.packageName == removedApp.packageName && it.userHandle == null
                                }
                            if (!hasPrimaryInstall) {
                                remove(removedApp.packageName)
                            }
                        }
                _editFavorites.value =
                    _editFavorites.value.filterNot {
                        it.packageName == removedApp.packageName &&
                            it.profileKey == removedApp.profileKey
                    }

                val currentFavorites = preferencesManager.favoritesFlow.first()
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
            if (forceReload) {
                appRepository.invalidateCache()
            }
            val apps = appRepository.getInstalledApps()
            if (apps.isEmpty() && (rawFavorites.value.isNotEmpty() || _allInstalledApps.value.isNotEmpty())) {
                return@launch
            }
            _appNameMap.value = apps.associate { it.packageName to it.label }
            _allInstalledApps.value = apps
            val installedPackages = apps.map { it.packageName }.toSet()
            val currentFavorites = rawFavorites.value
            val updatedFavorites = currentFavorites.filter { it.packageName in installedPackages }
            if (updatedFavorites.size != currentFavorites.size) {
                preferencesManager.setFavorites(updatedFavorites)
            }
        }
    }

    private fun loadShortcutActions() {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun startEditingHomeApps() {
        _appMenuTarget.value = null
        _editFavorites.value = favorites.value
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.getInstalledApps()
            _allInstalledApps.value = apps
            _appNameMap.value = apps.associate { it.packageName to it.label }
        }
    }

    fun startEditingShortcuts() {
        _appMenuTarget.value = null
        _editRightShortcuts.value = rightSideShortcuts.value
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.getInstalledApps()
            _allInstalledApps.value = apps
            _appNameMap.value = apps.associate { it.packageName to it.label }
            _allShortcutActions.value = appRepository.getAllShortcutActions()
        }
    }

    fun toggleAppOnHomeScreen(app: AppInfo) {
        val current = _editFavorites.value.toMutableList()
        val profileKey = appProfileKey(app.userHandle)
        val existing = current.indexOfFirst { it.packageName == app.packageName && it.profileKey == profileKey }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            val resolvedName = _renameMap.value[app.packageName] ?: app.label
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

    fun reorderFavorite(from: Int, to: Int) {
        val current = _editFavorites.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            _editFavorites.value = current
        }
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
        val current = _editRightShortcuts.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            _editRightShortcuts.value = current
        }
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

    fun renameApp(packageName: String, newName: String) {
        viewModelScope.launch {
            appRepository.renameApp(packageName, newName)
            _appMenuTarget.value = null
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.hideApp(packageName)
            // Also remove from home-screen favorites
            val current = rawFavorites.value.toMutableList()
            current.removeAll { it.packageName == packageName }
            preferencesManager.setFavorites(current)
        }
        _appMenuTarget.value = null
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
        _appMenuTarget.value = null
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = "package:$packageName".toUri()
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
            var timeFormat: SimpleDateFormat? = null
            while (true) {
                val now = Date()
                val locale = Locale.getDefault()
                if (locale != lastLocale || timeFormat == null) {
                    lastLocale = locale
                    timeFormat = SimpleDateFormat("H:mm", locale)
                }
                val current = _clockUiState.value
                val updated =
                    current.copy(
                        currentTime = timeFormat.format(now),
                        currentDate = formatCompactDate(now, locale)
                    )
                if (updated != current) {
                    _clockUiState.value = updated
                }
                delay(1_000)
            }
        }
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    batteryChangedReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(batteryChangedReceiver, filter)
            }
        } catch (_: Exception) { }
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
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchWeatherOnce()
                delay(30 * 60 * 1000L)
            }
        }
    }

    private fun observeHomeAlignment() {
        viewModelScope.launch {
            preferencesManager.homeAlignmentFlow.collect { alignment ->
                _uiState.value = _uiState.value.copy(homeAlignment = alignment)
            }
        }
    }

    private fun observeHomeWidgetItemPreferences() {
        viewModelScope.launch {
            combine(
                preferencesManager.showHomeClockFlow,
                preferencesManager.showHomeDateFlow,
                preferencesManager.showHomeWeatherFlow,
                preferencesManager.showHomeBatteryFlow
            ) { showClock, showDate, showWeather, showBattery ->
                WidgetItemPreferences(
                        showClock,
                        showDate,
                        showWeather,
                        showBattery
                )
            }.collect { w ->
                _uiState.value =
                        _uiState.value.copy(
                                showHomeClock = w.showClock,
                                showHomeDate = w.showDate,
                                showHomeWeather = w.showWeather,
                                showHomeBattery = w.showBattery
                        )
            }
        }
    }

    private data class WidgetItemPreferences(
            val showClock: Boolean,
            val showDate: Boolean,
            val showWeather: Boolean,
            val showBattery: Boolean
    )

    private fun observeWeatherRefreshTriggers() {
        viewModelScope.launch {
            preferencesManager.showHomeWeatherFlow
                    .distinctUntilChanged()
                    .collect { refreshWeather() }
        }
    }

    private fun observeDoubleTapEmptyLock() {
        viewModelScope.launch {
            preferencesManager.doubleTapEmptyLockFlow.collect { prefEnabled ->
                val svcEnabled = LockScreenHelper.isLockAccessibilityServiceEnabled(context)
                _uiState.value =
                        _uiState.value.copy(doubleTapEmptyLockEnabled = prefEnabled && svcEnabled)
            }
        }
    }

    fun refreshDoubleTapLockEffective() {
        viewModelScope.launch {
            val prefEnabled = preferencesManager.doubleTapEmptyLockFlow.first()
            val svcEnabled = LockScreenHelper.isLockAccessibilityServiceEnabled(context)
            _uiState.value =
                    _uiState.value.copy(doubleTapEmptyLockEnabled = prefEnabled && svcEnabled)
        }
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
        if (fav.profileKey != "0") {
            val app =
                    _allInstalledApps.value.firstOrNull {
                        it.packageName == fav.packageName && appProfileKey(it.userHandle) == fav.profileKey
                    }
            val cn = app?.componentName
            val uh = app?.userHandle
            if (cn != null && uh != null && appRepository.launchMainActivity(cn, uh)) return
        }
        appRepository.launchApp(fav.packageName)
    }

    fun launchShortcut(shortcut: HomeShortcut) {
        when (val target = shortcut.target) {
            is ShortcutTarget.App -> {
                if (shortcut.profileKey != "0") {
                    val app =
                            _allInstalledApps.value.firstOrNull {
                                it.packageName == target.packageName &&
                                        appProfileKey(it.userHandle) == shortcut.profileKey
                            }
                    val cn = app?.componentName
                    val uh = app?.userHandle
                    if (cn != null && uh != null && appRepository.launchMainActivity(cn, uh)) return
                }
                launchApp(target.packageName)
            }
            is ShortcutTarget.LauncherShortcut -> {
                val user =
                        resolveUserHandleForShortcut(shortcut.profileKey, target.packageName)
                appRepository.launchLauncherShortcut(
                        target.packageName,
                        target.shortcutId,
                        user,
                )
            }
            is ShortcutTarget.DeepLink -> {
                val intent = try {
                    Intent.parseUri(target.intentUri, Intent.URI_INTENT_SCHEME)
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW, target.intentUri.toUri())
                }
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // ignore malformed/unresolvable deep links
                }
            }
        }
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
            value.contains("call") || value.contains("dial") -> "call"
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
            val useFahrenheit = TemperatureUnitHelper.useFahrenheit(context)
            if (!preferencesManager.showHomeWeatherFlow.first()) {
                val hiddenState = HomeWeatherUiState(
                        weather = null,
                        weatherUseFahrenheit = useFahrenheit,
                        showWeatherWidget = false
                )
                if (_weatherUiState.value != hiddenState) {
                    _weatherUiState.value = hiddenState
                }
                return
            }
            val hasCoarsePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val shouldShow = hasCoarsePermission
            if (!shouldShow) {
                val hiddenState = HomeWeatherUiState(
                    weather = null,
                    weatherUseFahrenheit = useFahrenheit,
                    showWeatherWidget = false
                )
                if (_weatherUiState.value != hiddenState) {
                    _weatherUiState.value = hiddenState
                }
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
            if (_weatherUiState.value != updated) {
                _weatherUiState.value = updated
            }
        } catch (_: Exception) { }
    }

    private companion object {
        /** OEM / AOSP clock packages as fallback when [AlarmClock.ACTION_SHOW_ALARMS] is unavailable. */
        val CLOCK_LAUNCH_PACKAGES =
                listOf(
                        "com.sec.android.app.clockpackage",
                        "com.google.android.deskclock",
                        "com.android.deskclock",
                )
    }
}

internal fun formatCompactDate(date: Date, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEE d MMM")
    return SimpleDateFormat(pattern, locale)
        .format(date)
        .replace(",", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
