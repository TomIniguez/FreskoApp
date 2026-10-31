package com.example.freskoapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val expirationEpochDay: Long, // epoch day as Long avoids a TypeConverter
    val quantity: Int,
    val location: String,
    @ColumnInfo(defaultValue = "'ACTIVE'") val status: String = "ACTIVE", // ProductStatus.name
    @ColumnInfo(defaultValue = "0") val createdAt: Long = 0,
)
