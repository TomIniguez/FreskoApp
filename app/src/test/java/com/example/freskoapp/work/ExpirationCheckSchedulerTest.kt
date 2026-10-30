package com.example.freskoapp.work

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ExpirationCheckSchedulerTest {
    private fun delayAt(hour: Int, minute: Int) =
        ExpirationCheckScheduler.initialDelay(LocalDateTime.of(2026, 10, 30, hour, minute))

    @Test
    fun beforeNotificationTimeWaitsUntilToday() {
        assertEquals(Duration.ofMinutes(90), delayAt(7, 30))
    }

    @Test
    fun exactlyAtNotificationTimeIsZero() {
        assertEquals(Duration.ZERO, delayAt(9, 0))
    }

    @Test
    fun afterNotificationTimeWaitsUntilTomorrow() {
        assertEquals(Duration.ofHours(12), delayAt(21, 0))
        assertEquals(Duration.ofHours(9).plusMinutes(1), delayAt(23, 59))
    }
}
