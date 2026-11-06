package com.example.freskoapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Open Food Facts response for ?fields=product_name,brands; status is 1 found, 0 not found or invalid code.
@Serializable
data class ProductInfoDto(
    val status: Int = 0,
    val product: ProductDto? = null,
)

@Serializable
data class ProductDto(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null, // comma-separated, e.g. "Nutella, Ferrero"
)
