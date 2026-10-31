package com.example.freskoapp.domain

import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.repository.ProductInfoRepository
import kotlinx.coroutines.CompletableDeferred

class FakeProductInfoRepository(var result: ProductInfoResult = ProductInfoResult.NotFound) : ProductInfoRepository {
    var error: Throwable? = null
    // When set, search() suspends until it completes: lets a test hold a lookup in flight.
    var gate: CompletableDeferred<Unit>? = null
    val searched = mutableListOf<String>()
    val calls get() = searched.size

    override suspend fun search(barcode: String): ProductInfoResult {
        searched += barcode
        val answer = result // fixed at call time, so a held call returns what was set when it started
        gate?.await()
        error?.let { throw it }
        return answer
    }
}
