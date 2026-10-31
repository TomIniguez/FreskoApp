package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class GetExpiringProductsUseCaseTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun product(id: Long, daysFromToday: Long) = Product(
        id = id,
        name = "p$id",
        expirationDate = today.plusDays(daysFromToday),
        quantity = 1,
        location = Location.HELADERA,
    )

    private fun useCase(vararg products: Product, warningDays: Int = DEFAULT_WARNING_DAYS) =
        GetExpiringProductsUseCase(
            GetInventoryUseCase(
                object : ProductRepository {
                    override fun getProducts(): Flow<List<Product>> = flowOf(products.toList())
                    override fun getProduct(id: Long): Flow<Product?> = flowOf(null)
                    override suspend fun addProduct(product: Product) = AddResult.CREATED
                    override suspend fun update(expected: Product, new: Product) = false
                    override suspend fun updateDetails(product: Product) = false
                },
                clock = clock,
                warningDays = flowOf(warningDays),
            )
        )

    @Test
    fun returnsOnlyExpiringSoonInOrder() = runTest {
        val result = useCase(product(1, -1), product(2, 1), product(3, 3), product(4, 10))()
        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    @Test
    fun includesProductsExpiringToday() = runTest {
        assertEquals(listOf(1L), useCase(product(1, 0))().map { it.id })
    }

    @Test
    fun respectsCustomWarningDays() = runTest {
        val result = useCase(product(1, 2), product(2, 7), product(3, 8), warningDays = 7)()
        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun emptyWhenNothingIsExpiring() = runTest {
        assertEquals(emptyList<Product>(), useCase(product(1, -5), product(2, 20))())
    }

    // RF04 interaction: a consumed/discarded product stops being notified; undo makes it eligible again.
    @Test
    fun markedProductIsNotListedAndRestoredProductIs() = runTest {
        val repo = FakeProductRepository(listOf(product(1, 1)))
        val expiring = GetExpiringProductsUseCase(GetInventoryUseCase(repo, clock = clock))
        assertEquals(listOf(1L), expiring().map { it.id })
        CloseUnitsUseCase(repo)(1, 1, ProductStatus.CONSUMED)
        assertEquals(emptyList<Long>(), expiring().map { it.id })
        RestoreUnitsUseCase(repo)(1, 1, ProductStatus.CONSUMED, closed = true)
        assertEquals(listOf(1L), expiring().map { it.id })
    }
}
