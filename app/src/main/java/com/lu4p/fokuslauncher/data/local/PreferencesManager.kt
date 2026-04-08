package com.lu4p.fokuslauncher.data.local

import android.content.Context
import android.os.UserHandle
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.drawerOpenCountKey
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import com.lu4p.fokuslauncher.data.model.SystemCategoryKeys
import com.lu4p.fokuslauncher.data.model.HomeDateFormatStyle
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.DotSearchTargetPreference
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** BCP-47 tag (e.g. en, pl). Empty = follow system. Shared with [AppLocaleHelper]. */
internal val APP_LOCALE_TAG_KEY = stringPreferencesKey("app_locale_tag")

data class HomeWidgetVisibility(
        val showClock: Boolean,
        val showDate: Boolean,
        val showWeather: Boolean,
        val showBattery: Boolean,
)

@Singleton
class PreferencesManager @Inject constructor(@param:ApplicationContext private val context: Context) {

    private fun <T> prefFlow(key: Preferences.Key<T>, default: T): Flow<T> =
            context.fokusLauncherPreferencesDataStore.data.map { it[key] ?: default }

    private suspend fun <T> setPref(key: Preferences.Key<T>, value: T) {
        context.fokusLauncherPreferencesDataStore.edit { it[key] = value }
    }

    companion object {
        private val FAVORITES_KEY = stringPreferencesKey("favorite_apps")
        private val SWIPE_LEFT_KEY = stringPreferencesKey("swipe_left_app")
        private val SWIPE_RIGHT_KEY = stringPreferencesKey("swipe_right_app")
        private val RIGHT_SIDE_SHORTCUTS_KEY = stringPreferencesKey("right_side_shortcuts")
        /**
         * Stored when the user has zero right-side shortcuts. Non-empty so the preference key stays
         * written: some DataStore backends omit empty strings, which made [RIGHT_SIDE_SHORTCUTS_KEY]
         * disappear and [ensureRightSideShortcutsInitialized] re-seed the default phone shortcut.
         */
        private const val RIGHT_SIDE_SHORTCUTS_EMPTY_MARKER = "__empty__"
        private val PREFERRED_WEATHER_APP_KEY = stringPreferencesKey("preferred_weather_app")
        private val PREFERRED_CLOCK_APP_KEY = stringPreferencesKey("preferred_clock_app")
        private val PREFERRED_CALENDAR_APP_KEY = stringPreferencesKey("preferred_calendar_app")
        private val SHOW_STATUS_BAR_KEY = booleanPreferencesKey("show_status_bar")
        private val SHOW_HOME_CLOCK_KEY = booleanPreferencesKey("show_home_clock")
        private val SHOW_HOME_DATE_KEY = booleanPreferencesKey("show_home_date")
        private val HOME_DATE_FORMAT_STYLE_KEY = stringPreferencesKey("home_date_format_style")
        private val SHOW_HOME_WEATHER_KEY = booleanPreferencesKey("show_home_weather")
        private val SHOW_HOME_BATTERY_KEY = booleanPreferencesKey("show_home_battery")
        /** Vertical category sidebar in the drawer instead of chips + search bar. */
        private val DRAWER_SIDEBAR_CATEGORIES_KEY =
                booleanPreferencesKey("drawer_sidebar_categories")
        /**
         * When true, the vertical category rail is on the left. Default false = rail on the right
         * (toward the edge users often reach with the thumb).
         */
        private val DRAWER_CATEGORY_SIDEBAR_ON_LEFT_KEY =
                booleanPreferencesKey("drawer_category_sidebar_on_left")
        /** JSON object: normalized category key → MinimalIcons name. */
        private val DRAWER_CATEGORY_ICONS_KEY = stringPreferencesKey("drawer_category_icons")
        private val DRAWER_APP_SORT_MODE_KEY = stringPreferencesKey("drawer_app_sort_mode")
        /** JSON object: profile key string → JSON array of `drawerOpenCountKey` entries. */
        private val DRAWER_CUSTOM_APP_ORDER_KEY = stringPreferencesKey("drawer_custom_app_order")
        private val DRAWER_APP_OPEN_COUNTS_KEY = stringPreferencesKey("drawer_app_open_counts")
        /** JSON: {"profileKey":"0","target":""} — empty/missing target = system default search. */
        private val DRAWER_DOT_SEARCH_DEFAULT_KEY = stringPreferencesKey("drawer_dot_search_default")
        /** JSON object: single-character key → {"profileKey":"0","target":"app:…"}. */
        private val DRAWER_DOT_SEARCH_ALIASES_KEY = stringPreferencesKey("drawer_dot_search_aliases")
        private val HAS_COMPLETED_ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        private val ONBOARDING_REACHED_SET_DEFAULT_KEY = booleanPreferencesKey("onboarding_reached_set_default")
        private val HOME_ALIGNMENT_KEY = stringPreferencesKey("home_alignment")
        private val LAUNCHER_FONT_FAMILY_KEY = stringPreferencesKey("launcher_font_family")
        private val ALLOW_LANDSCAPE_ROTATION_KEY =
                booleanPreferencesKey("allow_landscape_rotation")
        private val DOUBLE_TAP_EMPTY_LOCK_KEY =
                booleanPreferencesKey("double_tap_empty_lock")
        private val LONG_LOCK_RETURN_HOME_KEY =
                booleanPreferencesKey("long_lock_return_home")
        private val LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES_KEY =
                intPreferencesKey("long_lock_return_home_threshold_minutes")
        private val LONG_LOCK_LAST_SCREEN_OFF_AT_MS_KEY =
                longPreferencesKey("long_lock_last_screen_off_at_ms")

        const val DEFAULT_LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES = 15

        /**
         * Format: "label;packageName;iconName" entries separated by "|" Falls back to legacy
         * "label:packageName" format when reading.
         */
        private const val DEFAULT_FAVORITES =
                "Music;com.google.android.apps.youtube.music;music|" +
                        "Work;com.google.android.gm;work|" +
                        "Read;com.google.android.apps.docs;read|" +
                        "Social;com.google.android.apps.messaging;chat|" +
                        "Health;${ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE};call;internal:phone|" +
                        "Finance;com.android.vending;finance"
    }

