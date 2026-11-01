package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.NoTransaction
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CloseUnitsUseCaseTest {
    private val repo = FakeProductRepository(
        listOf(Product(1, "Leche", LocalDate.of(2026, 12, 1), 3, Location.HELADERA))
    )
    private val today = Clock.fixed(Instant.parse("2026-11-29T12:00:00Z"), ZoneOffset.UTC) // 2 days before expiry
    private val events = FakeEventRepository()
    private val close = CloseUnitsUseCase(repo, events, NoTransaction, today)
    private val restore = RestoreUnitsUseCase(repo, events, NoTransaction)

    private suspend fun product() = repo.getProduct(1).first()!!

    @Test
    fun partialCloseKeepsTheRecordActiveWithFewerUnits() = runTest {
        assertEquals(2, close(1, 1, ProductStatus.CONSUMED)?.product?.quantity)
        assertEquals(ProductStatus.ACTIVE, product().status)
        assertEquals(2, product().quantity)
        assertEquals(listOf(1L), repo.getProducts().first().map { it.id })
    }

    @Test
    fun closingAllUnitsClosesTheRecordKeepingItsQuantity() = runTest {
        val result = close(1, 3, ProductStatus.DISCARDED)
        assertEquals(ProductStatus.DISCARDED, result?.product?.status)
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
        assertTrue(restore(1, 2, ProductStatus.CONSUMED, closed = false, eventId = 1))
        assertEquals(3, product().quantity)
        assertEquals(ProductStatus.ACTIVE, product().status)
    }

    @Test
    fun undoOfAFullCloseReopensTheRecord() = runTest {
        close(1, 3, ProductStatus.CONSUMED)
        assertTrue(restore(1, 3, ProductStatus.CONSUMED, closed = true, eventId = 1))
        assertEquals(3, product().quantity)
        assertEquals(ProductStatus.ACTIVE, product().status)
    }

    @Test
    fun undoFailsIfTheProductIsNotInTheStateCloseLeftIt() = runTest {
        close(1, 3, ProductStatus.CONSUMED)
        assertEquals(false, restore(1, 3, ProductStatus.DISCARDED, closed = true, eventId = 1))
        assertEquals(false, restore(1, 3, ProductStatus.CONSUMED, closed = false, eventId = 1)) // closed record, but undo says it stayed active
        assertEquals(ProductStatus.CONSUMED, product().status)
        assertEquals(1, events.events.value.size) // a failed undo keeps the event
    }

    @Test
    fun consumeRecordsUnitsAndDaysToExpiry() = runTest {
        val result = close(1, 2, ProductStatus.CONSUMED)!!
        assertEquals(
            listOf(
                ProductEvent(
                    id = result.eventId, productId = 1, type = EventType.UNITS_CONSUMED, units = 2, daysToExpiry = 2,
                    timestamp = today.millis(),
                )
            ),
            events.events.value,
        )
    }

    @Test
    fun discardRecordsUnitsDiscarded() = runTest {
        close(1, 3, ProductStatus.DISCARDED)
        assertEquals(listOf(EventType.UNITS_DISCARDED to 3), events.events.value.map { it.type to it.units })
    }

    @Test
    fun failedCloseRecordsNothing() = runTest {
        close(1, 4, ProductStatus.CONSUMED)
        close(99, 1, ProductStatus.CONSUMED)
        repo.products.value = repo.products.value.map { it.copy(status = ProductStatus.CONSUMED) }
        close(1, 1, ProductStatus.CONSUMED)
        assertTrue(events.events.value.isEmpty())
    }

    @Test
    fun undoRemovesOnlyTheEventItUndoes() = runTest {
        val first = close(1, 1, ProductStatus.CONSUMED)!!
        val second = close(1, 1, ProductStatus.DISCARDED)!!
        assertTrue(restore(1, 1, ProductStatus.DISCARDED, closed = false, eventId = second.eventId))
        assertEquals(listOf(first.eventId), events.events.value.map { it.id })
    }
}
