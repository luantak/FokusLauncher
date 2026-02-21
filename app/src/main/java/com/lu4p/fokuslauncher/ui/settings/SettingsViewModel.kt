package com.lu4p.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
        val hiddenApps: List<HiddenAppInfo> = emptyList(),
        val renamedApps: List<RenamedAppEntity> = emptyList(),
        val appCategories: Map<String, String> = emptyMap(),
        val categoryDefinitions: List<String> = emptyList(),
        val favorites: List<FavoriteApp> = emptyList(),
        val rightSideShortcuts: List<HomeShortcut> = emptyList(),
        val swipeLeftTarget: ShortcutTarget? = null,
        val swipeRightTarget: ShortcutTarget? = null,
        val preferredWeatherAppPackage: String = "",
        val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
        val showWallpaper: Boolean = false,
        val allApps: List<AppInfo> = emptyList()
)

data class HiddenAppInfo(val packageName: String, val label: String)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadAllApps()
        observeState()
    }

    private fun loadAllApps() {
        _uiState.value = _uiState.value.copy(allApps = appRepository.getInstalledApps())
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                            appRepository.getHiddenPackageNames(),
                            appRepository.getAllRenamedApps(),
                            preferencesManager.favoritesFlow,
                            preferencesManager.rightSideShortcutsFlow,
                            preferencesManager.swipeLeftTargetFlow
                    ) { hiddenNames, renamedApps, favorites, rightSideShortcuts, swipeLeft ->
                Quintuple(
                        hiddenNames = hiddenNames,
                        renamedApps = renamedApps,
                        favorites = favorites,
                        rightSideShortcuts = rightSideShortcuts,
                        swipeLeft = swipeLeft
                )
            }
                    .combine(appRepository.getAllAppCategories()) { leftState, appCategories ->
                        leftState to appCategories.associate { it.packageName to it.category }
                    }
                    .combine(appRepository.getAllCategoryDefinitions()) { stateWithCategories, definitions ->
                        val (leftState, appCategories) = stateWithCategories
                        CategoryState(
                                base = leftState,
                                appCategories = appCategories,
                                categoryDefinitions = definitions.map { it.name }
                        )
                    }
                    .combine(preferencesManager.swipeRightTargetFlow) { leftState, swipeRight ->
                        leftState to swipeRight
                    }
                    .combine(preferencesManager.preferredWeatherAppFlow) { swipeState, preferredWeatherApp ->
                        Pair(swipeState, preferredWeatherApp)
                    }
                    .combine(preferencesManager.showWallpaperFlow) { previousState, showWallpaper ->
                        val (swipeState, preferredWeatherApp) = previousState
                        val (leftState, swipeRight) = swipeState
                        Pair(Triple(leftState, swipeRight, preferredWeatherApp), showWallpaper)
                    }
                    .combine(preferencesManager.homeAlignmentFlow) {
                            weatherStateAndWallpaper,
                            homeAlignment ->
                        val (weatherState, showWallpaper) = weatherStateAndWallpaper
                        val (leftState, swipeRight, preferredWeatherApp) = weatherState
                        val allApps = appRepository.getInstalledApps()
                        val hiddenInfos =
                                leftState.base.hiddenNames.map { pkg ->
                                    val app = allApps.find { it.packageName == pkg }
                                    HiddenAppInfo(packageName = pkg, label = app?.label ?: pkg)
                                }
                        SettingsUiState(
                                hiddenApps = hiddenInfos,
                                renamedApps = leftState.base.renamedApps,
                                appCategories = leftState.appCategories,
                                categoryDefinitions = leftState.categoryDefinitions,
                                favorites = leftState.base.favorites,
                                rightSideShortcuts = leftState.base.rightSideShortcuts,
                                swipeLeftTarget = leftState.base.swipeLeft,
                                swipeRightTarget = swipeRight,
                                preferredWeatherAppPackage = preferredWeatherApp,
                                homeAlignment = homeAlignment,
                                showWallpaper = showWallpaper,
                                allApps = allApps
                        )
                    }
                    .collect { _uiState.value = it }
        }
    }

    private data class Quintuple(
            val hiddenNames: List<String>,
            val renamedApps: List<RenamedAppEntity>,
            val favorites: List<FavoriteApp>,
            val rightSideShortcuts: List<HomeShortcut>,
            val swipeLeft: ShortcutTarget?
    )

    private data class CategoryState(
            val base: Quintuple,
            val appCategories: Map<String, String>,
            val categoryDefinitions: List<String>
    )

    // --- Hidden Apps ---

    fun unhideApp(packageName: String) {
        viewModelScope.launch { appRepository.unhideApp(packageName) }
    }

    // --- Renamed Apps ---

    fun removeRename(packageName: String) {
        viewModelScope.launch { appRepository.removeRename(packageName) }
    }

    fun addCategoryDefinition(name: String) {
        viewModelScope.launch { appRepository.addCategoryDefinition(name) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch { appRepository.deleteCategory(name) }
    }

    fun setAppCategory(packageName: String, category: String) {
        viewModelScope.launch { appRepository.setAppCategory(packageName, category) }
    }

    fun reorderCategories(categories: List<String>) {
        viewModelScope.launch { appRepository.reorderCategoryDefinitions(categories) }
    }

    // --- Swipe gestures ---

    fun setSwipeLeftTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeLeftTarget(target) }
    }

    fun setSwipeRightTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeRightTarget(target) }
    }

    fun setPreferredWeatherApp(packageName: String) {
        viewModelScope.launch { preferencesManager.setPreferredWeatherApp(packageName) }
    }

    // --- Home alignment ---

    fun setHomeAlignment(alignment: HomeAlignment) {
        viewModelScope.launch { preferencesManager.setHomeAlignment(alignment) }
    }

    /** Clears all app state (preferences + database), equivalent to clearing storage. */
    suspend fun resetAllState() {
        preferencesManager.clearAll()
        appRepository.clearAllAppData()
        appRepository.invalidateCache()
    }

    fun setShowWallpaper(show: Boolean) {
        viewModelScope.launch { preferencesManager.setShowWallpaper(show) }
    }

    fun setSystemWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    wallpaperManager.setStream(stream)
                }
                // Optionally auto-enable the wallpaper toggle so the user sees their change
                preferencesManager.setShowWallpaper(true)
            } catch (e: Exception) {
                // Ignore or handle error
            }
        }
    }

    fun setBlackWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create a full-screen black bitmap
                val width = context.resources.displayMetrics.widthPixels
                val height = context.resources.displayMetrics.heightPixels
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.BLACK)

                val wallpaperManager = WallpaperManager.getInstance(context)
                // Set for home screen
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                // Try to set for lock screen too
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } catch (_: Exception) {
                    // Lock screen wallpaper may fail on some devices, ignore
                }

                // Also update the UI state
                preferencesManager.setShowWallpaper(false)
            } catch (e: Exception) {
                e.printStackTrace()
                // If setting wallpaper fails, at least set the launcher preference
                preferencesManager.setShowWallpaper(false)
            }
        }
    }
}
