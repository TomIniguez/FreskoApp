package com.example.freskoapp.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.presentation.common.ProductRemoval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val PRODUCT_ID_ARG = "productId"
const val PRODUCT_DETAIL_ROUTE = "product/{productId}"

fun productDetailRoute(id: Long) = "product/$id"

// Result of the detail screen, kept in the Inventory entry's SavedStateHandle (survives rotation and process death).
const val REMOVAL_ID_KEY = "removal_id"
const val REMOVAL_NAME_KEY = "removal_name"
const val REMOVAL_STATUS_KEY = "removal_status"
const val REMOVAL_UNITS_KEY = "removal_units"
const val REMOVAL_CLOSED_KEY = "removal_closed"

// The id is written last because it is the trigger observed by removalFlow().
fun SavedStateHandle.putRemoval(removal: ProductRemoval) {
    this[REMOVAL_NAME_KEY] = removal.name
    this[REMOVAL_STATUS_KEY] = removal.status.name
    this[REMOVAL_UNITS_KEY] = removal.units
    this[REMOVAL_CLOSED_KEY] = removal.closed
    this[REMOVAL_ID_KEY] = removal.id
}

fun SavedStateHandle.removalFlow(): Flow<ProductRemoval?> =
    getStateFlow<Long?>(REMOVAL_ID_KEY, null).map { id ->
        id?.let {
            ProductRemoval(
                it,
                get<String>(REMOVAL_NAME_KEY) ?: "",
                ProductStatus.valueOf(get<String>(REMOVAL_STATUS_KEY)!!),
                get<Int>(REMOVAL_UNITS_KEY) ?: 1,
                get<Boolean>(REMOVAL_CLOSED_KEY) ?: true,
            )
        }
    }

fun SavedStateHandle.clearRemoval() {
    // Set to null instead of remove(): remove() detaches the StateFlow from the handle, so later removals would not arrive.
    this[REMOVAL_ID_KEY] = null as Long?
    remove<String>(REMOVAL_NAME_KEY)
    remove<String>(REMOVAL_STATUS_KEY)
    remove<Int>(REMOVAL_UNITS_KEY)
    remove<Boolean>(REMOVAL_CLOSED_KEY)
}
