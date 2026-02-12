package com.lu4p.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
        val hiddenApps: List<HiddenAppInfo> = emptyList(),
        val renamedApps: List<RenamedAppEntity> = emptyList(),
        val favorites: List<FavoriteApp> = emptyList(),
        val rightSideShortcuts: List<HomeShortcut> = emptyList(),
        val swipeLeftTarget: ShortcutTarget? = null,
        val swipeRightTarget: ShortcutTarget? = null,
        val preferredWeatherAppPackage: String = "",
        val allApps: List<AppInfo> = emptyList()
)

data class HiddenAppInfo(val packageName: String, val label: String)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
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
                    .combine(preferencesManager.swipeRightTargetFlow) { leftState, swipeRight ->
                        leftState to swipeRight
                    }
                    .combine(preferencesManager.preferredWeatherAppFlow) {
                            swipeState,
                            preferredWeatherApp ->
                        val (leftState, swipeRight) = swipeState
                        val allApps = appRepository.getInstalledApps()
                        val hiddenInfos =
                                leftState.hiddenNames.map { pkg ->
                                    val app = allApps.find { it.packageName == pkg }
                                    HiddenAppInfo(packageName = pkg, label = app?.label ?: pkg)
                                }
                        SettingsUiState(
                                hiddenApps = hiddenInfos,
                                renamedApps = leftState.renamedApps,
                                favorites = leftState.favorites,
                                rightSideShortcuts = leftState.rightSideShortcuts,
                                swipeLeftTarget = leftState.swipeLeft,
                                swipeRightTarget = swipeRight,
                                preferredWeatherAppPackage = preferredWeatherApp,
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

    // --- Hidden Apps ---

    fun unhideApp(packageName: String) {
        viewModelScope.launch { appRepository.unhideApp(packageName) }
    }

    // --- Renamed Apps ---

    fun removeRename(packageName: String) {
        viewModelScope.launch { appRepository.removeRename(packageName) }
    }

    // --- Favorites ---

    fun addFavorite(label: String, packageName: String, iconName: String) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            current.add(FavoriteApp(label = label, packageName = packageName, iconName = iconName))
            preferencesManager.setFavorites(current)
        }
    }

    fun removeFavorite(index: Int) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                preferencesManager.setFavorites(current)
            }
        }
    }

    fun moveFavoriteUp(index: Int) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index > 0 && index < current.size) {
                val item = current.removeAt(index)
                current.add(index - 1, item)
                preferencesManager.setFavorites(current)
            }
        }
    }

    fun moveFavoriteDown(index: Int) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index >= 0 && index < current.size - 1) {
                val item = current.removeAt(index)
                current.add(index + 1, item)
                preferencesManager.setFavorites(current)
            }
        }
    }

    fun updateFavoriteLabel(index: Int, newLabel: String) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(label = newLabel)
                preferencesManager.setFavorites(current)
            }
        }
    }

    fun updateFavoriteApp(index: Int, packageName: String) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(packageName = packageName)
                preferencesManager.setFavorites(current)
            }
        }
    }

    fun updateFavoriteIcon(index: Int, iconName: String) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(iconName = iconName)
                preferencesManager.setFavorites(current)
            }
        }
    }

    fun updateFavoriteIconApp(index: Int, packageName: String) {
        updateFavoriteIconTarget(index, ShortcutTarget.App(packageName))
    }

    fun updateFavoriteIconTarget(index: Int, target: ShortcutTarget?) {
        viewModelScope.launch {
            val current = preferencesManager.favoritesFlow.first().toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(iconPackage = ShortcutTarget.encode(target))
                preferencesManager.setFavorites(current)
            }
        }
    }

    // --- Swipe gestures ---

    fun setSwipeLeftApp(packageName: String) {
        setSwipeLeftTarget(if (packageName.isBlank()) null else ShortcutTarget.App(packageName))
    }

    fun setSwipeRightApp(packageName: String) {
        setSwipeRightTarget(if (packageName.isBlank()) null else ShortcutTarget.App(packageName))
    }

    fun setSwipeLeftTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeLeftTarget(target) }
    }

    fun setSwipeRightTarget(target: ShortcutTarget?) {
        viewModelScope.launch { preferencesManager.setSwipeRightTarget(target) }
    }

    fun setPreferredWeatherApp(packageName: String) {
        viewModelScope.launch { preferencesManager.setPreferredWeatherApp(packageName) }
    }

    /** Clears all app state (preferences + database), equivalent to clearing storage. */
    fun resetAllState() {
        viewModelScope.launch {
            preferencesManager.clearAll()
            appRepository.clearAllAppData()
            appRepository.invalidateCache()
        }
    }
}
