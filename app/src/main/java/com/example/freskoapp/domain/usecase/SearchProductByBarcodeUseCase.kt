package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.repository.ProductInfoRepository

class SearchProductByBarcodeUseCase(private val repository: ProductInfoRepository) {
    suspend operator fun invoke(barcode: String): ProductInfoResult {
        // Retail barcodes (EAN-8 to GTIN-14) are ASCII digits only; anything else can't be in the catalog.
        if (barcode.length !in 8..14 || barcode.any { it !in '0'..'9' }) return ProductInfoResult.NotFound
        val result = repository.search(barcode)
        // Defensive: a product without a usable name is no help to prefill the form.
        return if (result is ProductInfoResult.Found && result.info.name.isBlank()) ProductInfoResult.NotFound else result
    }
}
