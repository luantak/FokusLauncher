package com.lu4p.fokuslauncher.ui.navigation

import android.content.ComponentName
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.lu4p.fokuslauncher.MainActivity
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.local.TwoFingerDirection
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.WidgetTapTarget
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PrivateSpaceGestureTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var preferences: PreferencesManager
    @Inject lateinit var privateSpace: PrivateSpaceManager

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            FileInputStream(it.fileDescriptor).bufferedReader().readText().trim()
        }

    /** Opt in with privateSpaceFixture=true (credential-free profile) or false (no profile). */
    @Test
    fun gesturePickersAndDispatchFollowPrivateSpaceAvailability() {
        val fixture = InstrumentationRegistry.getArguments().getString("privateSpaceFixture")
        assumeTrue(fixture == "true" || fixture == "false")
        val hasFixture = fixture == "true"
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val originalHome = shell("cmd role get-role-holders --user 0 android.app.role.HOME")
        val originalLeft = runBlocking { preferences.swipeLeftTargetFlow.first() }
        val originalRight = runBlocking { preferences.swipeRightTargetFlow.first() }
        val originalDouble = runBlocking { preferences.doubleTapEmptyTargetFlow.first() }
        val originalLock = runBlocking { preferences.doubleTapEmptyLockFlow.first() }
        val originalOnboarding = runBlocking { preferences.hasCompletedOnboardingFlow.first() }
        val originalTwo = runBlocking {
            TwoFingerDirection.entries.associateWith { preferences.twoFingerTargetFlow(it).first() }
        }
        try {
            val component = ComponentName(context, MainActivity::class.java).flattenToString()
            shell("cmd package set-home-activity $component")
            assertEquals("Unexpected Private Space fixture", hasFixture, privateSpace.hasPrivateSpaceProfile())
            runBlocking {
                preferences.setHasCompletedOnboarding(true)
                preferences.setSwipeLeftTarget(null)
                preferences.setSwipeRightTarget(null)
                preferences.setDoubleTapEmptyLock(false)
                preferences.setDoubleTapEmptyTarget(null)
                TwoFingerDirection.entries.forEach {
                    preferences.setTwoFingerTarget(it, null)
                }
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                checkGesturePickers(scenario, hasFixture)
                if (!hasFixture) return@use
                compose.onNodeWithTag("clock_widget").assertIsDisplayed()
                repeat(2) { index ->
                    lockFixture()
                    compose.onRoot().performTouchInput {
                        val start = Offset(width * if (index == 0) 0.75f else 0.25f, height * 0.45f)
                        val end = Offset(width - start.x, start.y)
                        swipe(start, end, durationMillis = 400)
                    }
                    awaitUnlock("single-finger swipe $index")
                }
                TwoFingerDirection.entries.forEach { direction ->
                    lockFixture()
                    compose.onRoot().performTouchInput {
                        val delta = when (direction) {
                            TwoFingerDirection.UP -> Offset(0f, -height * 0.025f)
                            TwoFingerDirection.DOWN -> Offset(0f, height * 0.025f)
                            TwoFingerDirection.LEFT -> Offset(-width * 0.04f, 0f)
                            TwoFingerDirection.RIGHT -> Offset(width * 0.04f, 0f)
                        }
                        down(0, Offset(width * 0.45f, height * 0.4f))
                        down(1, Offset(width * 0.55f, height * 0.4f))
                        repeat(10) {
                            moveBy(0, delta)
                            moveBy(1, delta)
                        }
                        up(0)
                        up(1)
                    }
                    awaitUnlock("two-finger $direction")
                }
                lockFixture()
                compose.onNodeWithTag("home_screen").performTouchInput {
                    doubleClick(Offset(width * 0.5f, height * 0.1f))
                }
                awaitUnlock("double-tap")
            }
        } finally {
            runBlocking {
                preferences.setSwipeLeftTarget(originalLeft)
                preferences.setSwipeRightTarget(originalRight)
                preferences.setDoubleTapEmptyTarget(originalDouble)
                preferences.setDoubleTapEmptyLock(originalLock)
                preferences.setHasCompletedOnboarding(originalOnboarding)
                originalTwo.forEach { (direction, target) -> preferences.setTwoFingerTarget(direction, target) }
            }
            if (originalHome.isNotBlank()) {
                shell("cmd role add-role-holder --user 0 android.app.role.HOME $originalHome")
            }
        }
    }

    private fun checkGesturePickers(scenario: ActivityScenario<MainActivity>, hasFixture: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithTag("clock_widget").assertIsDisplayed()
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithTag("settings_button").performClick()
        compose.onNodeWithTag("menu_settings").performClick()
        compose.onNodeWithText(context.getString(R.string.settings_gestures)).performScrollTo().performClick()
        val option = hasText(context.getString(R.string.drawer_private_space_unlock)) and
            hasAnyAncestor(isDialog())
        listOf(
            R.string.settings_swipe_left, R.string.settings_swipe_right,
            R.string.settings_double_tap, R.string.settings_two_finger_up,
            R.string.settings_two_finger_down, R.string.settings_two_finger_left,
            R.string.settings_two_finger_right,
        ).forEach { label ->
            val labelBounds = compose.onNodeWithText(context.getString(label)).performScrollTo()
                .fetchSemanticsNode().boundsInRoot
            val edit = compose.onAllNodesWithContentDescription(context.getString(R.string.action_change))
                .fetchSemanticsNodes().minBy { abs(it.boundsInRoot.center.y - labelBounds.center.y) }
            assertTrue(abs(edit.boundsInRoot.center.y - labelBounds.center.y) < labelBounds.height)
            compose.onNode(SemanticsMatcher("Edit button for gesture") { it.id == edit.id }).performClick()
            compose.onNode(isDialog()).assertIsDisplayed()
            if (hasFixture) {
                compose.waitUntil(10_000) { compose.onAllNodes(option).fetchSemanticsNodes().isNotEmpty() }
                compose.onNode(option).performClick()
            } else {
                compose.onNode(option).assertDoesNotExist()
                compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
            }
        }
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        if (hasFixture) {
            compose.waitUntil(10_000) {
                runBlocking {
                    preferences.swipeLeftTargetFlow.first() == ShortcutTarget.UnlockPrivateSpace &&
                        preferences.swipeRightTargetFlow.first() == ShortcutTarget.UnlockPrivateSpace &&
                        preferences.doubleTapEmptyTargetFlow.first() == WidgetTapTarget(ShortcutTarget.UnlockPrivateSpace) &&
                        TwoFingerDirection.entries.all {
                            preferences.twoFingerTargetFlow(it).first() == ShortcutTarget.UnlockPrivateSpace
                        }
                }
            }
        }
    }

    private fun lockFixture() {
        assertTrue(privateSpace.lock())
        val profile = privateSpace.getPrivateSpaceProfile()!!.hashCode()
        compose.waitUntil(30_000) {
            !privateSpace.isPrivateSpaceUnlocked() && shell("am is-user-stopped $profile") == "true"
        }
        compose.waitForIdle()
    }

    private fun awaitUnlock(gesture: String) {
        val profile = privateSpace.getPrivateSpaceProfile()!!.hashCode()
        try {
            compose.waitUntil(30_000) {
                privateSpace.isPrivateSpaceUnlocked() &&
                    shell("am get-started-user-state $profile").contains("RUNNING_UNLOCKED")
            }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError("Private Space did not unlock after $gesture", error)
        }
        compose.onNodeWithTag("clock_widget").assertIsDisplayed()
    }
}
