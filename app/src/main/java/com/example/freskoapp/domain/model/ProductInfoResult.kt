package com.example.freskoapp.domain.model

sealed interface ProductInfoResult {
    data class Found(val info: ProductInfo) : ProductInfoResult
    data object NotFound : ProductInfoResult
    data object Offline : ProductInfoResult
    data object Timeout : ProductInfoResult
    data object ServerError : ProductInfoResult
}
