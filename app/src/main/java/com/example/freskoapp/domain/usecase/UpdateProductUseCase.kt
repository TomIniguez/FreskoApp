package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.UpdateResult
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

// Edits an ACTIVE product. Never merges with another record (each record is a lot, unlike registration) and
// never touches status or createdAt. Same validations as AddProductUseCase. PRODUCT_EDITED goes in the same transaction.
class UpdateProductUseCase(
    private val repository: ProductRepository,
    private val events: EventRepository,
    private val transaction: TransactionRunner,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        expirationDate: LocalDate,
        quantity: Int,
        location: Location,
    ): UpdateResult {
        require(name.isNotBlank())
        require(quantity > 0)
        val product = repository.getProduct(id).first() ?: return UpdateResult.NOT_FOUND
        if (product.status != ProductStatus.ACTIVE) return UpdateResult.NOT_ACTIVE
        val edited = product.copy(name = name.trim(), expirationDate = expirationDate, quantity = quantity, location = location)
        return transaction.run {
            if (!repository.updateDetails(edited)) return@run UpdateResult.NOT_ACTIVE
            events.record(
                ProductEvent(
                    productId = id,
                    type = EventType.PRODUCT_EDITED,
                    units = quantity,
                    daysToExpiry = daysToExpiry(expirationDate, clock),
                    timestamp = clock.millis(),
                )
            )
            UpdateResult.UPDATED
        }
    }
}
