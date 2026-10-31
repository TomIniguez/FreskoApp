package com.example.freskoapp.domain

import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.repository.ProductInfoRepository

class FakeProductInfoRepository(var result: ProductInfoResult = ProductInfoResult.NotFound) : ProductInfoRepository {
    var error: Throwable? = null
    val searched = mutableListOf<String>()

    override suspend fun search(barcode: String): ProductInfoResult {
        searched += barcode
        error?.let { throw it }
        return result
    }
}
