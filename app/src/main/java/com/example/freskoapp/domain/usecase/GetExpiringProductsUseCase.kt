package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.InventoryFilter
import com.example.freskoapp.domain.model.Product
import kotlinx.coroutines.flow.first

// One-shot snapshot of the products the daily notification talks about (same rule as the inventory filter).
class GetExpiringProductsUseCase(private val getInventory: GetInventoryUseCase) {
    suspend operator fun invoke(): List<Product> =
        getInventory(InventoryFilter(status = ExpirationStatus.EXPIRING_SOON)).first().map { it.product }
}
