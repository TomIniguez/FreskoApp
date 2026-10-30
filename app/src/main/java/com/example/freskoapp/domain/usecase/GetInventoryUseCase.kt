package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.InventoryFilter
import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

// Active products ordered by expiration, with the computed expiration status, filtered by status and location.
class GetInventoryUseCase(
    private val repository: ProductRepository,
    private val calculateStatus: CalculateExpirationStatusUseCase = CalculateExpirationStatusUseCase(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val warningDays: Int = DEFAULT_WARNING_DAYS,
) {
    operator fun invoke(filter: InventoryFilter = InventoryFilter()): Flow<List<InventoryItem>> =
        repository.getProducts().map { products ->
            val today = LocalDate.now(clock)
            products
                .filter { filter.location == null || it.location == filter.location }
                .map { InventoryItem(it, calculateStatus(it.expirationDate, today, warningDays)) }
                .filter { filter.status == null || it.expirationStatus == filter.status }
        }
}
