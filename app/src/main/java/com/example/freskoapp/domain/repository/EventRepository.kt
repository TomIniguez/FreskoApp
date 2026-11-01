package com.example.freskoapp.domain.repository

import com.example.freskoapp.domain.model.ProductEvent
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun record(event: ProductEvent): Long // returns the new event id
    suspend fun delete(id: Long)
    fun observeAll(): Flow<List<ProductEvent>> // oldest first
}
