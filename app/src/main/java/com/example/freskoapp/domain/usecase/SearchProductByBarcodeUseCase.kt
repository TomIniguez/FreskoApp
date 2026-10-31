package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.repository.ProductInfoRepository

class SearchProductByBarcodeUseCase(private val repository: ProductInfoRepository) {
    // No format validation: an invalid code is just NotFound, as the catalog reports it.
    suspend operator fun invoke(barcode: String): ProductInfoResult {
        val result = repository.search(barcode)
        // Defensive: a product without a usable name is no help to prefill the form.
        return if (result is ProductInfoResult.Found && result.info.name.isBlank()) ProductInfoResult.NotFound else result
    }
}
