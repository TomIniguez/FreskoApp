package com.example.freskoapp.data.repository

import com.example.freskoapp.data.local.ProductDao
import com.example.freskoapp.data.mapper.toDomain
import com.example.freskoapp.data.mapper.toEntity
import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.isSameLotAs
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(private val dao: ProductDao) : ProductRepository {
    override fun getProducts(): Flow<List<Product>> =
        dao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getProduct(id: Long): Flow<Product?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun addProduct(product: Product): AddResult =
        if (dao.insertOrMerge(product.toEntity()) { it.toDomain().isSameLotAs(product) }) AddResult.MERGED else AddResult.CREATED

    override suspend fun update(expected: Product, new: Product): Boolean =
        dao.updateIfUnchanged(expected.id, expected.quantity, expected.status.name, new.quantity, new.status.name) > 0

    override suspend fun updateDetails(product: Product): Boolean = dao.updateDetails(
        product.id, product.name, product.expirationDate.toEpochDay(), product.quantity, product.location.name,
    ) > 0
}
