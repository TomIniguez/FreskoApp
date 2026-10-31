package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CloseUnitsUseCaseTest {
    private val repo = FakeProductRepository(
        listOf(Product(1, "Leche", LocalDate.of(2026, 12, 1), 3, Location.HELADERA))
    )
    private val close = CloseUnitsUseCase(repo)
    private val restore = RestoreUnitsUseCase(repo)

    private suspend fun product() = repo.getProduct(1).first()!!

    @Test
    fun partialCloseKeepsTheRecordActiveWithFewerUnits() = runTest {
        assertEquals(2, close(1, 1, ProductStatus.CONSUMED)?.quantity)
        assertEquals(ProductStatus.ACTIVE, product().status)
        assertEquals(2, product().quantity)
        assertEquals(listOf(1L), repo.getProducts().first().map { it.id })
    }

    @Test
    fun closingAllUnitsClosesTheRecordKeepingItsQuantity() = runTest {
        val result = close(1, 3, ProductStatus.DISCARDED)
        assertEquals(ProductStatus.DISCARDED, result?.status)
        assertEquals(3, product().quantity)
        assertTrue(repo.getProducts().first().isEmpty())
    }

    @Test
    fun outOfRangeUnitsChangeNothing() = runTest {
        assertNull(close(1, 0, ProductStatus.CONSUMED))
        assertNull(close(1, 4, ProductStatus.CONSUMED))
        assertEquals(3, product().quantity)
    }

    @Test
    fun closedOrUnknownProductReturnsNull() = runTest {
        close(1, 3, ProductStatus.CONSUMED)
        assertNull(close(1, 1, ProductStatus.DISCARDED))
        assertNull(close(99, 1, ProductStatus.CONSUMED))
    }

    @Test
    fun activeTargetIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { close(1, 1, ProductStatus.ACTIVE) } }
    }

    @Test
    fun undoOfAPartialCloseGivesTheUnitsBack() = runTest {
        close(1, 2, ProductStatus.CONSUMED)
        assertTrue(restore(1, 2, ProductStatus.CONSUMED, closed = false))
        assertEquals(3, product().quantity)
        assertEquals(ProductStatus.ACTIVE, product().status)
    }

    @Test
    fun undoOfAFullCloseReopensTheRecord() = runTest {
        close(1, 3, ProductStatus.CONSUMED)
        assertTrue(restore(1, 3, ProductStatus.CONSUMED, closed = true))
        assertEquals(3, product().quantity)
        assertEquals(ProductStatus.ACTIVE, product().status)
    }

    @Test
    fun undoFailsIfTheProductIsNotInTheStateCloseLeftIt() = runTest {
        close(1, 3, ProductStatus.CONSUMED)
        assertEquals(false, restore(1, 3, ProductStatus.DISCARDED, closed = true))
        assertEquals(false, restore(1, 3, ProductStatus.CONSUMED, closed = false)) // closed record, but undo says it stayed active
        assertEquals(ProductStatus.CONSUMED, product().status)
    }
}
