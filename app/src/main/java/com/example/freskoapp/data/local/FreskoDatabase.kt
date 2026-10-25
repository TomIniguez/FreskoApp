package com.example.freskoapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class FreskoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
