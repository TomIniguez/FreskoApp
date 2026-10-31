package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.repository.ProductRepository

class AddProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product): AddResult {
        require(product.name.isNotBlank())
        require(product.quantity > 0)
        return repository.addProduct(product.copy(name = product.name.trim()))
    }
}
