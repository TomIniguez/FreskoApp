package com.example.freskoapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val warningDays: Flow<Int> // always within MIN_WARNING_DAYS..MAX_WARNING_DAYS
    val notificationPromptDismissed: Flow<Boolean>
    suspend fun setWarningDays(days: Int)
    suspend fun dismissNotificationPrompt()
}
