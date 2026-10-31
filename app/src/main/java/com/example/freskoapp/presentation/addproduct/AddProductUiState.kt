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
    val barcode: String? = null, // last scanned code, for display and "Reintentar"; never saved with the product
) {
    // The barcode lookup never gates saving: manual entry must always work.
    val canSave get() = !isSaving && name.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0 && expirationDate != null
}

// Name lookup for a scanned barcode; it only ever suggests the name.
sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data class Found(val name: String) : LookupState
    data class Failure(val kind: LookupFailure) : LookupState {
        // Offline gets no retry: the user has to fix the connection first, and manual entry is right there.
        val canRetry get() = kind != LookupFailure.OFFLINE
    }
}

enum class LookupFailure { NOT_FOUND, TIMEOUT, SERVER_ERROR, OFFLINE }
