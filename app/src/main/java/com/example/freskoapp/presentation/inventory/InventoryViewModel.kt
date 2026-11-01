package com.example.freskoapp.presentation.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.freskoapp.presentation.navigation.navEntrySavedStateHandle
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
import com.example.freskoapp.domain.usecase.RestoreUnitsUseCase
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.navigation.INVENTORY_STATUS_ARG
import com.example.freskoapp.presentation.navigation.clearLotMerged
import com.example.freskoapp.presentation.navigation.clearRemoval
import com.example.freskoapp.presentation.navigation.lotMergedFlow
import com.example.freskoapp.presentation.navigation.removalFlow
import com.example.freskoapp.presentation.navigation.takeRemoval
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    getInventory: GetInventoryUseCase,
    private val settings: SettingsRepository,
    private val restoreUnits: RestoreUnitsUseCase,
    // Must be the handle Navigation gives this entry: the detail screen posts its result into it.
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    initialStatus: ExpirationStatus? = null,
) : ViewModel() {
    private val _filter = MutableStateFlow(InventoryFilter(status = initialStatus))
    val filter: StateFlow<InventoryFilter> = _filter

    // Bumped by "Reintentar" to restart the query after a read error.
    private val retries = MutableStateFlow(0)

    // Loading until Room delivers the first list (skeleton instead of flashing "empty"); Error if the read fails.
    @OptIn(ExperimentalCoroutinesApi::class)
    val content: StateFlow<InventoryContent> = combine(_filter, retries) { filter, _ -> filter }
        .flatMapLatest { filter ->
            getInventory(filter)
                .map<List<InventoryItem>, InventoryContent> { InventoryContent.Loaded(it) }
                .catch { emit(InventoryContent.Error) } // the data stays on disk; the screen offers a retry
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryContent.Loading)

    fun retry() = retries.update { it + 1 }

    // Opened from the expiration alert (deep link with a status): the list explains why it is filtered.
    val openedFromAlert: Boolean = initialStatus != null

    // Starts true so the permission card doesn't flash before DataStore emits.
    val notificationPromptDismissed: StateFlow<Boolean> = settings.notificationPromptDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Last product marked as consumed/discarded from the detail screen, pending its "Deshacer" snackbar.
    val pendingRemoval: StateFlow<ProductRemoval?> = savedStateHandle.removalFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onRemovalShown() = savedStateHandle.clearRemoval()

    val lotMerged: StateFlow<Boolean> = savedStateHandle.lotMergedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onLotMergedShown() = savedStateHandle.clearLotMerged()

    // Consumes the pending removal first, so calling it again (double tap, recomposition) does nothing.
    fun undoRemoval() {
        val removal = savedStateHandle.takeRemoval() ?: return
        viewModelScope.launch { restoreUnits(removal.id, removal.units, removal.status, removal.closed, removal.eventId) }
    }

    // Selecting the active chip again clears that dimension.
    fun onStatusSelected(status: ExpirationStatus) =
        _filter.update { it.copy(status = status.takeIf { s -> s != it.status }) }

    // "Todos": no status filter.
    fun onAllStatusesSelected() = _filter.update { it.copy(status = null) }

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
                val savedStateHandle = navEntrySavedStateHandle()
                // Set by the notification deep link (fresko://inventory?status=...).
                val initialStatus = savedStateHandle.get<String>(INVENTORY_STATUS_ARG)
                    ?.let { runCatching { ExpirationStatus.valueOf(it) }.getOrNull() }
                InventoryViewModel(
                    app.getInventoryUseCase(),
                    app.settingsRepository,
                    app.restoreUnitsUseCase(),
                    savedStateHandle,
                    initialStatus,
                )
            }
        }
    }
}

sealed interface InventoryContent {
    data object Loading : InventoryContent
    data class Loaded(val items: List<InventoryItem>) : InventoryContent
    data object Error : InventoryContent
}
