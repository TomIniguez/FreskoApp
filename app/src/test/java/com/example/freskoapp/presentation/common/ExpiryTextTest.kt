package com.example.freskoapp.presentation.common

import com.example.freskoapp.domain.model.ExpirationStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpiryTextTest {
    private val today = LocalDate.of(2026, 9, 30)

    private fun phrase(status: ExpirationStatus, days: Long) = expiryPhrase(status, today.plusDays(days), today)

    @Test
    fun expiredCountsDaysAgoAndNeverZero() {
        assertEquals(ExpiryPhrase.ExpiredDaysAgo(1), phrase(ExpirationStatus.EXPIRED, -1))
        assertEquals(ExpiryPhrase.ExpiredDaysAgo(10), phrase(ExpirationStatus.EXPIRED, -10))
    }

    @Test
    fun expiringSoonSaysTodayTomorrowOrInDays() {
        assertEquals(ExpiryPhrase.Today, phrase(ExpirationStatus.EXPIRING_SOON, 0))
        assertEquals(ExpiryPhrase.Tomorrow, phrase(ExpirationStatus.EXPIRING_SOON, 1))
        assertEquals(ExpiryPhrase.InDays(3), phrase(ExpirationStatus.EXPIRING_SOON, 3))
    }

    @Test
    fun freshUsesDaysThenMonthsThenYears() {
        assertEquals(ExpiryPhrase.FreshDays(12), phrase(ExpirationStatus.FRESH, 12))
        assertEquals(ExpiryPhrase.FreshMonths(1), expiryPhrase(ExpirationStatus.FRESH, LocalDate.of(2026, 10, 30), today))
        assertEquals(ExpiryPhrase.FreshMonths(5), expiryPhrase(ExpirationStatus.FRESH, LocalDate.of(2027, 3, 15), today))
        assertEquals(ExpiryPhrase.FreshYears(1), expiryPhrase(ExpirationStatus.FRESH, LocalDate.of(2027, 9, 30), today))
    }

    @Test
    fun shortDateAddsTheYearOnlyOutsideTheCurrentOne() {
        assertEquals("29 sep", LocalDate.of(2026, 9, 29).shortDate(today))
        assertEquals("15 mar 2027", LocalDate.of(2027, 3, 15).shortDate(today))
        assertEquals("27 sep 2026", LocalDate.of(2026, 9, 27).shortDate(today, withYear = true))
    }
}
