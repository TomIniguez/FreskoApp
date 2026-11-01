package com.example.freskoapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.model.PilotSummary
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.usecase.DEFAULT_WARNING_DAYS
import com.example.freskoapp.domain.usecase.SetWarningDaysUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    settings: SettingsRepository,
    private val setWarningDays: SetWarningDaysUseCase,
    private val events: EventRepository,
) : ViewModel() {
    val warningDays: StateFlow<Int> = settings.warningDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_WARNING_DAYS)

    val pilotSummary: StateFlow<PilotSummary> = combine(events.observeAll(), settings.warningDays, PilotSummary::from)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PilotSummary())

    fun onWarningDaysSelected(days: Int) {
        viewModelScope.launch { setWarningDays(days) }
    }

    suspend fun pilotCsv(): String = events.observeAll().first().toCsv()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                SettingsViewModel(app.settingsRepository, SetWarningDaysUseCase(app.settingsRepository), app.eventRepository)
            }
        }
    }
}

// Every value is a number or an enum name, so nothing needs quoting. Empty cell = not applicable (null).
internal fun List<ProductEvent>.toCsv(): String = buildString {
    appendLine("id,productId,type,units,daysToExpiry,method,durationMs,timestamp")
    for (e in this@toCsv) {
        appendLine(
            listOf(e.id, e.productId, e.type, e.units, e.daysToExpiry, e.method, e.durationMs, e.timestamp)
                .joinToString(",") { it?.toString() ?: "" }
        )
    }
}
