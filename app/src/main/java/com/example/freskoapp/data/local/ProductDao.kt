package com.example.freskoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY expirationEpochDay ASC")
    fun getActive(): Flow<List<ProductEntity>>

    @Insert
    suspend fun insert(product: ProductEntity)
}
