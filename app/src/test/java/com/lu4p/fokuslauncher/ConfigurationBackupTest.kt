package com.lu4p.fokuslauncher

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import com.lu4p.fokuslauncher.data.backup.ConfigurationBackup
import com.lu4p.fokuslauncher.data.database.AppDatabase
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [28])
class ConfigurationBackupTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var backup: ConfigurationBackup

    @Before
    fun setUp() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        backup = ConfigurationBackup(context, database)
        context.fokusLauncherPreferencesDataStore.edit { it.clear() }
        Unit
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun roundTripRestoresPreferencesAndCustomCategories() = runBlocking {
        val favorites = stringPreferencesKey("favorite_apps")
        context.fokusLauncherPreferencesDataStore.edit { it[favorites] = "Mail;org.example.mail;mail;;0" }
        database.appDao().upsertCategoryDefinition(AppCategoryDefinitionEntity("Mine", 3))
        database.appDao().setAppCategory(AppCategoryEntity("org.example.mail", "0", "Mine"))
        val exported = backup.export()
        context.fokusLauncherPreferencesDataStore.edit { it[favorites] = "changed" }
        database.appDao().clearAllAppCategories()
        database.appDao().clearAllCategoryDefinitions()
        backup.restore(backup.parse(exported))
        assertEquals("Mail;org.example.mail;mail;;0", context.fokusLauncherPreferencesDataStore.data.first()[favorites])
        assertEquals(listOf(AppCategoryDefinitionEntity("Mine", 3)), database.appDao().getAllCategoryDefinitions().first())
        assertEquals("Mine", database.appDao().getAppCategory("org.example.mail", "0", ""))
    }

    @Test
    fun restoreReplacesOldPreferencesButPreservesDeviceState() = runBlocking {
        val clock = stringPreferencesKey("preferred_clock_app")
        val widgets = stringPreferencesKey("hosted_widgets")
        val disclosure = androidx.datastore.preferences.core.booleanPreferencesKey("accessibility_prominent_disclosure_accepted")
        val exported = backup.export()
        context.fokusLauncherPreferencesDataStore.edit {
            it[clock] = "org.example.clock"
            it[widgets] = "device-widget-42"
            it[disclosure] = true
        }
        backup.restore(backup.parse(exported))
        val preferences = context.fokusLauncherPreferencesDataStore.data.first()
        assertEquals(null, preferences[clock])
        assertEquals("device-widget-42", preferences[widgets])
        assertEquals(true, preferences[disclosure])
    }

    @Test
    fun roundTripPreservesTypesAndAllDatabaseTables() = runBlocking {
        val boolean = androidx.datastore.preferences.core.booleanPreferencesKey("show_home_weather")
        val integer = androidx.datastore.preferences.core.intPreferencesKey("notification_indicator_color")
        val float = androidx.datastore.preferences.core.floatPreferencesKey("launcher_font_scale")
        context.fokusLauncherPreferencesDataStore.edit {
            it[boolean] = false
            it[integer] = -123
            it[float] = 1.25f
        }
        val dao = database.appDao()
        val hidden = com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity("org.example.app", "10", "shortcut")
        val renamed = com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity("org.example.app", "10", "My app", "shortcut")
        val suppressed = com.lu4p.fokuslauncher.data.database.entity.SuppressedCategoryDefinitionEntity("Games")
        dao.hideApp(hidden)
        dao.renameApp(renamed)
        dao.upsertSuppressedCategoryDefinition(suppressed)
        val exported = backup.export()
        dao.clearAllHiddenApps()
        dao.clearAllRenamedApps()
        dao.clearAllSuppressedCategoryDefinitions()
        context.fokusLauncherPreferencesDataStore.edit { it.clear() }
        backup.restore(backup.parse(exported))
        val preferences = context.fokusLauncherPreferencesDataStore.data.first()
        assertEquals(false, preferences[boolean])
        assertEquals(-123, preferences[integer])
        assertEquals(1.25f, preferences[float])
        assertEquals(listOf(hidden), dao.getHiddenApps().first())
        assertEquals(listOf(renamed), dao.getAllRenamedApps().first())
        assertEquals(listOf(suppressed), dao.getAllSuppressedCategoryDefinitions().first())
    }

    @Test
    fun exportDoesNotIncludeLocationConsentWidgetsOrRunningTimer() = runBlocking {
        context.fokusLauncherPreferencesDataStore.edit {
            it[stringPreferencesKey("last_weather_location")] = "private location"
            it[stringPreferencesKey("hosted_widgets")] = "device widgets"
            it[stringPreferencesKey("pomodoro_runtime")] = "active timer"
        }
        val preferences = org.json.JSONObject(backup.export()).getJSONObject("preferences")
        assertEquals(0, preferences.length())
    }

    @Test
    fun invalidBackupsLeaveCurrentDataUnchanged() = runBlocking {
        val favorites = stringPreferencesKey("favorite_apps")
        context.fokusLauncherPreferencesDataStore.edit { it[favorites] = "existing" }
        val valid = backup.export()
        val cases = listOf(
            "not json",
            org.json.JSONObject(valid).put("version", 2).toString(),
            org.json.JSONObject(valid).put("format", "another app").toString(),
            org.json.JSONObject(valid).apply { getJSONObject("preferences").put("show_home_weather", "true") }.toString(),
            org.json.JSONObject(valid).apply { getJSONObject("preferences").put("favorite_apps", 123) }.toString(),
            org.json.JSONObject(valid).apply { getJSONObject("preferences").put("accessibility_prominent_disclosure_accepted", true) }.toString(),
            org.json.JSONObject(valid).apply { getJSONObject("preferences").put("launcher_font_family", "custom:../../outside.ttf") }.toString(),
            org.json.JSONObject(valid).apply { getJSONObject("preferences").put("launcher_font_family", "custom:active.ttf") }.toString(),
            org.json.JSONObject(valid).put("definitions", org.json.JSONArray().put(org.json.JSONObject().put("name", "Mine").put("position", "3"))).toString(),
            valid.dropLast(5),
        )
        for (text in cases) {
            org.junit.Assert.assertThrows(Exception::class.java) { backup.parse(text) }
            assertEquals("existing", context.fokusLauncherPreferencesDataStore.data.first()[favorites])
        }
    }

    @Test
    fun oversizedBackupIsRejected() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            backup.parse(" ".repeat(ConfigurationBackup.MAX_BYTES + 1))
        }
    }
}
