package com.example.freskoapp.domain.model

// Outcome of registering a product: the record that holds it, and whether the quantity went into an existing lot.
data class AddResult(val productId: Long, val merged: Boolean)
