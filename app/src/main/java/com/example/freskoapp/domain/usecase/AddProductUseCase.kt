package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.repository.ProductRepository
import java.time.Clock

class AddProductUseCase(
    private val repository: ProductRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(product: Product): AddResult {
        require(product.name.isNotBlank())
        require(product.quantity > 0)
        // A merge only adds quantity to the existing lot, so that lot keeps its original createdAt.
        return repository.addProduct(product.copy(name = product.name.trim(), createdAt = clock.millis()))
    }
}
