package com.lu4p.fokuslauncher.ui.navigation

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.lu4p.fokuslauncher.MainActivity
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.DiscordInvitationOutcome
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class DiscordInvitationNavigationTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    @Inject lateinit var preferences: PreferencesManager
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val store get() = instrumentation.targetContext.fokusLauncherPreferencesDataStore
    private val completedAt = longPreferencesKey("discord_invitation_onboarding_completed_at_ms")
    private val outcome = stringPreferencesKey("discord_invitation_outcome")
    private val onboarding = booleanPreferencesKey("has_completed_onboarding")
    private var original = emptyMap<androidx.datastore.preferences.core.Preferences.Key<*>, Any>()

    @Before fun setUp() = runBlocking {
        hiltRule.inject()
        original = store.data.first().asMap()
        store.edit {
            it[onboarding] = true
            it.remove(completedAt) // Genuine pre-feature completed install.
            it.remove(outcome)
        }
        preferences.setDrawerSidebarCategories(false)
    }

    @After fun restorePreferences() = runBlocking {
        store.edit { restored ->
            restored.clear()
            original.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                restored[key as androidx.datastore.preferences.core.Preferences.Key<Any>] = value
            }
        }
        Unit
    }

    @Test fun noThanksConsumesInvitationAndIsSettingsOnly() = roundTrip {
        saveScreenshot()
        compose.onNodeWithText("No thanks").performClick()
    }

    @Test fun backDismissalConsumesInvitation() = roundTrip { Espresso.pressBack() }

    @Test fun outsideDismissalConsumesInvitation() = roundTrip {
        val automation = instrumentation.uiAutomation
        val now = android.os.SystemClock.uptimeMillis()
        val height = instrumentation.targetContext.resources.displayMetrics.heightPixels
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 2f, height / 2f, 0)
        val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 2f, height / 2f, 0)
        try {
            automation.injectInputEvent(down, true)
            automation.injectInputEvent(up, true)
        } finally { down.recycle(); up.recycle() }
    }

    @Test fun joinPersistsTheOpenedOutcomeBeforeLaunchingTheExistingInvite() {
        var requestedUrl: String? = null
        var outcomeAtLaunch: DiscordInvitationOutcome? = null
        val monitor = object : android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: android.content.Intent): android.app.Instrumentation.ActivityResult? {
                if (intent.action != android.content.Intent.ACTION_VIEW) return null
                requestedUrl = intent.dataString
                outcomeAtLaunch = runBlocking { preferences.discordInvitationOutcomeFlow.first() }
                return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                openSettings()
                awaitInvitation()
                compose.onNodeWithText("Join Discord").performClick()
                compose.waitUntil(5_000) { requestedUrl != null }
                assertEquals("https://discord.gg/DHVPC9Ty7g", requestedUrl)
                assertEquals(DiscordInvitationOutcome.LINK_OPENED, outcomeAtLaunch)
                compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
                scenario.recreate()
                compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun recreationCannotRepeatAnUnansweredInvitation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openSettings()
            awaitInvitation()
            compose.onNodeWithText("Help shape Fokus").assertIsDisplayed()
            scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
            assertEquals(DiscordInvitationOutcome.SHOWN, runBlocking { preferences.discordInvitationOutcomeFlow.first() })
        }
    }

    @Test fun freshInstallIsNotInvitedOnHomeOrInEarlySettings() {
        runBlocking { store.edit { it[completedAt] = System.currentTimeMillis() } }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("clock_widget").assertIsDisplayed()
            compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
            openSettings()
            compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
            assertEquals(DiscordInvitationOutcome.NOT_SHOWN, runBlocking { preferences.discordInvitationOutcomeFlow.first() })
        }
    }

    private fun roundTrip(dismiss: () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithTag("clock_widget").assertIsDisplayed()
            compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
            openSettings()
            awaitInvitation()
            compose.onNodeWithText("Help shape Fokus").assertIsDisplayed()
            compose.onNodeWithText("Join our Discord community to share ideas, get help, and see what’s coming next.").assertIsDisplayed()
            dismiss()
            compose.waitUntil(5_000) {
                runBlocking { preferences.discordInvitationOutcomeFlow.first() } == DiscordInvitationOutcome.DISMISSED
            }
            compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
            compose.onNodeWithTag("settings_screen").assertIsDisplayed()
            var back = ""
            scenario.onActivity { back = it.getString(R.string.action_back) }
            compose.onNodeWithContentDescription(back).performClick()
            compose.onNodeWithTag("clock_widget").assertIsDisplayed()
            openSettings()
            compose.onNodeWithText("Help shape Fokus").assertDoesNotExist()
        }
    }

    private fun openSettings() {
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithTag("settings_button").performClick()
        compose.onNodeWithTag("menu_settings").performClick()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.waitForIdle()
    }

    private fun awaitInvitation() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("discord_invitation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun saveScreenshot() {
        val file = File(instrumentation.targetContext.filesDir, "discord-invitation.png")
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
