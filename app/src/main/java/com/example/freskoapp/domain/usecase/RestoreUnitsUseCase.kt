package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.reopenUnits
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first

// Undo of CloseUnitsUseCase: gives the units back and, if the record had closed (`closed`), reopens it.
// False if the product is no longer in the state that close left it in.
class RestoreUnitsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: Long, units: Int, status: ProductStatus, closed: Boolean): Boolean {
        require(status != ProductStatus.ACTIVE)
        require(units >= 1)
        val product = repository.getProduct(id).first() ?: return false
        if (product.status != (if (closed) status else ProductStatus.ACTIVE)) return false
        return repository.update(product, product.reopenUnits(units, closed))
    }
}
