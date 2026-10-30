package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class GetProductDetailUseCaseTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val repo = FakeProductRepository(
        listOf(Product(1, "Leche", today.plusDays(5), 1, Location.HELADERA))
    )

    @Test
    fun computesStatusWithWarningDays() = runTest {
        val default = GetProductDetailUseCase(repo, clock = clock)
        assertEquals(ExpirationStatus.FRESH, default(1).first()?.expirationStatus)
        val wide = GetProductDetailUseCase(repo, clock = clock, warningDays = flowOf(7))
        assertEquals(ExpirationStatus.EXPIRING_SOON, wide(1).first()?.expirationStatus)
    }

    @Test
    fun unknownIdEmitsNull() = runTest {
        assertNull(GetProductDetailUseCase(repo, clock = clock)(99).first())
    }

    @Test
    fun emitsAgainWhenTheProductIsMarked() = runTest {
        val emissions = mutableListOf<ProductStatus?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            GetProductDetailUseCase(repo, clock = clock)(1).collect { emissions.add(it?.product?.status) }
        }
        MarkProductUseCase(repo)(1, ProductStatus.CONSUMED)
        job.cancel()
        assertEquals(listOf(ProductStatus.ACTIVE, ProductStatus.CONSUMED), emissions)
    }
}
