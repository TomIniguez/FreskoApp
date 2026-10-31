package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AddProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = AddProductUseCase(repo)

    private fun product(
        name: String = "Leche",
        quantity: Int = 1,
        date: LocalDate = LocalDate.of(2026, 12, 1),
        location: Location = Location.HELADERA,
    ) = Product(name = name, expirationDate = date, quantity = quantity, location = location)

    @Test
    fun savesProductWithTrimmedName() = runBlocking {
        assertEquals(AddResult.CREATED, useCase(product(name = "  Leche  ")))
        assertEquals(listOf("Leche"), repo.products.value.map { it.name })
    }

    @Test
    fun blankNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(product(name = "   ")) } }
        assertTrue(repo.products.value.isEmpty())
    }

    @Test
    fun zeroQuantityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(product(quantity = 0)) } }
        assertTrue(repo.products.value.isEmpty())
    }

    @Test
    fun sameNameDateAndLocationMergesQuantities() = runBlocking {
        useCase(product(quantity = 2))
        assertEquals(AddResult.MERGED, useCase(product(quantity = 3)))
        assertEquals(listOf(5), repo.products.value.map { it.quantity })
    }

    @Test
    fun differentDateKeepsTwoLots() = runBlocking {
        useCase(product())
        assertEquals(AddResult.CREATED, useCase(product(date = LocalDate.of(2026, 12, 2))))
        assertEquals(2, repo.products.value.size)
    }

    @Test
    fun differentLocationKeepsTwoLots() = runBlocking {
        useCase(product())
        assertEquals(AddResult.CREATED, useCase(product(location = Location.FREEZER)))
        assertEquals(2, repo.products.value.size)
    }

    @Test
    fun nameDifferingOnlyByCaseOrSpacesMerges() = runBlocking {
        useCase(product(name = "Leche entera"))
        assertEquals(AddResult.MERGED, useCase(product(name = "  LECHE   Entera ")))
        assertEquals(1, repo.products.value.size)
    }

    @Test
    fun accentsStayDistinct() = runBlocking {
        useCase(product(name = "Leche"))
        assertEquals(AddResult.CREATED, useCase(product(name = "Léche")))
    }

    @Test
    fun closedRecordsAreNeverMergedInto() = runBlocking {
        repo.products.value = listOf(product().copy(id = 1, status = ProductStatus.CONSUMED))
        assertEquals(AddResult.CREATED, useCase(product()))
        assertEquals(listOf(ProductStatus.CONSUMED, ProductStatus.ACTIVE), repo.products.value.map { it.status })
    }

    @Test
    fun createdAtComesFromTheClockAndAMergeKeepsTheOriginal() = runBlocking {
        fun at(millis: Long) = AddProductUseCase(repo, Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC))
        at(5_000)(product(quantity = 1))
        at(9_000)(product(quantity = 2))
        assertEquals(listOf(5_000L), repo.products.value.map { it.createdAt })
        assertEquals(listOf(3), repo.products.value.map { it.quantity })
    }
}
