package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.reopenUnits
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.first

// Undo of CloseUnitsUseCase: gives the units back and, if the record had closed (`closed`), reopens it, and removes
// the event that close wrote (`eventId`) in the same transaction. False if the product is no longer in the state
// that close left it in; then the event stays too.
class RestoreUnitsUseCase(
    private val repository: ProductRepository,
    private val events: EventRepository,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(id: Long, units: Int, status: ProductStatus, closed: Boolean, eventId: Long): Boolean {
        require(status != ProductStatus.ACTIVE)
        require(units >= 1)
        val product = repository.getProduct(id).first() ?: return false
        if (product.status != (if (closed) status else ProductStatus.ACTIVE)) return false
        // Known: reopening may leave two active records with the same name/date/location; not merged (editing doesn't merge either).
        return transaction.run {
            repository.update(product, product.reopenUnits(units, closed)).also { if (it) events.delete(eventId) }
        }
    }
}
