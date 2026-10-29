package com.lu4p.fokuslauncher.pomodoro

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.PomodoroConfig
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import com.lu4p.fokuslauncher.data.model.idleRuntimeFor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PomodoroDisableTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Inject lateinit var preferences: PreferencesManager
    @Inject lateinit var repository: PomodoroRepository
    @Inject lateinit var alerter: PomodoroCompletionAlerter
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifications: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() = runBlocking {
        hiltRule.inject()
        preferences.setShowHomePomodoro(false)
        preferences.setPomodoroConfig(PomodoroConfig())
        preferences.setShowHomePomodoro(true)
        repository.start()
        await("initial enabled") { repository.uiState.value.enabled }
    }

    @After
    fun tearDown() = runBlocking {
        preferences.setShowHomePomodoro(false)
        alerter.dismiss()
    }

    @Test
    fun disablingPomodoroStopsRunningAndOvertimeSessions() = runBlocking {
        disablingRunningTimerCancelsAndroidAlarmAndStaysSilentPastDeadline()
        switchingToMediaDismissesCompletionAndStaleDismissCannotStartBreak()
    }

    private suspend fun disablingRunningTimerCancelsAndroidAlarmAndStaysSilentPastDeadline() {
        preferences.setShowHomePomodoro(true)
        await("enabled") { repository.uiState.value.enabled }
        val deadline = System.currentTimeMillis() + 3_000L
        preferences.setPomodoroRuntime(
                idleRuntimeFor(PomodoroConfig(), PomodoroMode.FOCUS).copy(
                        phase = PomodoroPhase.RUNNING,
                        endsAtEpochMs = deadline,
                ),
        )
        await("scheduled running") { repository.uiState.value.isRunning && alarmIntent() != null }
        await("active notification") { notifications.activeNotifications.any { it.id == PomodoroActiveNotifier.NOTIFICATION_ID } }

        preferences.setShowHomePomodoro(false)
        await("cancelled alarm") { !repository.uiState.value.enabled && alarmIntent() == null }
        delay((deadline - System.currentTimeMillis()).coerceAtLeast(0L) + 1_500L)

        assertNull(alarmIntent())
        assertStopped()
        preferences.setShowHomePomodoro(true)
        await("reenabled") { repository.uiState.value.enabled }
        assertEquals(PomodoroPhase.IDLE, repository.uiState.value.phase)
    }

    private suspend fun switchingToMediaDismissesCompletionAndStaleDismissCannotStartBreak() {
        preferences.setShowHomePomodoro(true)
        await("enabled") { repository.uiState.value.enabled }
        preferences.setPomodoroRuntime(
                idleRuntimeFor(PomodoroConfig(), PomodoroMode.FOCUS).copy(
                        phase = PomodoroPhase.OVERTIME,
                        endsAtEpochMs = System.currentTimeMillis() - 1_000L,
                ),
        )
        await("completion notification") { notifications.activeNotifications.any { it.id == PomodoroCompletionAlerter.NOTIFICATION_ID } }

        preferences.setShowHomeMedia(true)
        await("media replacement") { !repository.uiState.value.enabled && notifications.activeNotifications.none { it.id == PomodoroCompletionAlerter.NOTIFICATION_ID } }
        context.sendBroadcast(Intent(context, PomodoroDismissReceiver::class.java).apply {
            action = PomodoroDismissReceiver.ACTION_DISMISS
        })
        context.sendBroadcast(Intent(context, PomodoroAlarmReceiver::class.java).apply {
            action = PomodoroAlarmReceiver.ACTION_POMODORO_COMPLETE
        })
        delay(1_500L)

        assertStopped()
        assertEquals(PomodoroMode.FOCUS, preferences.getPomodoroRuntime().mode)
    }

    private suspend fun assertStopped() {
        assertEquals(PomodoroPhase.IDLE, preferences.getPomodoroRuntime().phase)
        assertFalse(repository.uiState.value.isRunning)
        assertFalse(repository.uiState.value.awaitingDismiss)
        assertFalse(notifications.activeNotifications.any {
            it.id == PomodoroCompletionAlerter.NOTIFICATION_ID ||
                    it.id == PomodoroActiveNotifier.NOTIFICATION_ID
        })
    }

    private fun alarmIntent(): PendingIntent? = PendingIntent.getBroadcast(
            context,
            4217,
            Intent(context, PomodoroAlarmReceiver::class.java).apply {
                action = PomodoroAlarmReceiver.ACTION_POMODORO_COMPLETE
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    private suspend fun await(description: String, condition: () -> Boolean) {
        try {
            withTimeout(10_000L) {
                while (!condition()) delay(25L)
            }
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("Timed out waiting for $description: ${repository.uiState.value}; stored=${preferences.getPomodoroRuntime()}; enabled=${preferences.showHomePomodoroFlow.first()}", error)
        }
    }
}
