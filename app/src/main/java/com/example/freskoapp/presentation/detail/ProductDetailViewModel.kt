package com.example.freskoapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.freskoapp.presentation.navigation.navEntrySavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.CloseUnitsUseCase
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.navigation.PRODUCT_ID_ARG
import com.example.freskoapp.presentation.navigation.clearProductUnavailable
import com.example.freskoapp.presentation.navigation.productUnavailableFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productId: Long,
    getDetail: GetProductDetailUseCase,
    private val closeUnits: CloseUnitsUseCase,
    // Must be the handle Navigation gives this entry: the edit form posts its notice into it.
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    // Local state of the mark action; the product itself comes reactively from the repository.
    private data class Action(val isProcessing: Boolean = false, val removal: ProductRemoval? = null)

    private val action = MutableStateFlow(Action())

    val state: StateFlow<ProductDetailUiState> = combine(getDetail(productId), action) { item, action ->
        ProductDetailUiState(isLoading = false, item = item, isProcessing = action.isProcessing, removal = action.removal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    val unavailableNotice: StateFlow<Boolean> = savedStateHandle.productUnavailableFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onUnavailableShown() = savedStateHandle.clearProductUnavailable()

    fun markConsumed(units: Int) = mark(ProductStatus.CONSUMED, units)

    // Discarding always takes every unit; only "Consumido" lets you pick how many.
    fun markDiscarded() {
        state.value.item?.let { mark(ProductStatus.DISCARDED, it.product.quantity) }
    }

    private fun mark(status: ProductStatus, units: Int) {
        val current = state.value
        val item = current.item ?: return
        if (current.isLoading || current.isProcessing || current.removal != null) return
        if (item.product.status != ProductStatus.ACTIVE) return
        // Set before launching so a fast double tap can't mark the product twice.
        action.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val result = closeUnits(productId, units, status)
            action.update {
                if (result != null) {
                    val closed = result.product.status != ProductStatus.ACTIVE
                    Action(isProcessing = true, removal = ProductRemoval(productId, item.product.name, status, units, closed, result.eventId))
                } else Action()
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                val savedStateHandle = navEntrySavedStateHandle()
                val productId = checkNotNull(savedStateHandle.get<Long>(PRODUCT_ID_ARG))
                ProductDetailViewModel(productId, app.getProductDetailUseCase(), app.closeUnitsUseCase(), savedStateHandle)
            }
        }
    }
}
