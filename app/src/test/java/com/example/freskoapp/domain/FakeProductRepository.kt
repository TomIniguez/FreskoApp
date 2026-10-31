package com.example.freskoapp.domain

import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.isSameLotAs
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// In-memory repository that mirrors the DAO: active-only list ordered by date, lot merging on add, compare-and-set updates.
class FakeProductRepository(initial: List<Product> = emptyList()) : ProductRepository {
    val products = MutableStateFlow(initial)

    override fun getProducts(): Flow<List<Product>> =
        products.map { list -> list.filter { it.status == ProductStatus.ACTIVE }.sortedBy { it.expirationDate } }

    override fun getProduct(id: Long): Flow<Product?> = products.map { list -> list.find { it.id == id } }

    override suspend fun addProduct(product: Product): AddResult {
        val match = products.value.find { it.isSameLotAs(product) }
        if (match != null) {
            products.value = products.value.map {
                if (it.id == match.id) it.copy(quantity = it.quantity + product.quantity) else it
            }
            return AddResult.MERGED
        }
        val nextId = (products.value.maxOfOrNull { it.id } ?: 0) + 1
        products.value += product.copy(id = nextId)
        return AddResult.CREATED
    }

    override suspend fun update(expected: Product, new: Product): Boolean {
        val current = products.value.find { it.id == expected.id }
        if (current == null || current.quantity != expected.quantity || current.status != expected.status) return false
        products.value = products.value.map { if (it.id == expected.id) it.copy(quantity = new.quantity, status = new.status) else it }
        return true
    }

    override suspend fun updateDetails(product: Product): Boolean {
        val current = products.value.find { it.id == product.id }
        if (current == null || current.status != ProductStatus.ACTIVE) return false
        products.value = products.value.map {
            if (it.id == product.id) it.copy(name = product.name, expirationDate = product.expirationDate, quantity = product.quantity, location = product.location) else it
        }
        return true
    }
}
