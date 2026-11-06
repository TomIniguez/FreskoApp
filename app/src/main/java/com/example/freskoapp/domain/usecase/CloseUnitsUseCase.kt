package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.closeUnits
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.first
import java.time.Clock

// The product as it is after closing (closed if status != ACTIVE) and the event that recorded it, needed by undo.
data class ClosedUnits(val product: Product, val eventId: Long)

// Consume or discard `units` of an active product (same rule for both). Returns null if it wasn't active,
// doesn't exist, has fewer units, or changed meanwhile. The UNITS_* event is written in the same transaction.
class CloseUnitsUseCase(
    private val repository: ProductRepository,
    private val events: EventRepository,
    private val transaction: TransactionRunner,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend operator fun invoke(id: Long, units: Int, status: ProductStatus): ClosedUnits? {
        require(status != ProductStatus.ACTIVE)
        val product = repository.getProduct(id).first() ?: return null
        if (product.status != ProductStatus.ACTIVE || units !in 1..product.quantity) return null
        val updated = product.closeUnits(units, status)
        return transaction.run {
            if (!repository.update(product, updated)) return@run null
            val eventId = events.record(
                ProductEvent(
                    productId = id,
                    type = if (status == ProductStatus.CONSUMED) EventType.UNITS_CONSUMED else EventType.UNITS_DISCARDED,
                    units = units,
                    daysToExpiry = daysToExpiry(product.expirationDate, clock),
                    timestamp = clock.millis(),
                )
            )
            ClosedUnits(updated, eventId)
        }
    }
}
