package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ExpirationStatus
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Days from today (in the clock's zone) to the expiration date, as recorded on pilot events; negative = expired.
fun daysToExpiry(expirationDate: LocalDate, clock: Clock): Int =
    ChronoUnit.DAYS.between(LocalDate.now(clock), expirationDate).toInt()

const val DEFAULT_WARNING_DAYS = 3
const val MIN_WARNING_DAYS = 1
const val MAX_WARNING_DAYS = 7

// Pure rule, the single source for list, filter and notification (warningDays comes from DataStore).
// Expiry day itself is EXPIRING_SOON, not EXPIRED: expired only from the next day.
// Expiring soon: from today up to `warningDays` days ahead (inclusive); fresh afterwards.
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
