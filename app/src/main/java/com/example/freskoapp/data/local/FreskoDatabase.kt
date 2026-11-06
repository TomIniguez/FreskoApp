package com.example.freskoapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class, ProductEventEntity::class], version = 4, exportSchema = true)
abstract class FreskoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productEventDao(): ProductEventDao
}
