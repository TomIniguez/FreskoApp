package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.repository.SettingsRepository

class SetWarningDaysUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(days: Int) {
        require(days in MIN_WARNING_DAYS..MAX_WARNING_DAYS)
        repository.setWarningDays(days)
    }
}