    // --- Favorites ---

    val favoritesFlow: Flow<List<FavoriteApp>> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                val raw = prefs[FAVORITES_KEY] ?: DEFAULT_FAVORITES
                parseFavorites(raw)
            }

    suspend fun setFavorites(favorites: List<FavoriteApp>) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs[FAVORITES_KEY] =
                    favorites.joinToString("|") {
                        "${it.label};${it.packageName};${it.iconName};${it.iconPackage};${it.profileKey}"
                    }
        }
    }

    // --- Right-side shortcuts ---

    val rightSideShortcutsFlow: Flow<List<HomeShortcut>> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                parseRightSideShortcuts(prefs[RIGHT_SIDE_SHORTCUTS_KEY] ?: "")
            }

    suspend fun ensureRightSideShortcutsInitialized() {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            if (!prefs.contains(RIGHT_SIDE_SHORTCUTS_KEY)) {
                val defaultShortcuts =
                        listOf(
                                HomeShortcut(
                                        iconName = "call",
                                        target = ShortcutTarget.PhoneDial,
                                )
                        )
                prefs[RIGHT_SIDE_SHORTCUTS_KEY] = serializeRightSideShortcuts(defaultShortcuts)
            }
        }
    }

    /**
     * Replaces hard-coded dialer package targets with [ShortcutTarget.PhoneDial] so the default
     * dialer resolves via [android.content.Intent.ACTION_DIAL].
     */
    suspend fun migrateLegacyDialerShortcutTargets() {
        val legacyDialerPackages =
                setOf(
                        "com.google.android.dialer",
                        "com.android.dialer",
                        "com.samsung.android.dialer",
                        "com.oneplus.dialer",
                )
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs[RIGHT_SIDE_SHORTCUTS_KEY]?.let { raw ->
                val list = parseRightSideShortcuts(raw)
                val migrated =
                        list.map { s ->
                            if (s.target is ShortcutTarget.App &&
                                            s.target.packageName in legacyDialerPackages
                            ) {
                                s.copy(target = ShortcutTarget.PhoneDial)
                            } else s
                        }
                if (migrated != list) {
                    prefs[RIGHT_SIDE_SHORTCUTS_KEY] = serializeRightSideShortcuts(migrated)
                }
            }

            prefs[FAVORITES_KEY]?.let { raw ->
                val favorites = parseFavorites(raw)
                val migrated =
                        favorites.map { fav ->
                            if (fav.packageName in legacyDialerPackages && fav.iconPackage.isBlank()) {
                                fav.copy(
                                        packageName = ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE,
                                        iconPackage = ShortcutTarget.encode(ShortcutTarget.PhoneDial),
                                )
                            } else fav
                        }
                if (migrated != favorites) {
                    prefs[FAVORITES_KEY] =
                            migrated.joinToString("|") {
                                "${it.label};${it.packageName};${it.iconName};${it.iconPackage};${it.profileKey}"
                            }
                }
            }
        }
    }

    suspend fun setRightSideShortcuts(shortcuts: List<HomeShortcut>) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs[RIGHT_SIDE_SHORTCUTS_KEY] = serializeRightSideShortcuts(shortcuts)
        }
    }

    // --- Swipe gestures ---

    val swipeLeftTargetFlow: Flow<ShortcutTarget?> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                ShortcutTarget.decode(prefs[SWIPE_LEFT_KEY] ?: "")
            }

    val swipeRightTargetFlow: Flow<ShortcutTarget?> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                ShortcutTarget.decode(prefs[SWIPE_RIGHT_KEY] ?: "")
            }

    suspend fun setSwipeLeftTarget(target: ShortcutTarget?) {
        context.fokusLauncherPreferencesDataStore.edit { prefs -> prefs[SWIPE_LEFT_KEY] = ShortcutTarget.encode(target) }
    }

    suspend fun setSwipeRightTarget(target: ShortcutTarget?) {
        context.fokusLauncherPreferencesDataStore.edit { prefs -> prefs[SWIPE_RIGHT_KEY] = ShortcutTarget.encode(target) }
    }

    // --- Preferred weather app ---

    val preferredWeatherAppFlow: Flow<String> = prefFlow(PREFERRED_WEATHER_APP_KEY, "")
    suspend fun setPreferredWeatherApp(packageName: String) =
            setPref(PREFERRED_WEATHER_APP_KEY, packageName)

    // --- Clock / calendar tap overrides (home widgets) ---

    val preferredClockAppFlow: Flow<String> = prefFlow(PREFERRED_CLOCK_APP_KEY, "")
    suspend fun setPreferredClockApp(packageName: String) =
            setPref(PREFERRED_CLOCK_APP_KEY, packageName)

    val preferredCalendarAppFlow: Flow<String> = prefFlow(PREFERRED_CALENDAR_APP_KEY, "")
    suspend fun setPreferredCalendarApp(packageName: String) =
            setPref(PREFERRED_CALENDAR_APP_KEY, packageName)

    // --- System UI ---

    val showStatusBarFlow: Flow<Boolean> = prefFlow(SHOW_STATUS_BAR_KEY, false)
    suspend fun setShowStatusBar(show: Boolean) = setPref(SHOW_STATUS_BAR_KEY, show)

    val showHomeClockFlow: Flow<Boolean> = prefFlow(SHOW_HOME_CLOCK_KEY, true)
    suspend fun setShowHomeClock(show: Boolean) = setPref(SHOW_HOME_CLOCK_KEY, show)

    val showHomeDateFlow: Flow<Boolean> = prefFlow(SHOW_HOME_DATE_KEY, true)
    suspend fun setShowHomeDate(show: Boolean) = setPref(SHOW_HOME_DATE_KEY, show)

    val homeDateFormatStyleFlow: Flow<HomeDateFormatStyle> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                HomeDateFormatStyle.fromString(prefs[HOME_DATE_FORMAT_STYLE_KEY])
            }

    suspend fun setHomeDateFormatStyle(style: HomeDateFormatStyle) =
            setPref(HOME_DATE_FORMAT_STYLE_KEY, style.name)

    val showHomeWeatherFlow: Flow<Boolean> = prefFlow(SHOW_HOME_WEATHER_KEY, true)
    suspend fun setShowHomeWeather(show: Boolean) = setPref(SHOW_HOME_WEATHER_KEY, show)

    val showHomeBatteryFlow: Flow<Boolean> = prefFlow(SHOW_HOME_BATTERY_KEY, true)
    suspend fun setShowHomeBattery(show: Boolean) = setPref(SHOW_HOME_BATTERY_KEY, show)

    val homeWidgetVisibilityFlow: Flow<HomeWidgetVisibility> =
            combine(
                    showHomeClockFlow,
                    showHomeDateFlow,
                    showHomeWeatherFlow,
                    showHomeBatteryFlow,
            ) { showClock, showDate, showWeather, showBattery ->
                HomeWidgetVisibility(showClock, showDate, showWeather, showBattery)
            }

    val drawerSidebarCategoriesFlow: Flow<Boolean> =
            prefFlow(DRAWER_SIDEBAR_CATEGORIES_KEY, false)
    suspend fun setDrawerSidebarCategories(enabled: Boolean) =
            setPref(DRAWER_SIDEBAR_CATEGORIES_KEY, enabled)

    val drawerCategorySidebarOnLeftFlow: Flow<Boolean> =
            prefFlow(DRAWER_CATEGORY_SIDEBAR_ON_LEFT_KEY, false)
    suspend fun setDrawerCategorySidebarOnLeft(onLeft: Boolean) =
            setPref(DRAWER_CATEGORY_SIDEBAR_ON_LEFT_KEY, onLeft)

    val drawerCategoryIconsFlow: Flow<Map<String, String>> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                parseDrawerCategoryIcons(prefs[DRAWER_CATEGORY_ICONS_KEY] ?: "")
            }

    suspend fun setDrawerCategoryIcon(rawCategory: String, iconName: String) {
        val key = SystemCategoryKeys.normalize(context, rawCategory)
        if (key.isBlank()) return
        val icon = iconName.trim()
        if (icon.isEmpty()) return
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val current = parseDrawerCategoryIcons(prefs[DRAWER_CATEGORY_ICONS_KEY] ?: "").toMutableMap()
            current[key] = icon
            prefs[DRAWER_CATEGORY_ICONS_KEY] = serializeDrawerCategoryIcons(current)
        }
    }

    suspend fun clearDrawerCategoryIcon(rawCategory: String) {
        val key = SystemCategoryKeys.normalize(context, rawCategory)
        if (key.isBlank()) return
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val current = parseDrawerCategoryIcons(prefs[DRAWER_CATEGORY_ICONS_KEY] ?: "").toMutableMap()
            current.remove(key)
            if (current.isEmpty()) prefs.remove(DRAWER_CATEGORY_ICONS_KEY)
            else prefs[DRAWER_CATEGORY_ICONS_KEY] = serializeDrawerCategoryIcons(current)
        }
    }

    suspend fun renameDrawerCategoryIcon(oldName: String, newName: String) {
        val oldKey = SystemCategoryKeys.normalize(context, oldName)
        val newKey = SystemCategoryKeys.normalize(context, newName)
        if (oldKey.isBlank() || newKey.isBlank() || oldKey == newKey) return
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val current = parseDrawerCategoryIcons(prefs[DRAWER_CATEGORY_ICONS_KEY] ?: "").toMutableMap()
            val icon = current.remove(oldKey) ?: return@edit
            current[newKey] = icon
            if (current.isEmpty()) prefs.remove(DRAWER_CATEGORY_ICONS_KEY)
            else prefs[DRAWER_CATEGORY_ICONS_KEY] = serializeDrawerCategoryIcons(current)
        }
    }

    // --- App drawer sort & launch counts (drawer opens only) ---

    val drawerAppSortModeFlow: Flow<DrawerAppSortMode> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                DrawerAppSortMode.fromStorage(prefs[DRAWER_APP_SORT_MODE_KEY])
            }

    suspend fun setDrawerAppSortMode(mode: DrawerAppSortMode) =
            setPref(DRAWER_APP_SORT_MODE_KEY, mode.name)

    val drawerCustomAppOrderFlow: Flow<Map<String, List<String>>> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                parseDrawerCustomAppOrderJson(prefs[DRAWER_CUSTOM_APP_ORDER_KEY] ?: "")
            }

    suspend fun setDrawerCustomAppOrder(order: Map<String, List<String>>) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            if (order.isEmpty()) prefs.remove(DRAWER_CUSTOM_APP_ORDER_KEY)
            else prefs[DRAWER_CUSTOM_APP_ORDER_KEY] = serializeDrawerCustomAppOrderJson(order)
        }
    }

    // --- Drawer dot-search ---

    val drawerDotSearchDefaultFlow: Flow<DotSearchTargetPreference> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                parseDrawerDotSearchTargetJson(prefs[DRAWER_DOT_SEARCH_DEFAULT_KEY] ?: "")
                        ?: DotSearchTargetPreference()
            }

    val drawerDotSearchAliasesFlow: Flow<Map<Char, DotSearchTargetPreference>> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                parseDrawerDotSearchAliasesJson(prefs[DRAWER_DOT_SEARCH_ALIASES_KEY] ?: "")
            }

    suspend fun setDrawerDotSearchDefault(config: DotSearchTargetPreference) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val encoded = serializeDrawerDotSearchTarget(config)
            if (encoded.isEmpty()) prefs.remove(DRAWER_DOT_SEARCH_DEFAULT_KEY)
            else prefs[DRAWER_DOT_SEARCH_DEFAULT_KEY] = encoded
        }
    }

    suspend fun clearDrawerDotSearchDefault() {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs.remove(DRAWER_DOT_SEARCH_DEFAULT_KEY)
        }
    }

    suspend fun setDrawerDotSearchAlias(
            alias: Char,
            config: DotSearchTargetPreference,
    ) {
        val key = alias.lowercaseChar()
        require(key in 'a'..'z') { "Alias must be a lowercase letter" }
        require(config.target != null) { "Alias target is required" }
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val current =
                    parseDrawerDotSearchAliasesJson(
                                    prefs[DRAWER_DOT_SEARCH_ALIASES_KEY] ?: ""
                            )
                            .toMutableMap()
            current[key] = config
            prefs[DRAWER_DOT_SEARCH_ALIASES_KEY] = serializeDrawerDotSearchAliases(current)
        }
    }

    suspend fun removeDrawerDotSearchAlias(alias: Char) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val current =
                    parseDrawerDotSearchAliasesJson(
                                    prefs[DRAWER_DOT_SEARCH_ALIASES_KEY] ?: ""
                            )
                            .toMutableMap()
            current.remove(alias.lowercaseChar())
            if (current.isEmpty()) prefs.remove(DRAWER_DOT_SEARCH_ALIASES_KEY)
            else prefs[DRAWER_DOT_SEARCH_ALIASES_KEY] = serializeDrawerDotSearchAliases(current)
        }
    }

    val drawerAppOpenCountsFlow: Flow<Map<String, Int>> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                parseDrawerOpenCounts(prefs[DRAWER_APP_OPEN_COUNTS_KEY] ?: "")
            }

    suspend fun recordDrawerAppOpen(packageName: String, userHandle: UserHandle?) {
        val key = drawerOpenCountKey(packageName, userHandle)
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            val raw = prefs[DRAWER_APP_OPEN_COUNTS_KEY] ?: ""
            val map = parseDrawerOpenCounts(raw).toMutableMap()
            map[key] = (map[key] ?: 0) + 1
            prefs[DRAWER_APP_OPEN_COUNTS_KEY] = serializeDrawerOpenCounts(map)
        }
    }

    // --- Onboarding ---

    val hasCompletedOnboardingFlow: Flow<Boolean> =
            prefFlow(HAS_COMPLETED_ONBOARDING_KEY, false)

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs[HAS_COMPLETED_ONBOARDING_KEY] = completed
            if (completed) prefs.remove(ONBOARDING_REACHED_SET_DEFAULT_KEY)
        }
    }

    suspend fun setOnboardingReachedSetDefault(reached: Boolean) {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs[ONBOARDING_REACHED_SET_DEFAULT_KEY] = reached
        }
    }

    suspend fun getOnboardingReachedSetDefault(): Boolean {
        return context.fokusLauncherPreferencesDataStore.data.map { prefs ->
            prefs[ONBOARDING_REACHED_SET_DEFAULT_KEY] ?: false
        }.first()
    }

    // --- Home alignment ---

    val homeAlignmentFlow: Flow<HomeAlignment> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                HomeAlignment.fromString(prefs[HOME_ALIGNMENT_KEY] ?: HomeAlignment.LEFT.name)
            }

    suspend fun setHomeAlignment(alignment: HomeAlignment) =
            setPref(HOME_ALIGNMENT_KEY, alignment.name)

    // --- Launcher text (system fonts + scale) ---

    val launcherFontFamilyFlow: Flow<String> =
            context.fokusLauncherPreferencesDataStore.data.map { prefs ->
                LauncherFontPreferences.normalizeFontFamilyFromStorage(
                        prefs[LAUNCHER_FONT_FAMILY_KEY]
                )
            }

    suspend fun setLauncherFontFamilyName(familyName: String) {
        val trimmed = familyName.trim()
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            if (trimmed.isEmpty()) prefs.remove(LAUNCHER_FONT_FAMILY_KEY)
            else prefs[LAUNCHER_FONT_FAMILY_KEY] = trimmed
        }
    }

    // --- App language (per-app locale) ---

    val appLocaleTagFlow: Flow<String> = prefFlow(APP_LOCALE_TAG_KEY, "")

    suspend fun setAppLocaleTag(tag: String) {
        val trimmed = tag.trim()
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            if (trimmed.isEmpty()) prefs.remove(APP_LOCALE_TAG_KEY)
            else prefs[APP_LOCALE_TAG_KEY] = trimmed
        }
    }

    // --- Screen rotation ---

    val allowLandscapeRotationFlow: Flow<Boolean> =
            prefFlow(ALLOW_LANDSCAPE_ROTATION_KEY, false)
    suspend fun setAllowLandscapeRotation(allow: Boolean) =
            setPref(ALLOW_LANDSCAPE_ROTATION_KEY, allow)

    val doubleTapEmptyLockFlow: Flow<Boolean> = prefFlow(DOUBLE_TAP_EMPTY_LOCK_KEY, false)
    suspend fun setDoubleTapEmptyLock(enabled: Boolean) =
            setPref(DOUBLE_TAP_EMPTY_LOCK_KEY, enabled)

    val longLockReturnHomeFlow: Flow<Boolean> = prefFlow(LONG_LOCK_RETURN_HOME_KEY, false)
    suspend fun setLongLockReturnHome(enabled: Boolean) =
            setPref(LONG_LOCK_RETURN_HOME_KEY, enabled)

    val longLockReturnHomeThresholdMinutesFlow: Flow<Int> =
            prefFlow(
                    LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES_KEY,
                    DEFAULT_LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES,
            )
    suspend fun setLongLockReturnHomeThresholdMinutes(minutes: Int) =
            setPref(LONG_LOCK_RETURN_HOME_THRESHOLD_MINUTES_KEY, minutes)

    val longLockLastScreenOffAtMsFlow: Flow<Long> =
            prefFlow(LONG_LOCK_LAST_SCREEN_OFF_AT_MS_KEY, 0L)
    suspend fun setLongLockLastScreenOffAtMs(timestampMs: Long) =
            setPref(LONG_LOCK_LAST_SCREEN_OFF_AT_MS_KEY, timestampMs)

    suspend fun clearLongLockLastScreenOffAtMs() {
        context.fokusLauncherPreferencesDataStore.edit { prefs ->
            prefs.remove(LONG_LOCK_LAST_SCREEN_OFF_AT_MS_KEY)
        }
    }

    suspend fun getLongLockReturnHomeEnabled(): Boolean = longLockReturnHomeFlow.first()

    suspend fun getLongLockReturnHomeThresholdMinutes(): Int =
            longLockReturnHomeThresholdMinutesFlow.first()

    suspend fun getLongLockLastScreenOffAtMs(): Long = longLockLastScreenOffAtMsFlow.first()

    /** Clears all preferences, equivalent to clearing app storage. */
    suspend fun clearAll() {
        context.fokusLauncherPreferencesDataStore.edit { prefs -> prefs.clear() }
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
                        iconPackage = semiParts.getOrElse(3) { "" },
                        profileKey = semiParts.getOrElse(4) { "0" },
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
        if (raw.isBlank() || raw == RIGHT_SIDE_SHORTCUTS_EMPTY_MARKER) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(";")
            if (parts.size < 2) return@mapNotNull null
            val iconName = parts[0].ifBlank { "circle" }
            val target = ShortcutTarget.decode(parts[1]) ?: return@mapNotNull null
            val profileKey = parts.getOrElse(2) { "0" }.ifBlank { "0" }
            HomeShortcut(iconName = iconName, target = target, profileKey = profileKey)
        }
    }

    private fun serializeRightSideShortcuts(shortcuts: List<HomeShortcut>): String {
        if (shortcuts.isEmpty()) return RIGHT_SIDE_SHORTCUTS_EMPTY_MARKER
        return shortcuts.joinToString("|") { shortcut ->
            "${shortcut.iconName};${ShortcutTarget.encode(shortcut.target)};${shortcut.profileKey}"
        }
    }

    private fun parseDrawerOpenCounts(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3) return@mapNotNull null
            val count = parts[2].toIntOrNull() ?: return@mapNotNull null
            "${parts[0]}|${parts[1]}" to count
        }.toMap()
    }

    private fun serializeDrawerOpenCounts(map: Map<String, Int>): String {
        if (map.isEmpty()) return ""
        return map.entries.joinToString(";") { (key, count) ->
            val parts = key.split("|")
            "${parts[0]}|${parts[1]}|$count"
        }
    }

    private fun parseDrawerCategoryIcons(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                val it = o.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val v = o.optString(k, "")
                    if (k.isNotBlank() && v.isNotBlank()) put(k, v)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun serializeDrawerCategoryIcons(map: Map<String, String>): String {
        if (map.isEmpty()) return ""
        val o = JSONObject()
        map.entries.sortedBy { it.key }.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    private fun parseDrawerDotSearchTargetJson(raw: String): DotSearchTargetPreference? {
        if (raw.isBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            val profileKey = o.optString("profileKey", "0").ifBlank { "0" }
            val targetRaw = o.optString("target", "")
            val target =
                    if (targetRaw.isBlank()) null else ShortcutTarget.decode(targetRaw) ?: return null
            DotSearchTargetPreference(profileKey = profileKey, target = target)
        }.getOrNull()
    }

    private fun serializeDrawerDotSearchTarget(config: DotSearchTargetPreference): String {
        if (config.target == null && config.profileKey == "0") return ""
        val o = JSONObject()
        o.put("profileKey", config.profileKey.ifBlank { "0" })
        o.put("target", if (config.target == null) "" else ShortcutTarget.encode(config.target))
        return o.toString()
    }

    private fun parseDrawerDotSearchAliasesJson(raw: String): Map<Char, DotSearchTargetPreference> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.length != 1) continue
                    val keyChar = k.single().lowercaseChar()
                    if (keyChar !in 'a'..'z') continue
                    val inner = root.optJSONObject(k) ?: continue
                    val profileKey = inner.optString("profileKey", "0").ifBlank { "0" }
                    val targetRaw = inner.optString("target", "")
                    val target = ShortcutTarget.decode(targetRaw) ?: continue
                    put(keyChar, DotSearchTargetPreference(profileKey, target))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun serializeDrawerDotSearchAliases(map: Map<Char, DotSearchTargetPreference>): String {
        if (map.isEmpty()) return ""
        val o = JSONObject()
        map.entries.sortedBy { it.key }.forEach { (ch, pref) ->
            val inner = JSONObject()
            inner.put("profileKey", pref.profileKey.ifBlank { "0" })
            inner.put("target", ShortcutTarget.encode(pref.target))
            o.put(ch.lowercaseChar().toString(), inner)
        }
        return o.toString()
    }

    private fun parseDrawerCustomAppOrderJson(raw: String): Map<String, List<String>> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                val keys = o.keys()
                while (keys.hasNext()) {
                    val profileKey = keys.next()
                    val arr = o.optJSONArray(profileKey) ?: continue
                    val entries = buildList {
                        for (i in 0 until arr.length()) {
                            val s = arr.optString(i, "").ifBlank { continue }
                            add(s)
                        }
                    }
                    if (entries.isNotEmpty()) put(profileKey, entries)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun serializeDrawerCustomAppOrderJson(map: Map<String, List<String>>): String {
        val o = JSONObject()
        map.entries.sortedBy { it.key }.forEach { (profileKey, keys) ->
            o.put(profileKey, JSONArray(keys))
        }
        return o.toString()
    }
}
