package com.example.freskoapp.presentation.addproduct

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.debugLog
import com.example.freskoapp.domain.model.AddResult
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.model.UpdateResult
import com.example.freskoapp.domain.usecase.AddProductUseCase
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.SearchProductByBarcodeUseCase
import com.example.freskoapp.domain.usecase.UpdateProductUseCase
import com.example.freskoapp.presentation.navigation.NO_PRODUCT_ID
import com.example.freskoapp.presentation.navigation.PRODUCT_ID_ARG
import com.example.freskoapp.presentation.navigation.cameraUnavailableFlow
import com.example.freskoapp.presentation.navigation.clearCameraUnavailable
import com.example.freskoapp.presentation.navigation.putCameraUnavailable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// One form for both modes: with a productId it preloads that product and saves through UpdateProductUseCase.
class AddProductViewModel(
    private val addProduct: AddProductUseCase,
    private val updateProduct: UpdateProductUseCase,
    getDetail: GetProductDetailUseCase,
    private val searchByBarcode: SearchProductByBarcodeUseCase,
    private val productId: Long? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _state = MutableStateFlow(AddProductUiState(isEditing = productId != null))
    val state: StateFlow<AddProductUiState> = _state.asStateFlow()

    val cameraUnavailableNotice: StateFlow<Boolean> = savedStateHandle.cameraUnavailableFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onCameraUnavailableShown() = savedStateHandle.clearCameraUnavailable()

    private var lookupJob: Job? = null
    private var scanCount = 0 // debug log only: shows whether one scan is delivered more than once

    init {
        if (productId != null) viewModelScope.launch {
            val product = getDetail(productId).first()?.product?.takeIf { it.status == ProductStatus.ACTIVE }
            _state.update {
                if (product == null) it.copy(notAvailable = true)
                else it.copy(
                    name = product.name,
                    quantity = product.quantity.toString(),
                    expirationDate = product.expirationDate,
                    location = product.location,
                )
            }
        }
    }

    // Both called by the route, which takes the scanner's result from the back stack entry (see FreskoNavHost).
    fun onBarcodeScanned(barcode: String) {
        debugLog("onBarcodeScanned #${++scanCount} '$barcode' (editing=${productId != null})")
        if (productId == null) lookUp(barcode)
    }

    // Copied into this ViewModel's own handle so a notice not yet shown survives process death.
    fun onCameraUnavailable() = savedStateHandle.putCameraUnavailable()

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onQuantityChange(quantity: String) = _state.update { it.copy(quantity = quantity.filter(Char::isDigit)) }
    fun onDateSelected(date: LocalDate) = _state.update { it.copy(expirationDate = date) }
    fun onLocationChange(location: Location) = _state.update { it.copy(location = location) }

    fun retry() {
        _state.value.barcode?.let(::lookUp)
    }

    private fun lookUp(barcode: String) {
        // A newer scan wins: the older answer must not land on top of it.
        if (lookupJob?.isActive == true) debugLog("cancelling the lookup for '${_state.value.barcode}', replaced by '$barcode'")
        lookupJob?.cancel()
        debugLog("lookup started for '$barcode'")
        _state.update { it.copy(barcode = barcode, lookup = LookupState.Loading) }
        lookupJob = viewModelScope.launch {
            val result = searchByBarcode(barcode)
            debugLog("lookup result for '$barcode': $result")
            _state.update {
                when (result) {
                    // Only fill a blank name: never overwrite what the user already typed.
                    is ProductInfoResult.Found -> it.copy(
                        name = it.name.ifBlank { result.info.name },
                        lookup = LookupState.Found(result.info.name),
                    )
                    ProductInfoResult.NotFound -> it.copy(lookup = LookupState.Failure(LookupFailure.NOT_FOUND))
                    ProductInfoResult.Timeout -> it.copy(lookup = LookupState.Failure(LookupFailure.TIMEOUT))
                    ProductInfoResult.ServerError -> it.copy(lookup = LookupState.Failure(LookupFailure.SERVER_ERROR))
                    ProductInfoResult.Offline -> it.copy(lookup = LookupState.Failure(LookupFailure.OFFLINE))
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        val date = s.expirationDate
        if (!s.canSave || date == null) return
        // Set before launching so a fast double tap can't save twice.
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (productId != null) {
                val result = updateProduct(productId, s.name, date, s.quantity.toInt(), s.location)
                _state.update {
                    if (result == UpdateResult.UPDATED) it.copy(saved = true) else it.copy(isSaving = false, notAvailable = true)
                }
            } else {
                val result = addProduct(Product(name = s.name, expirationDate = date, quantity = s.quantity.toInt(), location = s.location))
                _state.update { it.copy(saved = true, merged = result == AddResult.MERGED) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                val savedStateHandle = createSavedStateHandle()
                val id = savedStateHandle.get<Long>(PRODUCT_ID_ARG)?.takeIf { it != NO_PRODUCT_ID }
                AddProductViewModel(
                    AddProductUseCase(app.productRepository),
                    UpdateProductUseCase(app.productRepository),
                    app.getProductDetailUseCase(),
                    app.searchProductByBarcodeUseCase(),
                    id,
                    savedStateHandle,
                )
            }
        }
    }
}
