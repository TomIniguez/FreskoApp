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
    val lookup: LookupState = LookupState.Idle,
    val scannedBarcode: String? = null, // shown on screen and reused by "Reintentar"; never stored with the product
    val showErrors: Boolean = false, // set by a save attempt with missing fields; errors then show next to each field
) {
    // The barcode lookup never gates saving: manual entry must always work.
    val canSave get() = !isSaving && name.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0 && expirationDate != null
    val nameError get() = showErrors && name.isBlank()
    val dateError get() = showErrors && expirationDate == null
}

// Name lookup for a scanned barcode; it only ever suggests the name.
sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data class Found(val name: String) : LookupState
    data class Failure(val kind: LookupFailure) : LookupState
}

enum class LookupFailure { NOT_FOUND, TIMEOUT, SERVER_ERROR, OFFLINE }
