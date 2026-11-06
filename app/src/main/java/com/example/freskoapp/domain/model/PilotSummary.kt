package com.example.freskoapp.domain.model

data class PilotSummary(
    val addedByScan: Int = 0,
    val addedManually: Int = 0,
    val medianAddTimeMs: Long? = null, // null until there is a timed add
    val unitsConsumed: Int = 0,
    val unitsDiscarded: Int = 0,
    val unitsConsumedExpiringSoon: Int = 0, // consumed with daysToExpiry in 0..warningDays ("Por vencer")
) {
    companion object {
        // shortcut: uses today's warningDays for every event; if the participant changes it mid-pilot, past
        // events are judged by the new value. Store warningDays per event if that matters.
        fun from(events: List<ProductEvent>, warningDays: Int): PilotSummary {
            val added = events.filter { it.type == EventType.PRODUCT_ADDED }
            val consumed = events.filter { it.type == EventType.UNITS_CONSUMED }
            return PilotSummary(
                addedByScan = added.count { it.method == AddMethod.SCAN },
                addedManually = added.count { it.method == AddMethod.MANUAL },
                medianAddTimeMs = added.mapNotNull { it.durationMs }.median(),
                unitsConsumed = consumed.sumOf { it.units ?: 0 },
                unitsDiscarded = events.filter { it.type == EventType.UNITS_DISCARDED }.sumOf { it.units ?: 0 },
                unitsConsumedExpiringSoon = consumed.filter { it.daysToExpiry in 0..warningDays }.sumOf { it.units ?: 0 },
            )
        }

        private fun List<Long>.median(): Long? {
            if (isEmpty()) return null
            val s = sorted()
            return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
        }
    }
}
