package com.lu4p.fokuslauncher.pomodoro

import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.MAX_POMODORO_MINUTES
import com.lu4p.fokuslauncher.data.model.MIN_POMODORO_MINUTES
import com.lu4p.fokuslauncher.data.model.PomodoroConfig
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import com.lu4p.fokuslauncher.data.model.PomodoroRuntimeState
import com.lu4p.fokuslauncher.data.model.formatPomodoroMmSs
import com.lu4p.fokuslauncher.data.model.idleRuntimeFor
import com.lu4p.fokuslauncher.data.model.nextPomodoroMode
import com.lu4p.fokuslauncher.data.model.progressFraction
import com.lu4p.fokuslauncher.data.model.remainingMsAt
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PomodoroUiState(
        val enabled: Boolean = false,
        val mode: PomodoroMode = PomodoroMode.FOCUS,
        val phase: PomodoroPhase = PomodoroPhase.IDLE,
        val remainingText: String = "25:00",
        val progress: Float = 0f,
        val isRunning: Boolean = false,
        /** True while the alarm is sounding and the timer counts into negative overtime. */
        val awaitingDismiss: Boolean = false,
) {
    val showWidget: Boolean
        get() = enabled
}

/**
 * Owns Pomodoro session mutations, persistence, completion alarm scheduling, and a 1s UI ticker.
 */
