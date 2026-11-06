package com.example.freskoapp.presentation.detail

import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.presentation.common.ProductRemoval

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val item: InventoryItem? = null,
    val isProcessing: Boolean = false,
    val removal: ProductRemoval? = null, // set once the product was marked; the screen then hands it back and leaves
) {
    // Unknown id, or the product was already consumed/discarded (e.g. a stale screen).
    val notAvailable: Boolean
        get() = !isLoading && removal == null && !isProcessing &&
            (item == null || item.product.status != ProductStatus.ACTIVE)
}
