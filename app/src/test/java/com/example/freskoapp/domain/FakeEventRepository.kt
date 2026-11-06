package com.example.freskoapp.domain

import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEventRepository(initial: List<ProductEvent> = emptyList()) : EventRepository {
    val events = MutableStateFlow(initial)

    override suspend fun record(event: ProductEvent): Long {
        val id = (events.value.maxOfOrNull { it.id } ?: 0) + 1
        events.value += event.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        events.value = events.value.filterNot { it.id == id }
    }

    override fun observeAll(): Flow<List<ProductEvent>> = events
}

// No rollback: atomicity is Room's job (RoomTransactionRunner); unit tests only check what gets written.
object NoTransaction : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
