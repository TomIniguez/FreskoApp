package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ExpirationStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalculateExpirationStatusUseCaseTest {
    private val calculate = CalculateExpirationStatusUseCase()
    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun yesterdayIsExpired() = assertEquals(ExpirationStatus.EXPIRED, calculate(today.minusDays(1), today))

    @Test
    fun todayIsExpiringSoon() = assertEquals(ExpirationStatus.EXPIRING_SOON, calculate(today, today))

    @Test
    fun warningLimitIsInclusive() = assertEquals(ExpirationStatus.EXPIRING_SOON, calculate(today.plusDays(3), today))

    @Test
    fun afterWarningWindowIsFresh() = assertEquals(ExpirationStatus.FRESH, calculate(today.plusDays(4), today))

    @Test
    fun customWarningDaysAreRespected() =
        assertEquals(ExpirationStatus.EXPIRING_SOON, calculate(today.plusDays(7), today, warningDays = 7))
}
