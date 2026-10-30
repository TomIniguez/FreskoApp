package com.example.freskoapp.domain.model

// Logical state of a product; consumed/discarded products are never deleted (RF04).
enum class ProductStatus { ACTIVE, CONSUMED, DISCARDED }
