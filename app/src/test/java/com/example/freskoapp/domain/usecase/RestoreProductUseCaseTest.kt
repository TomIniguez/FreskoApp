package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RestoreProductUseCaseTest {
    private val repo = FakeProductRepository(
        listOf(
            Product(1, "Leche", LocalDate.of(2026, 12, 1), 1, Location.HELADERA),
            Product(2, "Queso", LocalDate.of(2026, 12, 10), 1, Location.HELADERA),
        )
    )
    private val mark = MarkProductUseCase(repo)
    private val useCase = RestoreProductUseCase(repo)

    private suspend fun activeIds() = repo.getProducts().first().map { it.id }

    @Test
    fun restoreBringsTheProductBackInDateOrder() = runTest {
        mark(1, ProductStatus.CONSUMED)
        assertEquals(listOf(2L), activeIds())
        assertTrue(useCase(1, ProductStatus.CONSUMED))
        assertEquals(listOf(1L, 2L), activeIds())
    }

    @Test
    fun wrongPreviousStatusReturnsFalseAndChangesNothing() = runTest {
        mark(1, ProductStatus.CONSUMED)
        assertFalse(useCase(1, ProductStatus.DISCARDED))
        assertEquals(ProductStatus.CONSUMED, repo.getProduct(1).first()?.status)
    }

    @Test
    fun activePreviousIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(1, ProductStatus.ACTIVE) } }
    }

    @Test
    fun restoringTwiceIsTrueThenFalse() = runTest {
        mark(1, ProductStatus.DISCARDED)
        assertTrue(useCase(1, ProductStatus.DISCARDED))
        assertFalse(useCase(1, ProductStatus.DISCARDED))
    }
}
