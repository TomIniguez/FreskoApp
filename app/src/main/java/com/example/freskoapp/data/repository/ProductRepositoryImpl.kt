package com.example.freskoapp.data.repository

import com.example.freskoapp.data.local.ProductDao
import com.example.freskoapp.data.mapper.toDomain
import com.example.freskoapp.data.mapper.toEntity
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(private val dao: ProductDao) : ProductRepository {
    override fun getProducts(): Flow<List<Product>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addProduct(product: Product) = dao.insert(product.toEntity())
}
