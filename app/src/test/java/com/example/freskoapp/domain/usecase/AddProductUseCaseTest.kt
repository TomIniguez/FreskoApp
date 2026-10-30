package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeRepository : ProductRepository {
    val saved = mutableListOf<Product>()
    override fun getProducts(): Flow<List<Product>> = flowOf(saved)
    override fun getProduct(id: Long): Flow<Product?> = flowOf(null)
    override suspend fun addProduct(product: Product) { saved += product }
    override suspend fun changeStatus(id: Long, from: ProductStatus, to: ProductStatus) = false
}

class AddProductUseCaseTest {
    private val repo = FakeRepository()
    private val useCase = AddProductUseCase(repo)

    private fun product(name: String = "Leche", quantity: Int = 1) = Product(
        name = name,
        expirationDate = LocalDate.of(2026, 12, 1),
        quantity = quantity,
        location = Location.HELADERA,
    )

    @Test
    fun savesProductWithTrimmedName() = runBlocking {
        useCase(product(name = "  Leche  "))
        assertEquals(listOf("Leche"), repo.saved.map { it.name })
    }

    @Test
    fun blankNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(product(name = "   ")) } }
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun zeroQuantityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(product(quantity = 0)) } }
        assertTrue(repo.saved.isEmpty())
    }
}
