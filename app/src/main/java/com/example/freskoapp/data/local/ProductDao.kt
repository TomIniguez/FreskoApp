package com.example.freskoapp.data.local

import androidx.room.Insert
import androidx.room.Query
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY expirationEpochDay ASC")
    fun getAll(): Flow<List<ProductEntity>>

    @Insert
    suspend fun insert(product: ProductEntity)
}