@Singleton
class PomodoroRepository
@Inject
constructor(
        private val preferencesManager: PreferencesManager,
        private val alarmScheduler: PomodoroAlarmScheduler,
        private val completionAlerter: PomodoroCompletionAlerter,
        private val activeNotifier: PomodoroActiveNotifier,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var latestConfig: PomodoroConfig = PomodoroConfig()
    private var latestRuntime: PomodoroRuntimeState = idleRuntimeFor(PomodoroConfig(), PomodoroMode.FOCUS)
    private var enabled: Boolean = false
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(
                            preferencesManager.showHomePomodoroFlow,
                            preferencesManager.pomodoroConfigFlow,
                            preferencesManager.pomodoroRuntimeFlow,
                    ) { show, config, runtime ->
                        Triple(show, config, runtime)
                    }
                    .distinctUntilChanged()
                    .collect { (show, config, runtime) ->
                        if (enabled && !show) {
                            completionAlerter.dismiss()
                            activeNotifier.cancel()
                        }
                        enabled = show
                        latestConfig = config
                        val reconciled = reconcileRuntime(runtime, now = System.currentTimeMillis())
                        if (reconciled != runtime) {
                            preferencesManager.setPomodoroRuntime(reconciled)
                            return@collect
                        }
                        latestRuntime = reconciled
                        if (reconciled.phase == PomodoroPhase.OVERTIME) {
                            completionAlerter.ensureAlerting(
                                    reconciled.mode,
                                    config.alarmSoundUri,
                            )
                        }
                        publishUi()
                        syncTickerAndAlarm()
                        syncActiveNotification()
                    }
        }
    }

    fun togglePlayPause() {
        scope.launch {
            mutex.withLock {
                when (latestRuntime.phase) {
                    PomodoroPhase.OVERTIME -> {
                        dismissOvertimeAndStartNextLocked()
                    }
                    PomodoroPhase.RUNNING -> {
                        completionAlerter.dismiss()
                        persist(pause(latestRuntime, System.currentTimeMillis()))
                    }
                    PomodoroPhase.PAUSED,
                    PomodoroPhase.IDLE -> {
                        completionAlerter.dismiss()
                        persist(startOrResume(latestRuntime, System.currentTimeMillis()))
                    }
                }
            }
        }
    }

    /** Stops the completion alarm and starts the next phase (Break after Fokus, etc.). */
    fun dismissOvertimeAndStartNext() {
        scope.launch { mutex.withLock { dismissOvertimeAndStartNextLocked() } }
    }

    fun selectMode(mode: PomodoroMode) {
        scope.launch {
            mutex.withLock {
                completionAlerter.dismiss()
                if (latestRuntime.mode == mode && latestRuntime.phase == PomodoroPhase.IDLE) {
                    return@withLock
                }
                alarmScheduler.cancel()
                persist(idleRuntimeFor(latestConfig, mode))
            }
        }
    }

    fun adjustMinutes(deltaMinutes: Int) {
        if (deltaMinutes == 0) return
        scope.launch {
            mutex.withLock {
                if (latestRuntime.phase == PomodoroPhase.OVERTIME) return@withLock
                completionAlerter.dismiss()
                val now = System.currentTimeMillis()
                val remaining = remainingMsAt(latestRuntime, now).coerceAtLeast(0L)
                val adjustedRemaining =
                        (remaining + TimeUnit.MINUTES.toMillis(deltaMinutes.toLong())).coerceIn(
                                TimeUnit.MINUTES.toMillis(MIN_POMODORO_MINUTES.toLong()),
                                TimeUnit.MINUTES.toMillis(MAX_POMODORO_MINUTES.toLong()),
                        )
                val deltaMs = adjustedRemaining - remaining
                val next =
                        when (latestRuntime.phase) {
                            PomodoroPhase.RUNNING -> {
                                val endsAt = latestRuntime.endsAtEpochMs + deltaMs
                                latestRuntime.copy(
                                        durationMs =
                                                (latestRuntime.durationMs + deltaMs).coerceAtLeast(
                                                        TimeUnit.MINUTES.toMillis(
                                                                MIN_POMODORO_MINUTES.toLong(),
                                                        ),
                                                ),
                                        endsAtEpochMs = endsAt,
                                )
                            }
                            PomodoroPhase.PAUSED ->
                                    latestRuntime.copy(
                                            durationMs =
                                                    (latestRuntime.durationMs + deltaMs)
                                                            .coerceAtLeast(
                                                                    TimeUnit.MINUTES.toMillis(
                                                                            MIN_POMODORO_MINUTES
                                                                                    .toLong(),
                                                                    ),
                                                            ),
                                            remainingMs = adjustedRemaining,
                                    )
                            PomodoroPhase.IDLE ->
                                    latestRuntime.copy(
                                            durationMs = adjustedRemaining,
                                            remainingMs = adjustedRemaining,
                                    )
                            PomodoroPhase.OVERTIME -> latestRuntime
                        }
                persist(next)
            }
        }
    }

    private suspend fun dismissOvertimeAndStartNextLocked() {
        if (latestRuntime.phase != PomodoroPhase.OVERTIME) {
            completionAlerter.dismiss()
            return
        }
        val completedMode = latestRuntime.mode
        completionAlerter.dismiss()
        alarmScheduler.cancel()
        val nextMode = nextPomodoroMode(completedMode)
        val idle = idleRuntimeFor(latestConfig, nextMode)
        persist(startOrResume(idle, System.currentTimeMillis()))
    }

    private suspend fun persist(state: PomodoroRuntimeState) {
        latestRuntime = state
        preferencesManager.setPomodoroRuntime(state)
        publishUi()
        syncTickerAndAlarm()
        syncActiveNotification()
    }

    /**
     * If a RUNNING session already elapsed, enter overtime (negative countdown) and alert.
     * OVERTIME is left as-is so dismiss can start the next phase.
     */
    private fun reconcileRuntime(
            runtime: PomodoroRuntimeState,
            now: Long,
    ): PomodoroRuntimeState {
        if (runtime.phase != PomodoroPhase.RUNNING) return runtime
        if (runtime.endsAtEpochMs > now) return runtime
        alarmScheduler.cancel()
        completionAlerter.announceComplete(runtime.mode, latestConfig.alarmSoundUri)
        return runtime.copy(phase = PomodoroPhase.OVERTIME)
    }

    private fun enterOvertime(state: PomodoroRuntimeState): PomodoroRuntimeState {
        alarmScheduler.cancel()
        completionAlerter.announceComplete(state.mode, latestConfig.alarmSoundUri)
        return state.copy(phase = PomodoroPhase.OVERTIME)
    }

    private fun startOrResume(state: PomodoroRuntimeState, now: Long): PomodoroRuntimeState {
        val remaining =
                when (state.phase) {
                    PomodoroPhase.PAUSED -> state.remainingMs.coerceAtLeast(0L)
                    else -> state.durationMs.coerceAtLeast(0L)
                }.coerceAtLeast(TimeUnit.SECONDS.toMillis(1))
        return state.copy(
                phase = PomodoroPhase.RUNNING,
                endsAtEpochMs = now + remaining,
                remainingMs = remaining,
                durationMs = state.durationMs.coerceAtLeast(remaining),
        )
    }

    private fun pause(state: PomodoroRuntimeState, now: Long): PomodoroRuntimeState {
        val remaining = remainingMsAt(state, now).coerceAtLeast(0L)
        return state.copy(
                phase = PomodoroPhase.PAUSED,
                remainingMs = remaining,
                endsAtEpochMs = 0L,
        )
    }

    private fun syncTickerAndAlarm() {
        val phase = latestRuntime.phase
        val needsTicker =
                enabled && (phase == PomodoroPhase.RUNNING || phase == PomodoroPhase.OVERTIME)
        if (!needsTicker) {
            tickerJob?.cancel()
            tickerJob = null
            if (phase != PomodoroPhase.RUNNING) {
                alarmScheduler.cancel()
            }
            return
        }
        if (phase == PomodoroPhase.RUNNING) {
            alarmScheduler.schedule(latestRuntime.endsAtEpochMs)
        } else {
            alarmScheduler.cancel()
        }
        if (tickerJob?.isActive == true) return
        tickerJob =
                scope.launch {
                    while (true) {
                        delay(TICK_MS)
                        mutex.withLock {
                            if (!enabled) {
                                return@launch
                            }
                            when (latestRuntime.phase) {
                                PomodoroPhase.RUNNING -> {
                                    val now = System.currentTimeMillis()
                                    if (latestRuntime.endsAtEpochMs <= now) {
                                        persist(enterOvertime(latestRuntime))
                                        return@withLock
                                    }
                                    publishUi(now)
                                }
                                PomodoroPhase.OVERTIME -> publishUi()
                                else -> return@launch
                            }
                        }
                    }
                }
    }

    private fun syncActiveNotification() {
        if (enabled && latestRuntime.phase == PomodoroPhase.RUNNING) {
            activeNotifier.showRunning(latestRuntime.mode, latestRuntime.endsAtEpochMs)
        } else {
            activeNotifier.cancel()
        }
    }

    private fun publishUi(now: Long = System.currentTimeMillis()) {
        val runtime = latestRuntime
        val remaining = remainingMsAt(runtime, now)
        _uiState.value =
                PomodoroUiState(
                        enabled = enabled,
                        mode = runtime.mode,
                        phase = runtime.phase,
                        remainingText = formatPomodoroMmSs(remaining),
                        progress = progressFraction(runtime, now),
                        isRunning = runtime.phase == PomodoroPhase.RUNNING,
                        awaitingDismiss = runtime.phase == PomodoroPhase.OVERTIME,
                )
    }

    companion object {
        private const val TICK_MS = 1_000L
    }
}
