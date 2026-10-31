package com.example.freskoapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class], version = 3, exportSchema = true)
abstract class FreskoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
