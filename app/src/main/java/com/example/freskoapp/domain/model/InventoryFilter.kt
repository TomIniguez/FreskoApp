package com.example.freskoapp.domain.model

// A null field means "no filter" for that dimension.
data class InventoryFilter(
    val status: ExpirationStatus? = null,
    val location: Location? = null,
) {
    val isActive get() = status != null || location != null
}
