package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.InventoryFilter
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class GetInventoryUseCaseTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun product(id: Long, daysFromToday: Long, location: Location) = Product(
        id = id,
        name = "p$id",
        expirationDate = today.plusDays(daysFromToday),
        quantity = 1,
        location = location,
    )

    private val products = listOf(
        product(1, -2, Location.HELADERA),
        product(2, 1, Location.FREEZER),
        product(3, 10, Location.HELADERA),
    )
    private val repo = object : ProductRepository {
        override fun getProducts(): Flow<List<Product>> = flowOf(products)
        override suspend fun addProduct(product: Product) = Unit
    }
    private val useCase = GetInventoryUseCase(repo, clock = clock)

    private suspend fun ids(filter: InventoryFilter) = useCase(filter).first().map { it.product.id }

    @Test
    fun noFilterReturnsEverythingInRepositoryOrder() = runTest {
        assertEquals(listOf(1L, 2L, 3L), ids(InventoryFilter()))
    }

    @Test
    fun filtersByStatus() = runTest {
        assertEquals(listOf(1L), ids(InventoryFilter(status = ExpirationStatus.EXPIRED)))
    }

    @Test
    fun filtersByLocation() = runTest {
        assertEquals(listOf(1L, 3L), ids(InventoryFilter(location = Location.HELADERA)))
    }

    @Test
    fun combinesStatusAndLocation() = runTest {
        assertEquals(emptyList<Long>(), ids(InventoryFilter(ExpirationStatus.EXPIRING_SOON, Location.HELADERA)))
        assertEquals(listOf(2L), ids(InventoryFilter(ExpirationStatus.EXPIRING_SOON, Location.FREEZER)))
    }
}
