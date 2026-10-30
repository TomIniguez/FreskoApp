package com.example.freskoapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.usecase.DEFAULT_WARNING_DAYS
import com.example.freskoapp.domain.usecase.SetWarningDaysUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    settings: SettingsRepository,
    private val setWarningDays: SetWarningDaysUseCase,
) : ViewModel() {
    val warningDays: StateFlow<Int> = settings.warningDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_WARNING_DAYS)

    fun onWarningDaysSelected(days: Int) {
        viewModelScope.launch { setWarningDays(days) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                SettingsViewModel(app.settingsRepository, SetWarningDaysUseCase(app.settingsRepository))
            }
        }
    }
}
