package com.example.freskoapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// No foreign key on purpose: products are never deleted, and an event must not depend on the product row.
@Entity(tableName = "product_event")
data class ProductEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long?,
    val type: String, // EventType.name
    val units: Int?,
    val daysToExpiry: Int?,
    val method: String?, // AddMethod.name
    val durationMs: Long?,
    val timestamp: Long,
)
