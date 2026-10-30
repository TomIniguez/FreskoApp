package com.example.freskoapp.presentation.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.InventoryFilter
import com.example.freskoapp.domain.model.InventoryItem
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.usecase.GetInventoryUseCase
import com.example.freskoapp.domain.usecase.RestoreProductUseCase
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.navigation.INVENTORY_STATUS_ARG
import com.example.freskoapp.presentation.navigation.clearRemoval
import com.example.freskoapp.presentation.navigation.removalFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    getInventory: GetInventoryUseCase,
    private val settings: SettingsRepository,
    private val restoreProduct: RestoreProductUseCase,
    // Must be the handle Navigation gives this entry: the detail screen posts its result into it.
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    initialStatus: ExpirationStatus? = null,
) : ViewModel() {
    private val _filter = MutableStateFlow(InventoryFilter(status = initialStatus))
    val filter: StateFlow<InventoryFilter> = _filter

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<InventoryItem>> = _filter
        .flatMapLatest { getInventory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Starts true so the permission card doesn't flash before DataStore emits.
    val notificationPromptDismissed: StateFlow<Boolean> = settings.notificationPromptDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Last product marked as consumed/discarded from the detail screen, pending its "Deshacer" snackbar.
    val pendingRemoval: StateFlow<ProductRemoval?> = savedStateHandle.removalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onRemovalShown() = savedStateHandle.clearRemoval()

    fun undoRemoval(removal: ProductRemoval) {
        viewModelScope.launch { restoreProduct(removal.id, removal.status) }
    }

    // Selecting the active chip again clears that dimension.
    fun onStatusSelected(status: ExpirationStatus) =
        _filter.update { it.copy(status = status.takeIf { s -> s != it.status }) }

    fun onLocationSelected(location: Location) =
        _filter.update { it.copy(location = location.takeIf { l -> l != it.location }) }

    fun clearFilters() = _filter.update { InventoryFilter() }

    fun dismissNotificationPrompt() {
        viewModelScope.launch { settings.dismissNotificationPrompt() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                val savedStateHandle = createSavedStateHandle()
                // Set by the notification deep link (fresko://inventory?status=...).
                val initialStatus = savedStateHandle.get<String>(INVENTORY_STATUS_ARG)
                    ?.let { runCatching { ExpirationStatus.valueOf(it) }.getOrNull() }
                InventoryViewModel(
                    app.getInventoryUseCase(),
                    app.settingsRepository,
                    RestoreProductUseCase(app.productRepository),
                    savedStateHandle,
                    initialStatus,
                )
            }
        }
    }
}
