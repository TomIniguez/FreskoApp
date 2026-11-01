package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.NoTransaction
import com.example.freskoapp.domain.model.AddMethod
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AddProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val today = Clock.fixed(Instant.parse("2026-11-28T12:00:00Z"), ZoneOffset.UTC) // 3 days before product()
    private val events = FakeEventRepository()
    private val useCase = AddProductUseCase(repo, events, NoTransaction, today)

    private suspend fun add(p: Product, method: AddMethod = AddMethod.MANUAL, durationMs: Long? = null) = useCase(p, method, durationMs)

    private fun product(
        name: String = "Leche",
        quantity: Int = 1,
        date: LocalDate = LocalDate.of(2026, 12, 1),
        location: Location = Location.HELADERA,
    ) = Product(name = name, expirationDate = date, quantity = quantity, location = location)

    @Test
    fun savesProductWithTrimmedName() = runBlocking {
        assertFalse(add(product(name = "  Leche  ")).merged)
        assertEquals(listOf("Leche"), repo.products.value.map { it.name })
    }

    @Test
    fun blankNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { add(product(name = "   ")) } }
        assertTrue(repo.products.value.isEmpty())
    }

    @Test
    fun zeroQuantityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { add(product(quantity = 0)) } }
        assertTrue(repo.products.value.isEmpty())
    }

    @Test
    fun sameNameDateAndLocationMergesQuantities() = runBlocking {
        add(product(quantity = 2))
        assertTrue(add(product(quantity = 3)).merged)
        assertEquals(listOf(5), repo.products.value.map { it.quantity })
    }

    @Test
    fun differentDateKeepsTwoLots() = runBlocking {
        add(product())
        assertFalse(add(product(date = LocalDate.of(2026, 12, 2))).merged)
        assertEquals(2, repo.products.value.size)
    }

    @Test
    fun differentLocationKeepsTwoLots() = runBlocking {
        add(product())
        assertFalse(add(product(location = Location.FREEZER)).merged)
        assertEquals(2, repo.products.value.size)
    }

    @Test
    fun nameDifferingOnlyByCaseOrSpacesMerges() = runBlocking {
        add(product(name = "Leche entera"))
        assertTrue(add(product(name = "  LECHE   Entera ")).merged)
        assertEquals(1, repo.products.value.size)
    }

    @Test
    fun accentsStayDistinct() = runBlocking {
        add(product(name = "Leche"))
        assertFalse(add(product(name = "Léche")).merged)
    }

    @Test
    fun closedRecordsAreNeverMergedInto() = runBlocking {
        repo.products.value = listOf(product().copy(id = 1, status = ProductStatus.CONSUMED))
        assertFalse(add(product()).merged)
        assertEquals(listOf(ProductStatus.CONSUMED, ProductStatus.ACTIVE), repo.products.value.map { it.status })
    }

    @Test
    fun createdAtComesFromTheClockAndAMergeKeepsTheOriginal() = runBlocking {
        fun at(millis: Long) = AddProductUseCase(repo, events, NoTransaction, Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC))
        at(5_000)(product(quantity = 1), AddMethod.MANUAL, null)
        at(9_000)(product(quantity = 2), AddMethod.MANUAL, null)
        assertEquals(listOf(5_000L), repo.products.value.map { it.createdAt })
        assertEquals(listOf(3), repo.products.value.map { it.quantity })
    }

    @Test
    fun addRecordsProductAddedWithMethodDurationAndDaysToExpiry() = runBlocking {
        add(product(quantity = 2), AddMethod.SCAN, durationMs = 12_345)
        assertEquals(
            listOf(
                ProductEvent(
                    id = 1, productId = 1, type = EventType.PRODUCT_ADDED, units = 2, daysToExpiry = 3,
                    method = AddMethod.SCAN, durationMs = 12_345, timestamp = today.millis(),
                )
            ),
            events.events.value,
        )
    }

    @Test
    fun mergedAddIsAlsoProductAddedOnTheExistingLot() = runBlocking {
        add(product(quantity = 1))
        add(product(quantity = 4), AddMethod.MANUAL, durationMs = 800)
        val second = events.events.value[1]
        assertEquals(EventType.PRODUCT_ADDED, second.type)
        assertEquals(1L, second.productId)
        assertEquals(4, second.units)
        assertEquals(800L, second.durationMs)
    }

    @Test
    fun rejectedAddRecordsNothing() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { add(product(quantity = 0)) } }
        assertTrue(events.events.value.isEmpty())
    }
}
