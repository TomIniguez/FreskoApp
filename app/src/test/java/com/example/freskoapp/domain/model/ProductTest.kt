package com.example.freskoapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class ProductTest {
    private val three = Product(1, "Leche", LocalDate.of(2026, 12, 1), 3, Location.HELADERA)

    @Test
    fun consumeOneOfThreeKeepsItActiveWithTwo() {
        val r = three.closeUnits(1, ProductStatus.CONSUMED)
        assertEquals(2, r.quantity)
        assertEquals(ProductStatus.ACTIVE, r.status)
    }

    @Test
    fun consumeAllClosesItKeepingQuantity() {
        val r = three.closeUnits(3, ProductStatus.CONSUMED)
        assertEquals(3, r.quantity)
        assertEquals(ProductStatus.CONSUMED, r.status)
    }

    @Test
    fun consumeMoreThanAvailableFails() {
        assertThrows(IllegalArgumentException::class.java) { three.closeUnits(4, ProductStatus.CONSUMED) }
        assertThrows(IllegalArgumentException::class.java) { three.closeUnits(0, ProductStatus.CONSUMED) }
    }

    @Test
    fun undoOfPartialGivesUnitsBack() {
        val after = three.closeUnits(1, ProductStatus.CONSUMED)
        assertEquals(three, after.reopenUnits(1, closed = false))
    }

    @Test
    fun undoOfFullCloseReopens() {
        val after = three.closeUnits(3, ProductStatus.DISCARDED)
        assertEquals(three, after.reopenUnits(3, closed = true))
    }
}
