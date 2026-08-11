package com.lu4p.fokuslauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PomodoroModelsTest {

    @Test
    fun `serialize and parse config round trip`() {
        val config =
                PomodoroConfig(
                        focusMinutes = 30,
                        breakMinutes = 10,
                        alarmSoundUri = "content://media/internal/audio/media/42",
                )
        val parsed = parsePomodoroConfig(serializePomodoroConfig(config))
        assertEquals(30, parsed.focusMinutes)
        assertEquals(10, parsed.breakMinutes)
        assertEquals("content://media/internal/audio/media/42", parsed.alarmSoundUri)
    }

    @Test
    fun `parse blank config uses defaults`() {
        val parsed = parsePomodoroConfig("")
        assertEquals(DEFAULT_FOCUS_MINUTES, parsed.focusMinutes)
        assertEquals(DEFAULT_BREAK_MINUTES, parsed.breakMinutes)
    }

    @Test
    fun `normalize clamps minutes`() {
        val parsed =
                normalizePomodoroConfig(PomodoroConfig(focusMinutes = 999, breakMinutes = 0))
        assertEquals(MAX_POMODORO_MINUTES, parsed.focusMinutes)
        assertEquals(MIN_POMODORO_MINUTES, parsed.breakMinutes)
    }

    @Test
    fun `serialize and parse runtime round trip`() {
        val state =
                PomodoroRuntimeState(
                        mode = PomodoroMode.BREAK,
                        phase = PomodoroPhase.RUNNING,
                        durationMs = TimeUnit.MINUTES.toMillis(5),
                        endsAtEpochMs = 1_700_000_000_000L,
                        remainingMs = TimeUnit.MINUTES.toMillis(3),
                )
        val parsed = parsePomodoroRuntime(serializePomodoroRuntime(state))
        assertEquals(PomodoroMode.BREAK, parsed.mode)
        assertEquals(PomodoroPhase.RUNNING, parsed.phase)
        assertEquals(state.durationMs, parsed.durationMs)
        assertEquals(state.endsAtEpochMs, parsed.endsAtEpochMs)
        assertEquals(state.remainingMs, parsed.remainingMs)
    }

    @Test
    fun `remainingMsAt uses endsAt when running`() {
        val now = 1_000_000L
        val state =
                PomodoroRuntimeState(
                        phase = PomodoroPhase.RUNNING,
                        durationMs = 60_000L,
                        endsAtEpochMs = now + 12_000L,
                )
        assertEquals(12_000L, remainingMsAt(state, now))
        assertEquals(0L, remainingMsAt(state, now + 20_000L))
    }

    @Test
    fun `remainingMsAt goes negative in overtime`() {
        val now = 1_000_000L
        val state =
                PomodoroRuntimeState(
                        phase = PomodoroPhase.OVERTIME,
                        durationMs = 60_000L,
                        endsAtEpochMs = now - 5_000L,
                )
        assertEquals(-5_000L, remainingMsAt(state, now))
    }

    @Test
    fun `formatPomodoroMmSs formats minutes and seconds`() {
        assertEquals("25:00", formatPomodoroMmSs(TimeUnit.MINUTES.toMillis(25)))
        assertEquals("1:05", formatPomodoroMmSs(65_000L))
        assertEquals("0:00", formatPomodoroMmSs(0L))
        assertEquals("-0:05", formatPomodoroMmSs(-5_000L))
        assertEquals("-1:05", formatPomodoroMmSs(-65_000L))
    }

    @Test
    fun `nextPomodoroMode alternates fokus and break`() {
        assertEquals(PomodoroMode.BREAK, nextPomodoroMode(PomodoroMode.FOCUS))
        assertEquals(PomodoroMode.FOCUS, nextPomodoroMode(PomodoroMode.BREAK))
    }

    @Test
    fun `progressFraction grows as time elapses`() {
        val now = 0L
        val state =
                PomodoroRuntimeState(
                        phase = PomodoroPhase.RUNNING,
                        durationMs = 100_000L,
                        endsAtEpochMs = 100_000L,
                )
        assertEquals(0f, progressFraction(state, now), 0.001f)
        assertTrue(progressFraction(state, 50_000L) in 0.49f..0.51f)
        assertEquals(1f, progressFraction(state, 100_000L), 0.001f)
    }

    @Test
    fun `idleRuntimeFor uses config duration for mode`() {
        val config = PomodoroConfig(focusMinutes = 20, breakMinutes = 7)
        val focus = idleRuntimeFor(config, PomodoroMode.FOCUS)
        val brk = idleRuntimeFor(config, PomodoroMode.BREAK)
        assertEquals(TimeUnit.MINUTES.toMillis(20), focus.durationMs)
        assertEquals(PomodoroPhase.IDLE, focus.phase)
        assertEquals(TimeUnit.MINUTES.toMillis(7), brk.durationMs)
        assertEquals(PomodoroMode.BREAK, brk.mode)
    }
}
