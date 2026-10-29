package com.lu4p.fokuslauncher.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lu4p.fokuslauncher.data.model.PomodoroConfig
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import com.lu4p.fokuslauncher.data.model.idleRuntimeFor
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PomodoroPreferencesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences by lazy {
        PreferencesManager(
                RuntimeEnvironment.getApplication(),
                mockk<NoteDocuments>(relaxed = true),
                PreferenceDataStoreFactory.create(scope = scope) {
                    temporaryFolder.newFile("pomodoro.preferences_pb")
                },
        )
    }

    @After
    fun tearDown() { scope.cancel() }

    @Test
    fun `disabling persists an idle timer without a running repository`() = runBlocking {
        val config = PomodoroConfig(focusMinutes = 45, breakMinutes = 10)
        preferences.setPomodoroConfig(config)
        preferences.setShowHomePomodoro(true)
        preferences.setPomodoroRuntime(
                idleRuntimeFor(config, PomodoroMode.BREAK).copy(
                        phase = PomodoroPhase.RUNNING,
                        endsAtEpochMs = System.currentTimeMillis() + 60_000L,
                ),
        )

        preferences.setShowHomePomodoro(false)

        assertFalse(preferences.showHomePomodoroFlow.first())
        assertEquals(idleRuntimeFor(config, PomodoroMode.BREAK), preferences.getPomodoroRuntime())
        preferences.setShowHomePomodoro(true)
        assertEquals(PomodoroPhase.IDLE, preferences.getPomodoroRuntime().phase)
    }

    @Test
    fun `late timer writes cannot resurrect a disabled session`() = runBlocking {
        preferences.setShowHomePomodoro(true)
        val running = idleRuntimeFor(PomodoroConfig(), PomodoroMode.FOCUS).copy(
                phase = PomodoroPhase.RUNNING,
                endsAtEpochMs = 1L,
        )
        preferences.setPomodoroRuntime(running)
        preferences.setShowHomePomodoro(false)

        preferences.setPomodoroRuntime(running.copy(phase = PomodoroPhase.OVERTIME))

        assertEquals(PomodoroPhase.IDLE, preferences.getPomodoroRuntime().phase)
    }

    @Test
    fun `enabling media stops an overtime timer without losing defaults`() = runBlocking {
        val config = PomodoroConfig(focusMinutes = 45, alarmSoundUri = "content://alarm/custom")
        preferences.setPomodoroConfig(config)
        preferences.setShowHomePomodoro(true)
        preferences.setPomodoroRuntime(
                idleRuntimeFor(config, PomodoroMode.FOCUS).copy(
                        phase = PomodoroPhase.OVERTIME,
                        endsAtEpochMs = 1L,
                ),
        )

        preferences.setShowHomeMedia(true)

        assertTrue(preferences.showHomeMediaFlow.first())
        assertFalse(preferences.showHomePomodoroFlow.first())
        assertEquals(idleRuntimeFor(config, PomodoroMode.FOCUS), preferences.getPomodoroRuntime())
        assertEquals(config, preferences.getPomodoroConfig())
    }
}
