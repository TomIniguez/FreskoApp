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

    // Returns the number of rows changed: 0 if quantity or status no longer match the expected ones.
    @Query("UPDATE products SET quantity = :newQuantity, status = :newStatus WHERE id = :id AND quantity = :oldQuantity AND status = :oldStatus")
    suspend fun updateIfUnchanged(id: Long, oldQuantity: Int, oldStatus: String, newQuantity: Int, newStatus: String): Int
}
