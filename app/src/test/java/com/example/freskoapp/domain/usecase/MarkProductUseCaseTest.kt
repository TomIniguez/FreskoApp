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

class MarkProductUseCaseTest {
    private val repo = FakeProductRepository(
        listOf(Product(1, "Leche", LocalDate.of(2026, 12, 1), 1, Location.HELADERA))
    )
    private val useCase = MarkProductUseCase(repo)

    private suspend fun statusOf(id: Long) = repo.getProduct(id).first()?.status

    @Test
    fun consumedLeavesTheActiveList() = runTest {
        assertTrue(useCase(1, ProductStatus.CONSUMED))
        assertEquals(ProductStatus.CONSUMED, statusOf(1))
        assertTrue(repo.getProducts().first().isEmpty())
    }

    @Test
    fun discardedLeavesTheActiveList() = runTest {
        assertTrue(useCase(1, ProductStatus.DISCARDED))
        assertEquals(ProductStatus.DISCARDED, statusOf(1))
        assertTrue(repo.getProducts().first().isEmpty())
    }

    @Test
    fun activeTargetIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(1, ProductStatus.ACTIVE) } }
    }

    @Test
    fun markingAnAlreadyConsumedProductReturnsFalseAndKeepsStatus() = runTest {
        useCase(1, ProductStatus.CONSUMED)
        assertFalse(useCase(1, ProductStatus.DISCARDED))
        assertEquals(ProductStatus.CONSUMED, statusOf(1))
    }

    @Test
    fun unknownIdReturnsFalse() = runTest {
        assertFalse(useCase(99, ProductStatus.CONSUMED))
    }
}
