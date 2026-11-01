package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.NoTransaction
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.UpdateResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UpdateProductUseCaseTest {
    private val date = LocalDate.of(2026, 12, 1)
    private val repo = FakeProductRepository(
        listOf(
            Product(1, "Leche", date, 2, Location.HELADERA, createdAt = 1_000),
            Product(2, "Leche", date.plusDays(5), 1, Location.HELADERA, createdAt = 2_000),
            Product(3, "Queso", date, 1, Location.HELADERA, ProductStatus.CONSUMED, createdAt = 3_000),
        )
    )
    private val today = Clock.fixed(Instant.parse("2026-11-30T12:00:00Z"), ZoneOffset.UTC) // 1 day before `date`
    private val events = FakeEventRepository()
    private val useCase = UpdateProductUseCase(repo, events, NoTransaction, today)

    private fun product(id: Long) = repo.products.value.single { it.id == id }

    @Test
    fun validEditChangesFieldsAndKeepsStatusAndCreatedAt() = runTest {
        val newDate = date.plusDays(3)
        assertEquals(UpdateResult.UPDATED, useCase(1, "  Leche entera ", newDate, 4, Location.FREEZER))
        assertEquals(Product(1, "Leche entera", newDate, 4, Location.FREEZER, ProductStatus.ACTIVE, 1_000), product(1))
    }

    @Test
    fun editingToMatchAnotherLotDoesNotMerge() = runTest {
        assertEquals(UpdateResult.UPDATED, useCase(2, "Leche", date, 1, Location.HELADERA))
        assertEquals(3, repo.products.value.size)
        assertEquals(listOf(2, 1), listOf(product(1).quantity, product(2).quantity))
    }

    @Test
    fun blankNameAndZeroQuantityThrow() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(1, "  ", date, 1, Location.HELADERA) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(1, "Leche", date, 0, Location.HELADERA) } }
        assertEquals("Leche", product(1).name)
    }

    @Test
    fun closedProductReturnsErrorAndStaysUntouched() = runTest {
        assertEquals(UpdateResult.NOT_ACTIVE, useCase(3, "Otro", date, 9, Location.FREEZER))
        assertEquals("Queso", product(3).name)
        assertEquals(emptyList<ProductEvent>(), events.events.value)
    }

    @Test
    fun unknownProductReturnsNotFound() = runTest {
        assertEquals(UpdateResult.NOT_FOUND, useCase(99, "Leche", date, 1, Location.HELADERA))
        assertEquals(emptyList<ProductEvent>(), events.events.value)
    }

    @Test
    fun editRecordsProductEditedWithTheNewValues() = runTest {
        useCase(1, "Leche", date.plusDays(2), 5, Location.HELADERA)
        assertEquals(
            listOf(
                ProductEvent(
                    id = 1, productId = 1, type = EventType.PRODUCT_EDITED, units = 5, daysToExpiry = 3,
                    timestamp = today.millis(),
                )
            ),
            events.events.value,
        )
    }
}
