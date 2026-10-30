package com.example.freskoapp.domain.repository

import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>> // active products only, ordered by expirationDate ascending
    fun getProduct(id: Long): Flow<Product?> // any status; emits null if the id doesn't exist
    suspend fun addProduct(product: Product)

    // Compare-and-set: only changes the product if its current status is `from`. Returns true if a row changed.
    suspend fun changeStatus(id: Long, from: ProductStatus, to: ProductStatus): Boolean
}
