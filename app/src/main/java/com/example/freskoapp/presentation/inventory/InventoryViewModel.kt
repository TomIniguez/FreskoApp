package com.example.freskoapp.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.InventoryFilter
import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.usecase.GetInventoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class InventoryViewModel(getInventory: GetInventoryUseCase) : ViewModel() {
    private val _filter = MutableStateFlow(InventoryFilter())
    val filter: StateFlow<InventoryFilter> = _filter

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<InventoryItem>> = _filter
        .flatMapLatest { getInventory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Selecting the active chip again clears that dimension.
    fun onStatusSelected(status: ExpirationStatus) =
        _filter.update { it.copy(status = status.takeIf { s -> s != it.status }) }

    fun onLocationSelected(location: Location) =
        _filter.update { it.copy(location = location.takeIf { l -> l != it.location }) }

    fun clearFilters() = _filter.update { InventoryFilter() }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                InventoryViewModel(GetInventoryUseCase(app.productRepository))
            }
        }
    }
}
