package com.example.freskoapp.domain.model

// What an external catalog knows about a barcode; brand is optional there.
data class ProductInfo(val name: String, val brand: String?)
