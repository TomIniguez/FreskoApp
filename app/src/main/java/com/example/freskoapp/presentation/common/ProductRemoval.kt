package com.example.freskoapp.presentation.common

import com.example.freskoapp.domain.model.ProductStatus

// What the detail screen hands back to the Inventory so it can offer "Deshacer": enough to revert the close
// (units to give back; `closed` = the record was closed, so undo must also reopen it; `eventId` = the pilot event undo removes).
data class ProductRemoval(
    val id: Long,
    val name: String,
    val status: ProductStatus,
    val units: Int,
    val closed: Boolean,
    val eventId: Long,
)
