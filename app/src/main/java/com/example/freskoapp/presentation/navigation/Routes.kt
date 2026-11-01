package com.example.freskoapp.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavBackStackEntry
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.presentation.common.ProductRemoval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The entry's own handle (args + results other screens put on it). createSavedStateHandle() would give each
// ViewModel a separate handle that never sees those results.
fun CreationExtras.navEntrySavedStateHandle(): SavedStateHandle =
    (checkNotNull(this[VIEW_MODEL_STORE_OWNER_KEY]) as NavBackStackEntry).savedStateHandle

const val PRODUCT_ID_ARG = "productId"
const val PRODUCT_DETAIL_ROUTE = "product/{productId}"

fun productDetailRoute(id: Long) = "product/$id"

// Same form for registering and editing: productId is optional, -1 means "new".
const val ADD_PRODUCT_ROUTE = "add_product?productId={productId}"
const val NO_PRODUCT_ID = -1L

fun addProductRoute() = "add_product"

fun editProductRoute(id: Long) = "add_product?productId=$id"

// Result of the detail screen, kept in the Inventory entry's SavedStateHandle (survives rotation and process death).
const val REMOVAL_ID_KEY = "removal_id"
const val REMOVAL_NAME_KEY = "removal_name"
const val REMOVAL_STATUS_KEY = "removal_status"
const val REMOVAL_UNITS_KEY = "removal_units"
const val REMOVAL_CLOSED_KEY = "removal_closed"
const val REMOVAL_EVENT_ID_KEY = "removal_event_id"

// The id is written last because it is the trigger observed by removalFlow().
fun SavedStateHandle.putRemoval(removal: ProductRemoval) {
    this[REMOVAL_NAME_KEY] = removal.name
    this[REMOVAL_STATUS_KEY] = removal.status.name
    this[REMOVAL_UNITS_KEY] = removal.units
    this[REMOVAL_CLOSED_KEY] = removal.closed
    this[REMOVAL_EVENT_ID_KEY] = removal.eventId
    this[REMOVAL_ID_KEY] = removal.id
}

private fun SavedStateHandle.readRemoval(id: Long) = ProductRemoval(
    id,
    get<String>(REMOVAL_NAME_KEY) ?: "",
    ProductStatus.valueOf(get<String>(REMOVAL_STATUS_KEY)!!),
    get<Int>(REMOVAL_UNITS_KEY) ?: 1,
    get<Boolean>(REMOVAL_CLOSED_KEY) ?: true,
    get<Long>(REMOVAL_EVENT_ID_KEY) ?: 0, // 0 matches no event: undo still restores the product
)

fun SavedStateHandle.removalFlow(): Flow<ProductRemoval?> =
    getStateFlow<Long?>(REMOVAL_ID_KEY, null).map { id -> id?.let { readRemoval(it) } }

// Single-use token: returns the pending removal and clears it in the same step, so a second call gets null.
fun SavedStateHandle.takeRemoval(): ProductRemoval? {
    val id = get<Long?>(REMOVAL_ID_KEY) ?: return null
    return readRemoval(id).also { clearRemoval() }
}

fun SavedStateHandle.clearRemoval() {
    // Set to null instead of remove(): remove() detaches the StateFlow from the handle, so later removals would not arrive.
    this[REMOVAL_ID_KEY] = null as Long?
    remove<String>(REMOVAL_NAME_KEY)
    remove<String>(REMOVAL_STATUS_KEY)
    remove<Int>(REMOVAL_UNITS_KEY)
    remove<Boolean>(REMOVAL_CLOSED_KEY)
    remove<Long>(REMOVAL_EVENT_ID_KEY)
}

// One-shot notice for the Inventory: the product just registered was added to an existing lot.
const val LOT_MERGED_KEY = "lot_merged"

fun SavedStateHandle.putLotMerged() { this[LOT_MERGED_KEY] = true }

fun SavedStateHandle.lotMergedFlow(): Flow<Boolean> = getStateFlow(LOT_MERGED_KEY, false)

fun SavedStateHandle.clearLotMerged() { this[LOT_MERGED_KEY] = false }

// One-shot notice for the screen below the edit form: the product vanished or was closed while editing.
const val PRODUCT_UNAVAILABLE_KEY = "product_unavailable"

fun SavedStateHandle.putProductUnavailable() { this[PRODUCT_UNAVAILABLE_KEY] = true }

fun SavedStateHandle.productUnavailableFlow(): Flow<Boolean> = getStateFlow(PRODUCT_UNAVAILABLE_KEY, false)

fun SavedStateHandle.clearProductUnavailable() { this[PRODUCT_UNAVAILABLE_KEY] = false }

const val SCANNER_ROUTE = "scanner"

// Single-use result of the scanner, kept in the form entry's SavedStateHandle until the form consumes it.
const val SCANNED_BARCODE_KEY = "scanned_barcode"

fun SavedStateHandle.putScannedBarcode(barcode: String) { this[SCANNED_BARCODE_KEY] = barcode }

fun SavedStateHandle.scannedBarcodeFlow(): Flow<String?> = getStateFlow<String?>(SCANNED_BARCODE_KEY, null)

fun SavedStateHandle.clearScannedBarcode() { this[SCANNED_BARCODE_KEY] = null as String? }

// One-shot notice for the form: the scanner could not open a camera.
const val CAMERA_UNAVAILABLE_KEY = "camera_unavailable"

fun SavedStateHandle.putCameraUnavailable() { this[CAMERA_UNAVAILABLE_KEY] = true }

fun SavedStateHandle.cameraUnavailableFlow(): Flow<Boolean> = getStateFlow(CAMERA_UNAVAILABLE_KEY, false)

fun SavedStateHandle.clearCameraUnavailable() { this[CAMERA_UNAVAILABLE_KEY] = false }
