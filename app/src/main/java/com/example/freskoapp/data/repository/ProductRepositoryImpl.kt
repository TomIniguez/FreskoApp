package com.example.freskoapp.data.repository

import com.example.freskoapp.data.local.ProductDao
import com.example.freskoapp.data.mapper.toDomain
import com.example.freskoapp.data.mapper.toEntity
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(private val dao: ProductDao) : ProductRepository {
    override fun getProducts(): Flow<List<Product>> =
        dao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getProduct(id: Long): Flow<Product?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun addProduct(product: Product) = dao.insert(product.toEntity())

    override suspend fun changeStatus(id: Long, from: ProductStatus, to: ProductStatus): Boolean =
        dao.updateStatus(id, from.name, to.name) > 0
}
