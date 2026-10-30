package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.repository.ProductRepository

class AddProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product) {
        require(product.name.isNotBlank())
        require(product.quantity > 0)
        repository.addProduct(product.copy(name = product.name.trim()))
    }
}
