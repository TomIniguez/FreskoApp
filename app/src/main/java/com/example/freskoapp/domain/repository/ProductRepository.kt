package com.example.freskoapp.domain.repository

import com.example.freskoapp.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>> // active products only, ordered by expirationDate ascending
    fun getProduct(id: Long): Flow<Product?> // any status; emits null if the id doesn't exist
    suspend fun addProduct(product: Product)

    // Compare-and-set: writes quantity and status of `new` only if the stored ones still equal `expected`'s. True if a row changed.
    suspend fun update(expected: Product, new: Product): Boolean
}
