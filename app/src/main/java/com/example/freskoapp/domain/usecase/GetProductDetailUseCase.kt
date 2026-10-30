package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDate

// One product with its computed expiration status. Also returns non-active products; the caller decides what to show.
class GetProductDetailUseCase(
    private val repository: ProductRepository,
    private val calculateStatus: CalculateExpirationStatusUseCase = CalculateExpirationStatusUseCase(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val warningDays: Flow<Int> = flowOf(DEFAULT_WARNING_DAYS),
) {
    operator fun invoke(id: Long): Flow<InventoryItem?> =
        combine(repository.getProduct(id), warningDays) { product, days ->
            product?.let { InventoryItem(it, calculateStatus(it.expirationDate, LocalDate.now(clock), days)) }
        }
}
