package com.example.freskoapp.presentation.addproduct

import com.example.freskoapp.domain.model.Location
import java.time.LocalDate

data class AddProductUiState(
    val name: String = "",
    val quantity: String = "1",
    val expirationDate: LocalDate? = null,
    val location: Location = Location.HELADERA,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val merged: Boolean = false, // only meaningful once saved: the quantity went into an existing lot
    val notAvailable: Boolean = false, // edit mode: the product is gone or no longer active, the screen leaves
) {
    val canSave get() = !isSaving && name.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0 && expirationDate != null
}
