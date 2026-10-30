package com.example.freskoapp.domain

import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.usecase.DEFAULT_WARNING_DAYS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    warningDays: Int = DEFAULT_WARNING_DAYS,
    promptDismissed: Boolean = false,
) : SettingsRepository {
    val warningDaysState = MutableStateFlow(warningDays)
    val promptDismissedState = MutableStateFlow(promptDismissed)

    override val warningDays: Flow<Int> = warningDaysState
    override val notificationPromptDismissed: Flow<Boolean> = promptDismissedState

    override suspend fun setWarningDays(days: Int) {
        warningDaysState.value = days
    }

    override suspend fun dismissNotificationPrompt() {
        promptDismissedState.value = true
    }
}
