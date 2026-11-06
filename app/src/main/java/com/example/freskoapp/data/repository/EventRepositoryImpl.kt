package com.example.freskoapp.data.repository

import com.example.freskoapp.data.local.ProductEventDao
import com.example.freskoapp.data.local.ProductEventEntity
import com.example.freskoapp.domain.model.AddMethod
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepositoryImpl(private val dao: ProductEventDao) : EventRepository {
    override suspend fun record(event: ProductEvent): Long = dao.insert(
        ProductEventEntity(
            productId = event.productId,
            type = event.type.name,
            units = event.units,
            daysToExpiry = event.daysToExpiry,
            method = event.method?.name,
            durationMs = event.durationMs,
            timestamp = event.timestamp,
        )
    )

    override suspend fun delete(id: Long) = dao.delete(id)

    override fun observeAll(): Flow<List<ProductEvent>> = dao.observeAll().map { list ->
        list.map {
            ProductEvent(
                id = it.id,
                productId = it.productId,
                type = EventType.valueOf(it.type),
                units = it.units,
                daysToExpiry = it.daysToExpiry,
                method = it.method?.let(AddMethod::valueOf),
                durationMs = it.durationMs,
                timestamp = it.timestamp,
            )
        }
    }
}
