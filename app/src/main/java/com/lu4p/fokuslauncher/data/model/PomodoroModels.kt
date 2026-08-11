package com.lu4p.fokuslauncher.data.model

import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class PomodoroMode {
    FOCUS,
    BREAK,
}

enum class PomodoroPhase {
    IDLE,
    RUNNING,
    PAUSED,
    /**
     * Session reached zero: alarm is sounding and the display counts into negative overtime until
     * the user dismisses it.
     */
    OVERTIME,
}

/**
 * User-configured Pomodoro defaults (Configure widgets → Pomodoro).
 *
 * [alarmSoundUri] is empty for the system default alarm sound; otherwise a ringtone content URI.
 */
data class PomodoroConfig(
        val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
        val breakMinutes: Int = DEFAULT_BREAK_MINUTES,
        val alarmSoundUri: String = "",
) {
    fun minutesFor(mode: PomodoroMode): Int =
            when (mode) {
                PomodoroMode.FOCUS -> focusMinutes
                PomodoroMode.BREAK -> breakMinutes
            }

    fun durationMsFor(mode: PomodoroMode): Long =
            TimeUnit.MINUTES.toMillis(minutesFor(mode).toLong())
}

/**
 * Persisted session state so a running timer survives process death.
 *
 * When [phase] is [PomodoroPhase.RUNNING] or [PomodoroPhase.OVERTIME], [endsAtEpochMs] is the
 * absolute completion instant (overtime remaining is negative once past that instant).
 * When [phase] is [PomodoroPhase.PAUSED], [remainingMs] holds time left.
 * [durationMs] is the session length used for progress (after −/+ adjustments).
 */
data class PomodoroRuntimeState(
        val mode: PomodoroMode = PomodoroMode.FOCUS,
        val phase: PomodoroPhase = PomodoroPhase.IDLE,
        val durationMs: Long = TimeUnit.MINUTES.toMillis(DEFAULT_FOCUS_MINUTES.toLong()),
        val endsAtEpochMs: Long = 0L,
        val remainingMs: Long = 0L,
)

const val DEFAULT_FOCUS_MINUTES = 25
const val DEFAULT_BREAK_MINUTES = 5
const val MIN_POMODORO_MINUTES = 1
const val MAX_POMODORO_MINUTES = 120

val POMODORO_FOCUS_MINUTE_OPTIONS = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
val POMODORO_BREAK_MINUTE_OPTIONS = listOf(1, 3, 5, 10, 15, 20, 30)

fun clampPomodoroMinutes(minutes: Int): Int = minutes.coerceIn(MIN_POMODORO_MINUTES, MAX_POMODORO_MINUTES)

fun normalizePomodoroConfig(config: PomodoroConfig): PomodoroConfig =
        PomodoroConfig(
                focusMinutes = clampPomodoroMinutes(config.focusMinutes),
                breakMinutes = clampPomodoroMinutes(config.breakMinutes),
                alarmSoundUri = config.alarmSoundUri.trim(),
        )

fun serializePomodoroConfig(config: PomodoroConfig): String {
    val normalized = normalizePomodoroConfig(config)
    return JSONObject()
            .put("focusMinutes", normalized.focusMinutes)
            .put("breakMinutes", normalized.breakMinutes)
            .put("alarmSoundUri", normalized.alarmSoundUri)
            .toString()
}

fun parsePomodoroConfig(raw: String): PomodoroConfig {
    if (raw.isBlank()) return PomodoroConfig()
    return try {
        val obj = JSONObject(raw)
        normalizePomodoroConfig(
                PomodoroConfig(
                        focusMinutes =
                                obj.optInt("focusMinutes", DEFAULT_FOCUS_MINUTES).takeIf { it > 0 }
                                        ?: DEFAULT_FOCUS_MINUTES,
                        breakMinutes =
                                obj.optInt("breakMinutes", DEFAULT_BREAK_MINUTES).takeIf { it > 0 }
                                        ?: DEFAULT_BREAK_MINUTES,
                        alarmSoundUri = obj.optString("alarmSoundUri", "").trim(),
                ),
        )
    } catch (_: Exception) {
        PomodoroConfig()
    }
}

