package com.example.freskoapp.domain

import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// In-memory repository that mirrors the DAO: active-only list ordered by date, compare-and-set status changes.
class FakeProductRepository(initial: List<Product> = emptyList()) : ProductRepository {
    val products = MutableStateFlow(initial)

    override fun getProducts(): Flow<List<Product>> =
        products.map { list -> list.filter { it.status == ProductStatus.ACTIVE }.sortedBy { it.expirationDate } }

    override fun getProduct(id: Long): Flow<Product?> = products.map { list -> list.find { it.id == id } }

    override suspend fun addProduct(product: Product) {
        val nextId = (products.value.maxOfOrNull { it.id } ?: 0) + 1
        products.value += product.copy(id = nextId)
    }

    override suspend fun changeStatus(id: Long, from: ProductStatus, to: ProductStatus): Boolean {
        val current = products.value.find { it.id == id }
        if (current == null || current.status != from) return false
        products.value = products.value.map { if (it.id == id) it.copy(status = to) else it }
        return true
    }
}
