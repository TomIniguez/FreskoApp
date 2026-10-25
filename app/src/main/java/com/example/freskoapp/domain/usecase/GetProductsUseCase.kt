package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.repository.ProductRepository

class GetProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke() = repository.getProducts()
}
