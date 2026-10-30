package com.example.freskoapp.presentation.common

import com.example.freskoapp.domain.model.ProductStatus

// What the detail screen hands back to the Inventory so it can offer "Deshacer".
data class ProductRemoval(val id: Long, val name: String, val status: ProductStatus)
