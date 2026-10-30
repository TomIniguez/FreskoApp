package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.repository.ProductRepository

// Undo of MarkProductUseCase. False if the product is no longer in the `previous` status.
class RestoreProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: Long, previous: ProductStatus): Boolean {
        require(previous != ProductStatus.ACTIVE)
        return repository.changeStatus(id, previous, ProductStatus.ACTIVE)
    }
}
