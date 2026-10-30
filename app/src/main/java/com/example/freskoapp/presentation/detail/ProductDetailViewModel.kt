package com.example.freskoapp.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.MarkProductUseCase
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.navigation.PRODUCT_ID_ARG
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
    private val markProduct: MarkProductUseCase,
) : ViewModel() {
    // Local state of the mark action; the product itself comes reactively from the repository.
    private data class Action(val isProcessing: Boolean = false, val removal: ProductRemoval? = null)

    private val action = MutableStateFlow(Action())

    val state: StateFlow<ProductDetailUiState> = combine(getDetail(productId), action) { item, action ->
        ProductDetailUiState(isLoading = false, item = item, isProcessing = action.isProcessing, removal = action.removal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    fun markConsumed() = mark(ProductStatus.CONSUMED)

    fun markDiscarded() = mark(ProductStatus.DISCARDED)

    private fun mark(status: ProductStatus) {
        val current = state.value
        val item = current.item ?: return
        if (current.isLoading || current.isProcessing || current.removal != null) return
        if (item.product.status != ProductStatus.ACTIVE) return
        // Set before launching so a fast double tap can't mark the product twice.
        action.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val changed = markProduct(productId, status)
            action.update {
                if (changed) Action(isProcessing = true, removal = ProductRemoval(productId, item.product.name, status))
                else Action()
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                val productId = checkNotNull(createSavedStateHandle().get<Long>(PRODUCT_ID_ARG))
                ProductDetailViewModel(productId, app.getProductDetailUseCase(), MarkProductUseCase(app.productRepository))
            }
        }
    }
}
