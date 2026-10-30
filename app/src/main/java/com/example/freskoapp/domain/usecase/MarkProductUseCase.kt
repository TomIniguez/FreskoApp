package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.repository.ProductRepository

// Logical delete: an active product becomes CONSUMED or DISCARDED. False if it was not active (or doesn't exist).
class MarkProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: Long, status: ProductStatus): Boolean {
        require(status != ProductStatus.ACTIVE)
        return repository.changeStatus(id, ProductStatus.ACTIVE, status)
    }
}
