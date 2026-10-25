package com.example.freskoapp.domain.repository

import com.example.freskoapp.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>> // ordered by expirationDate ascending
    suspend fun addProduct(product: Product)
}
