package com.lu4p.fokuslauncher.pomodoro

import android.content.BroadcastReceiver
import android.content.Intent
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.PomodoroConfig
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import com.lu4p.fokuslauncher.data.model.idleRuntimeFor
import dagger.hilt.android.EntryPointAccessors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroAlarmReceiverTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = RuntimeEnvironment.getApplication()
    private val alerter = mockk<PomodoroCompletionAlerter>(relaxed = true)
    private val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
    private val receiver = spyk(PomodoroAlarmReceiver())
    private val runtime = idleRuntimeFor(PomodoroConfig(), PomodoroMode.FOCUS).copy(
            phase = PomodoroPhase.RUNNING,
            endsAtEpochMs = 1L,
    )

    private fun deliver(enabled: Boolean, disableDuringCompletion: Boolean = false) {
        mockkConstructor(PreferencesManager::class, PomodoroAlarmScheduler::class)
        mockkStatic(EntryPointAccessors::class)
        val entryPoint = mockk<PomodoroEntryPoint>()
        every { entryPoint.pomodoroCompletionAlerter() } returns alerter
        every { EntryPointAccessors.fromApplication(context, PomodoroEntryPoint::class.java) } returns entryPoint
        val enabledFlow = MutableStateFlow(enabled)
        every { anyConstructed<PreferencesManager>().showHomePomodoroFlow } returns enabledFlow
        coEvery { anyConstructed<PreferencesManager>().getPomodoroRuntime() } returns runtime
        coEvery { anyConstructed<PreferencesManager>().getPomodoroConfig() } returns PomodoroConfig()
        coEvery { anyConstructed<PreferencesManager>().setPomodoroRuntime(any()) } answers {
            if (disableDuringCompletion) enabledFlow.value = false
        }
        every { anyConstructed<PomodoroAlarmScheduler>().cancel() } returns Unit
        every { receiver.goAsync() } returns pendingResult
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns dispatcher
        receiver.onReceive(context, Intent(PomodoroAlarmReceiver.ACTION_POMODORO_COMPLETE))
    }

    @After
    fun tearDown() { unmockkAll() }

    @Test
    fun `already delivered alarm is ignored while disabled`() = runTest(dispatcher) {
        deliver(enabled = false)
        testScheduler.runCurrent()
        unmockkStatic(Dispatchers::class)

        verify { pendingResult.finish() }
        verify(exactly = 0) { alerter.announceComplete(any(), any()) }
        coVerify(exactly = 0) { anyConstructed<PreferencesManager>().setPomodoroRuntime(any()) }
    }

    @Test
    fun `disabling during completion does not announce the old session`() = runTest(dispatcher) {
        deliver(enabled = true, disableDuringCompletion = true)
        testScheduler.runCurrent()
        unmockkStatic(Dispatchers::class)

        verify { pendingResult.finish() }
        verify(exactly = 0) { alerter.announceComplete(any(), any()) }
    }

    @Test
    fun `enabled elapsed session still announces completion`() = runTest(dispatcher) {
        deliver(enabled = true)
        testScheduler.runCurrent()
        unmockkStatic(Dispatchers::class)

        verify { pendingResult.finish() }
        verify { alerter.announceComplete(PomodoroMode.FOCUS, "") }
        coVerify { anyConstructed<PreferencesManager>().setPomodoroRuntime(runtime.copy(phase = PomodoroPhase.OVERTIME)) }
    }
}
