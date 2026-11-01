package com.example.freskoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY expirationEpochDay ASC")
    fun getActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND expirationEpochDay = :epochDay AND location = :location")
    suspend fun findActiveLots(epochDay: Long, location: String): List<ProductEntity>

    @Query("UPDATE products SET quantity = quantity + :units WHERE id = :id")
    suspend fun addQuantity(id: Long, units: Int)

    // Editing: only ACTIVE rows; never touches status or createdAt. Returns rows changed.
    @Query("UPDATE products SET name = :name, expirationEpochDay = :epochDay, quantity = :quantity, location = :location WHERE id = :id AND status = 'ACTIVE'")
    suspend fun updateDetails(id: Long, name: String, epochDay: Long, quantity: Int, location: String): Int

    // Find + merge-or-insert in one transaction so a double tap can't create duplicates. Returns (id of the lot, merged).
    @Transaction
    suspend fun insertOrMerge(product: ProductEntity, isSameLot: (ProductEntity) -> Boolean): Pair<Long, Boolean> {
        val match = findActiveLots(product.expirationEpochDay, product.location).firstOrNull(isSameLot)
        if (match == null) return insert(product) to false
        addQuantity(match.id, product.quantity)
        return match.id to true
    }

    // Returns the number of rows changed: 0 if quantity or status no longer match the expected ones.
    @Query("UPDATE products SET quantity = :newQuantity, status = :newStatus WHERE id = :id AND quantity = :oldQuantity AND status = :oldStatus")
    suspend fun updateIfUnchanged(id: Long, oldQuantity: Int, oldStatus: String, newQuantity: Int, newStatus: String): Int
}
