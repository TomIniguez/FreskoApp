package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ExpirationStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val DEFAULT_WARNING_DAYS = 3
const val MIN_WARNING_DAYS = 1
const val MAX_WARNING_DAYS = 7

// Pure rule: expired if the date is before today; expiring soon from today up to
// `warningDays` days ahead (inclusive); fresh afterwards. In RF03 warningDays comes from settings.
class CalculateExpirationStatusUseCase {
    operator fun invoke(
        expirationDate: LocalDate,
        today: LocalDate,
        warningDays: Int = DEFAULT_WARNING_DAYS,
    ): ExpirationStatus {
        val daysLeft = ChronoUnit.DAYS.between(today, expirationDate)
        return when {
            daysLeft < 0 -> ExpirationStatus.EXPIRED
            daysLeft <= warningDays -> ExpirationStatus.EXPIRING_SOON
            else -> ExpirationStatus.FRESH
        }
    }
}
