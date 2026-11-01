package com.example.freskoapp.domain.model

enum class EventType { APP_OPEN, PRODUCT_ADDED, UNITS_CONSUMED, UNITS_DISCARDED, PRODUCT_EDITED, NOTIFICATION_TAPPED }

// How the add form got its product: SCAN = a barcode was scanned in that form, MANUAL otherwise.
enum class AddMethod { SCAN, MANUAL }

// Local pilot metric; never leaves the device except through the manual CSV export.
data class ProductEvent(
    val id: Long = 0,
    val productId: Long? = null,
    val type: EventType,
    val units: Int? = null,
    val daysToExpiry: Int? = null, // at the moment of the event; negative = already expired
    val method: AddMethod? = null,
    val durationMs: Long? = null,
    val timestamp: Long, // epoch millis
)
