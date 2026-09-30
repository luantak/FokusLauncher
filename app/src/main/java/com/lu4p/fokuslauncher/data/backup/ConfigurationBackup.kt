package com.lu4p.fokuslauncher.data.backup

import android.content.Context
import android.graphics.Typeface
import android.util.Base64
import com.lu4p.fokuslauncher.data.font.CustomFontStore
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import java.io.File
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.lu4p.fokuslauncher.data.database.AppDatabase
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.database.entity.SuppressedCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class ConfigurationBackup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {
    private val mutex = Mutex()
    private val fontFile: File
        get() = File(File(context.filesDir, CustomFontStore.FONTS_DIR_NAME), LauncherFontPreferences.CUSTOM_FONT_ACTIVE_FILE)

    class Snapshot internal constructor(
        internal val preferences: Map<String, Any>,
        internal val hidden: List<HiddenAppEntity>,
        internal val renamed: List<RenamedAppEntity>,
        internal val categories: List<AppCategoryEntity>,
        internal val definitions: List<AppCategoryDefinitionEntity>,
        internal val suppressed: List<SuppressedCategoryDefinitionEntity>,
        internal val font: ByteArray?,
    ) {
        val localeTag: String get() = preferences["app_locale_tag"] as? String ?: ""
        val usesPhotoWallpaper: Boolean get() = preferences["home_uses_photo_wallpaper"] as? Boolean ?: false
    }

    suspend fun export(): String = mutex.withLock {
        val preferences = JSONObject()
        context.fokusLauncherPreferencesDataStore.data.first().asMap().forEach { (key, value) ->
            if (key.name in preferenceTypes) preferences.put(key.name, value)
        }
        val dao = database.appDao()
        val root = database.withTransaction {
            JSONObject().put("format", FORMAT).put("version", 1)
                .put("preferences", preferences)
                .put("hidden", array(dao.getHiddenApps().first()) {
                    app(it.packageName, it.profileKey, it.launcherShortcutId)
                })
                .put("renamed", array(dao.getAllRenamedApps().first()) {
                    app(it.packageName, it.profileKey, it.launcherShortcutId).put("customName", it.customName)
                })
                .put("categories", array(dao.getAllAppCategories().first()) {
                    app(it.packageName, it.profileKey, it.launcherShortcutId).put("category", it.category)
                })
                .put("definitions", array(dao.getAllCategoryDefinitions().first()) {
                    JSONObject().put("name", it.name).put("position", it.position)
                })
                .put("suppressed", array(dao.getAllSuppressedCategoryDefinitions().first()) {
                    JSONObject().put("name", it.name)
                })
        }
        if (preferences.optString("launcher_font_family") == LauncherFontPreferences.CUSTOM_FONT_STORAGE) {
            require(fontFile.isFile && fontFile.length() <= MAX_FONT_BYTES)
            root.put("font", Base64.encodeToString(fontFile.readBytes(), Base64.NO_WRAP))
        }
        root.toString(2).also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) }
    }

    fun parse(text: String): Snapshot {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.get("format") == FORMAT && root.get("version") == 1)
        val rawPreferences = root.getJSONObject("preferences")
        val preferences = rawPreferences.keys().asSequence().associateWith { name ->
            val value = rawPreferences.get(name)
            require(preferenceTypes[name]?.accepts(value) == true) { "Invalid preference" }
            if (preferenceTypes[name] == Type.FLOAT) (value as Number).toFloat() else value
        }
        val family = preferences["launcher_font_family"] as? String ?: ""
        require(!LauncherFontPreferences.isCustomFont(family) || family == LauncherFontPreferences.CUSTOM_FONT_STORAGE)
        val font = if (root.has("font")) {
            val bytes = Base64.decode(string(root, "font"), Base64.NO_WRAP)
            require(bytes.size in 1..MAX_FONT_BYTES)
            val temp = File.createTempFile("backup-font-", ".ttf", context.cacheDir)
            try {
                temp.writeBytes(bytes)
                Typeface.createFromFile(temp)
            } finally {
                temp.delete()
            }
            bytes
        } else null
        require(family != LauncherFontPreferences.CUSTOM_FONT_STORAGE || font != null)
        return Snapshot(
            preferences,
            rows(root, "hidden") { HiddenAppEntity(string(it, "packageName"), string(it, "profileKey"), string(it, "launcherShortcutId")) },
            rows(root, "renamed") { RenamedAppEntity(string(it, "packageName"), string(it, "profileKey"), string(it, "customName"), string(it, "launcherShortcutId")) },
            rows(root, "categories") { AppCategoryEntity(string(it, "packageName"), string(it, "profileKey"), string(it, "category"), string(it, "launcherShortcutId")) },
            rows(root, "definitions") {
                val position = it.get("position")
                require(position is Int && position >= 0)
                AppCategoryDefinitionEntity(string(it, "name"), position)
            },
            rows(root, "suppressed") { SuppressedCategoryDefinitionEntity(string(it, "name")) },
            font,
        )
    }

    suspend fun restore(snapshot: Snapshot) = mutex.withLock {
        val dao = database.appDao()
        val previousPreferences = context.fokusLauncherPreferencesDataStore.data.first()
        val previousFont = fontFile.takeIf { it.isFile }?.readBytes()
        var preferencesWritten = false
        try {
            writeFont(snapshot.font)
            database.withTransaction {
                dao.clearAllHiddenApps()
                dao.clearAllRenamedApps()
                dao.clearAllAppCategories()
                dao.clearAllCategoryDefinitions()
                dao.clearAllSuppressedCategoryDefinitions()
                snapshot.hidden.forEach { dao.hideApp(it) }
                snapshot.renamed.forEach { dao.renameApp(it) }
                dao.upsertAppCategories(snapshot.categories)
                dao.upsertCategoryDefinitions(snapshot.definitions)
                snapshot.suppressed.forEach { dao.upsertSuppressedCategoryDefinition(it) }
                preferencesWritten = true
                context.fokusLauncherPreferencesDataStore.edit { preferences ->
                    preferenceTypes.forEach { (name, type) -> type.remove(preferences, name) }
                    snapshot.preferences.forEach { (name, value) -> preferenceTypes.getValue(name).set(preferences, name, value) }
                }
            }
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                try {
                    if (preferencesWritten) context.fokusLauncherPreferencesDataStore.edit { preferences ->
                        preferenceTypes.forEach { (name, type) -> type.remove(preferences, name) }
                        previousPreferences.asMap().forEach { (key, value) ->
                            preferenceTypes[key.name]?.set(preferences, key.name, value)
                        }
                    }
                    writeFont(previousFont)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
            }
            throw failure
        }
    }

    private fun writeFont(bytes: ByteArray?) {
        if (bytes == null) {
            if (fontFile.exists()) check(fontFile.delete())
            return
        }
        check(fontFile.parentFile!!.isDirectory || fontFile.parentFile!!.mkdirs())
        val file = android.util.AtomicFile(fontFile)
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }

    private enum class Type {
        STRING, BOOLEAN, INT, FLOAT;

        fun accepts(value: Any): Boolean = when (this) {
            STRING -> value is String
            BOOLEAN -> value is Boolean
            INT -> value is Int
            FLOAT -> value is Number && value.toFloat().isFinite()
        }

        fun remove(preferences: MutablePreferences, name: String) {
            when (this) {
                STRING -> preferences.remove(stringPreferencesKey(name))
                BOOLEAN -> preferences.remove(booleanPreferencesKey(name))
                INT -> preferences.remove(intPreferencesKey(name))
                FLOAT -> preferences.remove(floatPreferencesKey(name))
            }
        }

        fun set(preferences: MutablePreferences, name: String, value: Any) {
            when (this) {
                STRING -> preferences[stringPreferencesKey(name)] = value as String
                BOOLEAN -> preferences[booleanPreferencesKey(name)] = value as Boolean
                INT -> preferences[intPreferencesKey(name)] = value as Int
                FLOAT -> preferences[floatPreferencesKey(name)] = (value as Number).toFloat()
            }
        }
    }

    companion object {
        const val MAX_BYTES = 16 * 1024 * 1024
        private const val MAX_FONT_BYTES = 8 * 1024 * 1024
        private const val FORMAT = "fokus-launcher-configuration"

        private val preferenceTypes = buildMap {
            listOf(
                "favorite_apps", "swipe_left_app", "swipe_right_app", "double_tap_empty_target",
                "right_side_shortcuts", "world_clock_cities", "countdown_event", "home_extra_widgets",
                "preferred_weather_app", "preferred_clock_app", "preferred_calendar_app",
                "home_date_format_style", "temperature_unit", "pomodoro_config", "home_note_text",
                "notification_indicator_style", "drawer_category_icons", "drawer_app_sort_mode",
                "drawer_custom_app_order", "drawer_dot_search_default", "drawer_dot_search_aliases",
                "profile_display_names", "home_alignment", "launcher_visual_style", "home_app_icon_mode",
                "launcher_font_family", "launcher_custom_font_display_name", "app_locale_tag",
                "two_finger_swipe_up", "two_finger_swipe_down", "two_finger_swipe_left", "two_finger_swipe_right",
            ).forEach { put(it, Type.STRING) }
            listOf(
                "show_status_bar", "show_home_clock", "show_home_date", "show_home_weather",
                "show_home_air_quality", "show_world_clock_weather", "show_home_battery",
                "show_home_media", "show_home_pomodoro", "show_home_screen_time", "show_home_note",
                "show_notification_indicators", "drawer_sidebar_categories", "drawer_category_sidebar_on_left",
                "drawer_search_auto_launch", "drawer_scroll_to_top_auto_keyboard", "launcher_glow_enabled",
                "use_arcticons_drawer_icons", "home_uses_photo_wallpaper", "allow_landscape_rotation",
                "double_tap_empty_lock", "long_lock_return_home",
            ).forEach { put(it, Type.BOOLEAN) }
            listOf("notification_indicator_color", "long_lock_return_home_threshold_minutes").forEach { put(it, Type.INT) }
            listOf("photo_wallpaper_outline_width_dp", "photo_wallpaper_drawer_overlay_intensity", "launcher_font_scale").forEach { put(it, Type.FLOAT) }
        }

        private fun app(packageName: String, profileKey: String, shortcutId: String) =
            JSONObject().put("packageName", packageName).put("profileKey", profileKey).put("launcherShortcutId", shortcutId)

        private fun string(row: JSONObject, name: String): String =
            (row.get(name) as? String) ?: throw IllegalArgumentException("Invalid string")

        private fun <T> array(values: List<T>, encode: (T) -> JSONObject) =
            JSONArray().apply { values.forEach { put(encode(it)) } }

        private fun <T> rows(root: JSONObject, name: String, decode: (JSONObject) -> T): List<T> {
            val array = root.getJSONArray(name)
            require(array.length() <= 10000)
            return List(array.length()) { decode(array.getJSONObject(it)) }
        }
    }
}
