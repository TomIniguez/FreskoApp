package com.example.freskoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductEventDao {
    @Insert
    suspend fun insert(event: ProductEventEntity): Long

    @Query("DELETE FROM product_event WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM product_event ORDER BY timestamp ASC, id ASC")
    fun observeAll(): Flow<List<ProductEventEntity>>
}