fun serializePomodoroRuntime(state: PomodoroRuntimeState): String =
        JSONObject()
                .put("mode", state.mode.name)
                .put("phase", state.phase.name)
                .put("durationMs", state.durationMs.coerceAtLeast(0L))
                .put("endsAtEpochMs", state.endsAtEpochMs.coerceAtLeast(0L))
                .put("remainingMs", state.remainingMs.coerceAtLeast(0L))
                .toString()

fun parsePomodoroRuntime(raw: String, config: PomodoroConfig = PomodoroConfig()): PomodoroRuntimeState {
    if (raw.isBlank()) {
        return idleRuntimeFor(config, PomodoroMode.FOCUS)
    }
    return try {
        val obj = JSONObject(raw)
        val mode =
                runCatching { PomodoroMode.valueOf(obj.optString("mode", PomodoroMode.FOCUS.name)) }
                        .getOrDefault(PomodoroMode.FOCUS)
        val phase =
                runCatching {
                            PomodoroPhase.valueOf(obj.optString("phase", PomodoroPhase.IDLE.name))
                        }
                        .getOrDefault(PomodoroPhase.IDLE)
        val durationMs =
                obj.optLong("durationMs", config.durationMsFor(mode)).coerceAtLeast(
                        TimeUnit.MINUTES.toMillis(MIN_POMODORO_MINUTES.toLong()),
                )
        PomodoroRuntimeState(
                mode = mode,
                phase = phase,
                durationMs = durationMs,
                endsAtEpochMs = obj.optLong("endsAtEpochMs", 0L).coerceAtLeast(0L),
                remainingMs = obj.optLong("remainingMs", 0L).coerceAtLeast(0L),
        )
    } catch (_: Exception) {
        idleRuntimeFor(config, PomodoroMode.FOCUS)
    }
}

fun idleRuntimeFor(config: PomodoroConfig, mode: PomodoroMode): PomodoroRuntimeState {
    val durationMs = config.durationMsFor(mode)
    return PomodoroRuntimeState(
            mode = mode,
            phase = PomodoroPhase.IDLE,
            durationMs = durationMs,
            endsAtEpochMs = 0L,
            remainingMs = durationMs,
    )
}

/** Remaining millis for display / completion checks at [nowEpochMs]. May be negative in overtime. */
fun remainingMsAt(state: PomodoroRuntimeState, nowEpochMs: Long): Long =
        when (state.phase) {
            PomodoroPhase.RUNNING -> (state.endsAtEpochMs - nowEpochMs).coerceAtLeast(0L)
            PomodoroPhase.OVERTIME -> state.endsAtEpochMs - nowEpochMs
            PomodoroPhase.PAUSED -> state.remainingMs.coerceAtLeast(0L)
            PomodoroPhase.IDLE -> state.durationMs.coerceAtLeast(0L)
        }

fun progressFraction(state: PomodoroRuntimeState, nowEpochMs: Long): Float {
    if (state.phase == PomodoroPhase.OVERTIME) return 1f
    val total = state.durationMs.coerceAtLeast(1L).toFloat()
    val remaining = remainingMsAt(state, nowEpochMs).coerceAtLeast(0L).toFloat()
    return (1f - (remaining / total)).coerceIn(0f, 1f)
}

fun formatPomodoroMmSs(remainingMs: Long): String {
    val negative = remainingMs < 0L
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(kotlin.math.abs(remainingMs))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val body = "%d:%02d".format(minutes, seconds)
    return if (negative) "-$body" else body
}

/** After dismissing a completed [mode] session, start the opposite phase. */
fun nextPomodoroMode(mode: PomodoroMode): PomodoroMode =
        when (mode) {
            PomodoroMode.FOCUS -> PomodoroMode.BREAK
            PomodoroMode.BREAK -> PomodoroMode.FOCUS
        }
