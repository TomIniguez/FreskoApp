package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.UpdateResult
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Edits an ACTIVE product. Never merges with another record (each record is a lot, unlike registration) and
// never touches status or createdAt. Same validations as AddProductUseCase.
class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        expirationDate: LocalDate,
        quantity: Int,
        location: Location,
    ): UpdateResult {
        require(name.isNotBlank())
        require(quantity > 0)
        val product = repository.getProduct(id).first() ?: return UpdateResult.NOT_FOUND
        if (product.status != ProductStatus.ACTIVE) return UpdateResult.NOT_ACTIVE
        val edited = product.copy(name = name.trim(), expirationDate = expirationDate, quantity = quantity, location = location)
        return if (repository.updateDetails(edited)) UpdateResult.UPDATED else UpdateResult.NOT_ACTIVE
    }
}
