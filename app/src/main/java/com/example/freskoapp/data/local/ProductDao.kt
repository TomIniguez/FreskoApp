package com.example.freskoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY expirationEpochDay ASC")
    fun getActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Insert
    suspend fun insert(product: ProductEntity)

    // Returns the number of rows changed: 0 if the product isn't in the `from` status.
    @Query("UPDATE products SET status = :to WHERE id = :id AND status = :from")
    suspend fun updateStatus(id: Long, from: String, to: String): Int
}
