package com.example.freskoapp.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

// Until the user picks another one in Ajustes.
val DEFAULT_ALERT_TIME: LocalTime = LocalTime.of(9, 0)

interface SettingsRepository {
    val warningDays: Flow<Int> // always within MIN_WARNING_DAYS..MAX_WARNING_DAYS
    val notificationPromptDismissed: Flow<Boolean>
    val alertTime: Flow<LocalTime> // time of the daily expiration alert, minute precision; DEFAULT_ALERT_TIME until set
    suspend fun setWarningDays(days: Int)
    suspend fun dismissNotificationPrompt()
    suspend fun setAlertTime(time: LocalTime)
}
