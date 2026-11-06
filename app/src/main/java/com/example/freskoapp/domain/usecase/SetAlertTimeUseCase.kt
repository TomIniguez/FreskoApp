package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.repository.SettingsRepository
import java.time.LocalTime

// Saves the daily alert time and moves the pending check to it. `reschedule` is the platform side (WorkManager).
class SetAlertTimeUseCase(
    private val repository: SettingsRepository,
    private val reschedule: (LocalTime) -> Unit,
) {
    suspend operator fun invoke(time: LocalTime) {
        val minutes = time.withSecond(0).withNano(0) // the setting has minute precision
        repository.setAlertTime(minutes)
        reschedule(minutes)
    }
}
