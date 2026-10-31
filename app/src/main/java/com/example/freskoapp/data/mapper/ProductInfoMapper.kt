package com.example.freskoapp.data.mapper

import com.example.freskoapp.data.remote.ProductInfoDto
import com.example.freskoapp.domain.model.ProductInfo

// Null means "not found": status 0 (unknown or invalid code), no product, or no usable name.
fun ProductInfoDto.toDomain(): ProductInfo? {
    val product = product ?: return null
    val name = product.productName?.trim()
    if (status != 1 || name.isNullOrEmpty()) return null
    return ProductInfo(name = name, brand = product.brands?.trim()?.ifEmpty { null })
}
