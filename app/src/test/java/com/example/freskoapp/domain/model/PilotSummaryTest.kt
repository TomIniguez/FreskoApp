package com.example.freskoapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PilotSummaryTest {
    private fun added(method: AddMethod, durationMs: Long?) =
        ProductEvent(type = EventType.PRODUCT_ADDED, method = method, durationMs = durationMs, timestamp = 0)

    private fun closed(type: EventType, units: Int, daysToExpiry: Int) =
        ProductEvent(productId = 1, type = type, units = units, daysToExpiry = daysToExpiry, timestamp = 0)

    @Test
    fun emptyLogGivesZerosAndNoMedian() {
        assertEquals(PilotSummary(), PilotSummary.from(emptyList(), warningDays = 3))
        assertNull(PilotSummary.from(emptyList(), 3).medianAddTimeMs)
    }

    @Test
    fun countsAddsByMethod() {
        val s = PilotSummary.from(
            listOf(added(AddMethod.SCAN, 1), added(AddMethod.SCAN, 2), added(AddMethod.MANUAL, 3)),
            warningDays = 3,
        )
        assertEquals(2, s.addedByScan)
        assertEquals(1, s.addedManually)
    }

    @Test
    fun medianOfOddAndEvenCountsIgnoringUntimedAdds() {
        val odd = listOf(added(AddMethod.MANUAL, 9_000), added(AddMethod.SCAN, 1_000), added(AddMethod.SCAN, 4_000))
        assertEquals(4_000L, PilotSummary.from(odd, 3).medianAddTimeMs)
        val even = odd + added(AddMethod.MANUAL, 6_000) + added(AddMethod.MANUAL, null)
        assertEquals(5_000L, PilotSummary.from(even, 3).medianAddTimeMs) // (4000 + 6000) / 2
    }

    @Test
    fun sumsUnitsAndConsumedWhileExpiringSoonUsesZeroToWarningDaysInclusive() {
        val events = listOf(
            closed(EventType.UNITS_CONSUMED, 1, daysToExpiry = -1), // expired: not "Por vencer"
            closed(EventType.UNITS_CONSUMED, 2, daysToExpiry = 0),
            closed(EventType.UNITS_CONSUMED, 3, daysToExpiry = 3),
            closed(EventType.UNITS_CONSUMED, 4, daysToExpiry = 4), // fresh with warningDays = 3
            closed(EventType.UNITS_DISCARDED, 5, daysToExpiry = 1), // discarded never counts as consumed
            ProductEvent(type = EventType.APP_OPEN, timestamp = 0),
        )
        val s = PilotSummary.from(events, warningDays = 3)
        assertEquals(10, s.unitsConsumed)
        assertEquals(5, s.unitsDiscarded)
        assertEquals(5, s.unitsConsumedExpiringSoon)
    }
}
