package com.example.freskoapp.domain.repository

import com.example.freskoapp.domain.model.ProductInfoResult

interface ProductInfoRepository {
    // Never throws for network or server failures; they come back as a result.
    suspend fun search(barcode: String): ProductInfoResult
}
