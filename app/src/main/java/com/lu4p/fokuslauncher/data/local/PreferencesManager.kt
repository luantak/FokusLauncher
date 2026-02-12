package com.lu4p.fokuslauncher.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "fokus_launcher_prefs")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private val FAVORITES_KEY = stringPreferencesKey("favorite_apps")
        private val SWIPE_LEFT_KEY = stringPreferencesKey("swipe_left_app")
        private val SWIPE_RIGHT_KEY = stringPreferencesKey("swipe_right_app")
        private val RIGHT_SIDE_SHORTCUTS_KEY = stringPreferencesKey("right_side_shortcuts")
        private val PREFERRED_WEATHER_APP_KEY = stringPreferencesKey("preferred_weather_app")
        private val SHOW_WALLPAPER_KEY = booleanPreferencesKey("show_wallpaper")
        private val HAS_COMPLETED_ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        private val ONBOARDING_REACHED_SET_DEFAULT_KEY = booleanPreferencesKey("onboarding_reached_set_default")
        private val WEATHER_LOCATION_OPTED_OUT_KEY = booleanPreferencesKey("weather_location_opted_out")

        /**
         * Format: "label;packageName;iconName" entries separated by "|" Falls back to legacy
         * "label:packageName" format when reading.
         */
        private const val DEFAULT_FAVORITES =
                "Music;com.google.android.apps.youtube.music;music|" +
                        "Work;com.google.android.gm;work|" +
                        "Read;com.google.android.apps.docs;read|" +
                        "Social;com.google.android.apps.messaging;chat|" +
                        "Health;com.google.android.dialer;call|" +
                        "Finance;com.android.vending;finance"
    }

    // --- Favorites ---

    val favoritesFlow: Flow<List<FavoriteApp>> =
            context.dataStore.data.map { prefs ->
                val raw = prefs[FAVORITES_KEY] ?: DEFAULT_FAVORITES
                parseFavorites(raw)
            }

    suspend fun setFavorites(favorites: List<FavoriteApp>) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITES_KEY] =
                    favorites.joinToString("|") {
                        "${it.label};${it.packageName};${it.iconName};${it.iconPackage}"
                    }
        }
    }

    // --- Right-side shortcuts ---

    val rightSideShortcutsFlow: Flow<List<HomeShortcut>> =
            context.dataStore.data.map { prefs ->
                parseRightSideShortcuts(prefs[RIGHT_SIDE_SHORTCUTS_KEY] ?: "")
            }

    suspend fun ensureRightSideShortcutsInitialized() {
        context.dataStore.edit { prefs ->
            if (!prefs.contains(RIGHT_SIDE_SHORTCUTS_KEY)) {
                val defaultShortcuts =
                        listOf(
                                HomeShortcut(
                                        iconName = "call",
                                        target = ShortcutTarget.App("com.google.android.dialer")
                                )
                        )
                prefs[RIGHT_SIDE_SHORTCUTS_KEY] = serializeRightSideShortcuts(defaultShortcuts)
            }
        }
    }

    suspend fun setRightSideShortcuts(shortcuts: List<HomeShortcut>) {
        context.dataStore.edit { prefs ->
            prefs[RIGHT_SIDE_SHORTCUTS_KEY] = serializeRightSideShortcuts(shortcuts)
        }
    }

    // --- Swipe gestures ---

    val swipeLeftTargetFlow: Flow<ShortcutTarget?> =
            context.dataStore.data.map { prefs ->
                ShortcutTarget.decode(prefs[SWIPE_LEFT_KEY] ?: "")
            }

    val swipeRightTargetFlow: Flow<ShortcutTarget?> =
            context.dataStore.data.map { prefs ->
                ShortcutTarget.decode(prefs[SWIPE_RIGHT_KEY] ?: "")
            }

    // Backward-compatible app-only flows used by existing settings UI.
    val swipeLeftAppFlow: Flow<String> =
            context.dataStore.data.map { prefs ->
                (ShortcutTarget.decode(prefs[SWIPE_LEFT_KEY] ?: "") as? ShortcutTarget.App)
                        ?.packageName
                        ?: ""
            }

    val swipeRightAppFlow: Flow<String> =
            context.dataStore.data.map { prefs ->
                (ShortcutTarget.decode(prefs[SWIPE_RIGHT_KEY] ?: "") as? ShortcutTarget.App)
                        ?.packageName
                        ?: ""
            }

    suspend fun setSwipeLeftApp(packageName: String) {
        setSwipeLeftTarget(if (packageName.isBlank()) null else ShortcutTarget.App(packageName))
    }

    suspend fun setSwipeRightApp(packageName: String) {
        setSwipeRightTarget(if (packageName.isBlank()) null else ShortcutTarget.App(packageName))
    }

    suspend fun setSwipeLeftTarget(target: ShortcutTarget?) {
        context.dataStore.edit { prefs -> prefs[SWIPE_LEFT_KEY] = ShortcutTarget.encode(target) }
    }

    suspend fun setSwipeRightTarget(target: ShortcutTarget?) {
        context.dataStore.edit { prefs -> prefs[SWIPE_RIGHT_KEY] = ShortcutTarget.encode(target) }
    }

    // --- Preferred weather app ---

    val preferredWeatherAppFlow: Flow<String> =
            context.dataStore.data.map { prefs -> prefs[PREFERRED_WEATHER_APP_KEY] ?: "" }

    suspend fun setPreferredWeatherApp(packageName: String) {
        context.dataStore.edit { prefs -> prefs[PREFERRED_WEATHER_APP_KEY] = packageName }
    }

    // --- Wallpaper Background ---

    val showWallpaperFlow: Flow<Boolean> =
            context.dataStore.data.map { prefs -> prefs[SHOW_WALLPAPER_KEY] ?: false }

    suspend fun setShowWallpaper(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_WALLPAPER_KEY] = show }
    }

    // --- Onboarding ---

    val hasCompletedOnboardingFlow: Flow<Boolean> =
            context.dataStore.data.map { prefs ->
                prefs[HAS_COMPLETED_ONBOARDING_KEY] ?: false
            }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_COMPLETED_ONBOARDING_KEY] = completed
            if (completed) prefs.remove(ONBOARDING_REACHED_SET_DEFAULT_KEY)
        }
    }

    suspend fun setOnboardingReachedSetDefault(reached: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_REACHED_SET_DEFAULT_KEY] = reached
        }
    }

    suspend fun getOnboardingReachedSetDefault(): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[ONBOARDING_REACHED_SET_DEFAULT_KEY] ?: false
        }.first()
    }

    // --- Weather location opt-out ---

    val weatherLocationOptedOutFlow: Flow<Boolean> =
            context.dataStore.data.map { prefs ->
                prefs[WEATHER_LOCATION_OPTED_OUT_KEY] ?: false
            }

    suspend fun setWeatherLocationOptedOut(optedOut: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WEATHER_LOCATION_OPTED_OUT_KEY] = optedOut
        }
    }

    /** Clears all preferences, equivalent to clearing app storage. */
    suspend fun clearAll() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }

    // --- Parsing ---

    private fun parseFavorites(raw: String): List<FavoriteApp> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            // New format: "label;packageName;iconName;iconPackage"
            val semiParts = entry.split(";")
            if (semiParts.size >= 3) {
                FavoriteApp(
                        label = semiParts[0],
                        packageName = semiParts[1],
                        iconName = semiParts[2],
                        iconPackage = semiParts.getOrElse(3) { "" }
                )
            } else {
                // Legacy format: "label:packageName"
                val colonParts = entry.split(":", limit = 2)
                if (colonParts.size == 2) {
                    FavoriteApp(
                            label = colonParts[0],
                            packageName = colonParts[1],
                            iconName = "circle"
                    )
                } else null
            }
        }
    }

    private fun parseRightSideShortcuts(raw: String): List<HomeShortcut> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(";", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val iconName = parts[0].ifBlank { "circle" }
            val target = ShortcutTarget.decode(parts[1]) ?: return@mapNotNull null
            HomeShortcut(iconName = iconName, target = target)
        }
    }

    private fun serializeRightSideShortcuts(shortcuts: List<HomeShortcut>): String {
        return shortcuts.joinToString("|") { shortcut ->
            "${shortcut.iconName};${ShortcutTarget.encode(shortcut.target)}"
        }
    }
}
