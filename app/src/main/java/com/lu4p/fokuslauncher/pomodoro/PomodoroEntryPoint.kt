package com.lu4p.fokuslauncher.pomodoro

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PomodoroEntryPoint {
    fun pomodoroCompletionAlerter(): PomodoroCompletionAlerter

    fun pomodoroRepository(): PomodoroRepository
}
