package com.example.freskoapp.work

import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpirationNotifierTextTest {
    private val today = LocalDate.of(2026, 9, 30)

    private fun product(name: String, daysLeft: Long) = Product(name = name, expirationDate = today.plusDays(daysLeft), quantity = 1, location = Location.HELADERA)

    @Test
    fun namesTheProductClosestToExpiring() {
        val soonest = soonestToExpire(listOf(product("Pan", 3), product("Yogur natural", 1), product("Pollo", 2)), today)
        assertEquals(SoonestToExpire("Yogur natural", 1), soonest)
    }

    @Test
    fun aTieIsBrokenByNameSoTheTextIsStable() {
        assertEquals("Leche", soonestToExpire(listOf(product("Yogur", 0), product("Leche", 0)), today).name)
    }

    @Test
    fun neverReportsNegativeDays() {
        // The worker runs after midnight: a product that was "today" yesterday must not read "-1 días".
        assertEquals(0, soonestToExpire(listOf(product("Leche", -1)), today).daysLeft)
    }
}
