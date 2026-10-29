package com.lu4p.fokuslauncher.pomodoro

import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.PomodoroConfig
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import com.lu4p.fokuslauncher.data.model.idleRuntimeFor
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val config = PomodoroConfig()
    private val enabled = MutableStateFlow(true)
    private val runtime = MutableStateFlow(
            idleRuntimeFor(config, PomodoroMode.FOCUS).copy(
                    phase = PomodoroPhase.RUNNING,
                    endsAtEpochMs = System.currentTimeMillis() + 60_000L,
            ),
    )
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val scheduler = mockk<PomodoroAlarmScheduler>(relaxed = true)
    private val alerter = mockk<PomodoroCompletionAlerter>(relaxed = true)
    private val notifier = mockk<PomodoroActiveNotifier>(relaxed = true)

    private fun repository(): PomodoroRepository {
        every { preferences.showHomePomodoroFlow } returns enabled
        every { preferences.pomodoroConfigFlow } returns MutableStateFlow(config)
        every { preferences.pomodoroRuntimeFlow } returns runtime
        coEvery { preferences.setPomodoroRuntime(any()) } answers { runtime.value = firstArg() }
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Default } returns dispatcher
        return PomodoroRepository(preferences, scheduler, alerter, notifier).also { it.start() }
    }

    @After
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
    }

    @Test
    fun `disabled startup discards an elapsed session without alerting`() = runTest(dispatcher) {
        enabled.value = false
        runtime.value = runtime.value.copy(endsAtEpochMs = 1L)
        val repository = repository()
        testScheduler.runCurrent()

        assertFalse(repository.uiState.value.isRunning)
        assertFalse(repository.uiState.value.awaitingDismiss)
        assertEquals(PomodoroPhase.IDLE, runtime.value.phase)
        verify { scheduler.cancel() }
        verify(exactly = 0) { alerter.announceComplete(any(), any()) }
        verify(exactly = 0) { alerter.ensureAlerting(any(), any()) }
    }

    @Test
    fun `disabled controls cannot restart the timer`() = runTest(dispatcher) {
        enabled.value = false
        val repository = repository()
        testScheduler.runCurrent()
        clearMocks(scheduler, alerter, notifier)

        repository.togglePlayPause()
        repository.dismissOvertimeAndStartNext()
        repository.adjustMinutes(5)
        repository.selectMode(PomodoroMode.BREAK)
        testScheduler.runCurrent()

        assertEquals(idleRuntimeFor(config, PomodoroMode.FOCUS), runtime.value)
        verify(exactly = 0) { scheduler.schedule(any()) }
        verify(exactly = 0) { notifier.showRunning(any(), any()) }
    }

    @Test
    fun `disabling overtime stays silent when reenabled`() = runTest(dispatcher) {
        runtime.value = runtime.value.copy(phase = PomodoroPhase.OVERTIME, endsAtEpochMs = 1L)
        val repository = repository()
        testScheduler.runCurrent()
        verify { alerter.ensureAlerting(any(), any()) }
        clearMocks(scheduler, alerter, notifier)

        enabled.value = false
        testScheduler.runCurrent()
        enabled.value = true
        testScheduler.runCurrent()

        try {
            assertEquals(PomodoroPhase.IDLE, repository.uiState.value.phase)
            verify { alerter.dismiss() }
            verify(exactly = 0) { alerter.announceComplete(any(), any()) }
            verify(exactly = 0) { alerter.ensureAlerting(any(), any()) }
            verify(exactly = 0) { scheduler.schedule(any()) }
        } finally {
            enabled.value = false
            testScheduler.runCurrent()
        }
    }

    @Test
    fun `disabling a running timer cancels its scheduled completion`() = runTest(dispatcher) {
        val repository = repository()
        testScheduler.runCurrent()
        verify { scheduler.schedule(runtime.value.endsAtEpochMs) }
        clearMocks(scheduler, alerter, notifier)

        enabled.value = false
        testScheduler.runCurrent()

        assertFalse(repository.uiState.value.enabled)
        verify { scheduler.cancel() }
        verify { alerter.dismiss() }
        verify { notifier.cancel() }
        verify(exactly = 0) { scheduler.schedule(any()) }
    }
}
