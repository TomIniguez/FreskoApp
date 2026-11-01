package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.AddMethod
import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.repository.TransactionRunner
import java.time.Clock

class AddProductUseCase(
    private val repository: ProductRepository,
    private val events: EventRepository,
    private val transaction: TransactionRunner,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    // durationMs: from opening the add form to this save. A merged add is still one PRODUCT_ADDED.
    suspend operator fun invoke(product: Product, method: AddMethod, durationMs: Long?): AddResult {
        require(product.name.isNotBlank())
        require(product.quantity > 0)
        val now = clock.millis()
        return transaction.run {
            // A merge only adds quantity to the existing lot, so that lot keeps its original createdAt.
            val result = repository.addProduct(product.copy(name = product.name.trim(), createdAt = now))
            events.record(
                ProductEvent(
                    productId = result.productId,
                    type = EventType.PRODUCT_ADDED,
                    units = product.quantity,
                    daysToExpiry = daysToExpiry(product.expirationDate, clock),
                    method = method,
                    durationMs = durationMs,
                    timestamp = now,
                )
            )
            result
        }
    }
}
