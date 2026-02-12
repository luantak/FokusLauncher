package com.lu4p.fokuslauncher.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.app.ActivityOptions
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.WeatherData
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.repository.WeatherRepository
import com.lu4p.fokuslauncher.utils.WallpaperHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val currentTime: String = "",
    val currentDate: String = "",
    val batteryPercent: Int = 0,
    val weather: WeatherData? = null,
    val showWeatherWidget: Boolean = false,
    val showWallpaper: Boolean = false,
    val isDefaultLauncher: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager,
    private val weatherRepository: WeatherRepository,
    val wallpaperHelper: WallpaperHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    // ── Dialog / overlay state ──────────────────────────────────────

    private val _showEditOverlay = MutableStateFlow(false)
    val showEditOverlay: StateFlow<Boolean> = _showEditOverlay.asStateFlow()

    private val _showEditShortcutsOverlay = MutableStateFlow(false)
    val showEditShortcutsOverlay: StateFlow<Boolean> = _showEditShortcutsOverlay.asStateFlow()

    private val _appMenuTarget = MutableStateFlow<FavoriteApp?>(null)
    val appMenuTarget: StateFlow<FavoriteApp?> = _appMenuTarget.asStateFlow()

    private val _showHomeScreenMenu = MutableStateFlow(false)
    val showHomeScreenMenu: StateFlow<Boolean> = _showHomeScreenMenu.asStateFlow()

    private val _showWeatherAppPicker = MutableStateFlow(false)
    val showWeatherAppPicker: StateFlow<Boolean> = _showWeatherAppPicker.asStateFlow()

    // ── Edit overlay state ──────────────────────────────────────────

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

    init {
        viewModelScope.launch {
            preferencesManager.ensureRightSideShortcutsInitialized()
        }
        startClockTicker()
        updateBattery()
        startWeatherTicker()
        observeWallpaperSetting()
        checkDefaultLauncher()
        loadAppNames()
        loadShortcutActions()
        observeRenames()
    }

    // ── Name resolution ─────────────────────────────────────────────

    private fun observeRenames() {
        viewModelScope.launch {
            appRepository.getAllRenamedApps().collect { renamedApps ->
                _renameMap.value = renamedApps.associate { it.packageName to it.customName }
            }
        }
    }

    /**
     * Pre-warms the app cache and builds the package-name → label map
     * used to resolve real app names for home-screen favorites.
     */
    private fun loadAppNames() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.getInstalledApps()
            _appNameMap.value = apps.associate { it.packageName to it.label }
            _allInstalledApps.value = apps
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

    // ── Edit overlay ────────────────────────────────────────────────

    fun openEditOverlay() {
        _appMenuTarget.value = null
        _editFavorites.value = favorites.value
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.getInstalledApps()
            _allInstalledApps.value = apps
            _appNameMap.value = apps.associate { it.packageName to it.label }
        }
        _showEditOverlay.value = true
    }

    fun closeEditOverlay() {
        _showEditOverlay.value = false
    }

    fun openEditShortcutsOverlay() {
        _appMenuTarget.value = null
        _editRightShortcuts.value = rightSideShortcuts.value
        viewModelScope.launch(Dispatchers.IO) {
            _allShortcutActions.value = appRepository.getAllShortcutActions()
            _showEditShortcutsOverlay.value = true
        }
    }

    fun closeEditShortcutsOverlay() {
        _showEditShortcutsOverlay.value = false
    }

    fun toggleAppOnHomeScreen(app: AppInfo) {
        val current = _editFavorites.value.toMutableList()
        val existing = current.indexOfFirst { it.packageName == app.packageName }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            val resolvedName = _renameMap.value[app.packageName] ?: app.label
            current.add(
                FavoriteApp(
                    label = resolvedName,
                    packageName = app.packageName,
                    iconName = "circle",
                    iconPackage = ""
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
            _showEditOverlay.value = false
        }
    }

    fun toggleRightShortcut(action: AppShortcutAction) {
        val current = _editRightShortcuts.value.toMutableList()
        val existing = current.indexOfFirst { it.target == action.target }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(
                HomeShortcut(
                    iconName = inferIconNameForAction(action),
                    target = action.target
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
            _showEditShortcutsOverlay.value = false
        }
    }

    // ── Remove ──────────────────────────────────────────────────────

    fun removeFavorite(fav: FavoriteApp) {
        viewModelScope.launch {
            val current = rawFavorites.value.toMutableList()
            current.removeAll { it.packageName == fav.packageName }
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
                "Selected weather app could not be launched",
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
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
        _appMenuTarget.value = null
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
        _appMenuTarget.value = null
    }

    // ── Clock / Battery / Weather ───────────────────────────────────

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                val now = Date()
                val timeFormat = SimpleDateFormat("H:mm", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEE. d MMM.", Locale.getDefault())
                _uiState.value = _uiState.value.copy(
                    currentTime = timeFormat.format(now),
                    currentDate = dateFormat.format(now)
                )
                delay(1_000)
            }
        }
    }

    private fun updateBattery() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        _uiState.value = _uiState.value.copy(batteryPercent = percent)
    }

    private fun startWeatherTicker() {
        viewModelScope.launch {
            while (true) {
                fetchWeatherOnce()
                delay(30 * 60 * 1000L)
            }
        }
    }

    private fun observeWallpaperSetting() {
        viewModelScope.launch {
            preferencesManager.showWallpaperFlow.collect { show ->
                _uiState.value = _uiState.value.copy(showWallpaper = show)
            }
        }
    }

    private fun checkDefaultLauncher() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(
            homeIntent, PackageManager.MATCH_DEFAULT_ONLY
        )
        val isDefault = resolveInfo?.activityInfo?.packageName == context.packageName
        _uiState.value = _uiState.value.copy(isDefaultLauncher = isDefault)
    }

    fun recheckDefaultLauncher() = checkDefaultLauncher()

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
        viewModelScope.launch { fetchWeatherOnce() }
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
    }

    fun launchShortcut(target: ShortcutTarget) {
        when (target) {
            is ShortcutTarget.App -> launchApp(target.packageName)
            is ShortcutTarget.LauncherShortcut ->
                appRepository.launchLauncherShortcut(target.packageName, target.shortcutId)
            is ShortcutTarget.DeepLink -> {
                val intent = try {
                    Intent.parseUri(target.intentUri, Intent.URI_INTENT_SCHEME)
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(target.intentUri))
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

    fun formatShortcutTarget(target: ShortcutTarget): String {
        return when (target) {
            is ShortcutTarget.App -> _appNameMap.value[target.packageName] ?: target.packageName
            is ShortcutTarget.DeepLink -> "Deep link"
            is ShortcutTarget.LauncherShortcut -> {
                val appName = _appNameMap.value[target.packageName] ?: target.packageName
                val shortcut = _allShortcutActions.value.firstOrNull { it.target == target }
                val actionLabel = shortcut?.actionLabel ?: "Shortcut"
                "$appName - $actionLabel"
            }
        }
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
     * Launches an app with a directional slide-in animation matching the swipe gesture.
     * @param fromLeft true when the user swiped left-to-right (app slides in from the left),
     *                 false when the user swiped right-to-left (app slides in from the right).
     */
    fun launchAppWithDirection(packageName: String, fromLeft: Boolean) {
        val options = if (fromLeft) {
            ActivityOptions.makeCustomAnimation(
                context,
                com.lu4p.fokuslauncher.R.anim.slide_in_left,
                com.lu4p.fokuslauncher.R.anim.slide_out_right
            )
        } else {
            ActivityOptions.makeCustomAnimation(
                context,
                com.lu4p.fokuslauncher.R.anim.slide_in_right,
                com.lu4p.fokuslauncher.R.anim.slide_out_left
            )
        }
        appRepository.launchApp(packageName, options.toBundle())
    }

    /**
     * Opens the default clock / alarm app.
     */
    fun openClockApp() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback: try DeskClock
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage("com.google.android.deskclock")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    /**
     * Opens the default calendar app.
     */
    fun openCalendarApp() {
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
            val optedOut = preferencesManager.weatherLocationOptedOutFlow.first()
            val hasCoarsePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val shouldShow = hasCoarsePermission && !optedOut
            _uiState.value = _uiState.value.copy(showWeatherWidget = shouldShow)
            if (!shouldShow) {
                _uiState.value = _uiState.value.copy(weather = null)
                return
            }
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @Suppress("MissingPermission")
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                val weather = weatherRepository.getWeather(location.latitude, location.longitude)
                _uiState.value = _uiState.value.copy(weather = weather)
            }
        } catch (_: Exception) { }
    }

    /**
     * Opens a weather app from the weather widget.
     */
    fun openWeatherApp() {
        val preferredPackage = preferredWeatherAppPackage.value
        if (preferredPackage.isNotBlank() && appRepository.launchApp(preferredPackage)) {
            return
        }
        if (preferredPackage.isNotBlank()) {
            Toast.makeText(
                context,
                "Selected weather app could not be launched",
                Toast.LENGTH_SHORT
            ).show()
        }

        try {
            val weatherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_WEATHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val canOpenWeatherCategory = weatherIntent.resolveActivity(context.packageManager) != null
            if (canOpenWeatherCategory) {
                context.startActivity(weatherIntent)
            } else {
                Toast.makeText(
                    context,
                    "No weather app available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "No weather app available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
