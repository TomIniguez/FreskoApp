package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.closeUnits
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first

// Consume or discard `units` of an active product (same rule for both). Returns the product as it is now
// (closed if status != ACTIVE), or null if it wasn't active, doesn't exist, has fewer units, or changed meanwhile.
class CloseUnitsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: Long, units: Int, status: ProductStatus): Product? {
        require(status != ProductStatus.ACTIVE)
        val product = repository.getProduct(id).first() ?: return null
        if (product.status != ProductStatus.ACTIVE || units !in 1..product.quantity) return null
        val updated = product.closeUnits(units, status)
        return updated.takeIf { repository.update(product, it) }
    }
}
