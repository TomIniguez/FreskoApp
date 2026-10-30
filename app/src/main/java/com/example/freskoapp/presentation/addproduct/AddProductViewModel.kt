package com.example.freskoapp.presentation.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.usecase.AddProductUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddProductViewModel(private val addProduct: AddProductUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AddProductUiState())
    val state: StateFlow<AddProductUiState> = _state.asStateFlow()

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onQuantityChange(quantity: String) = _state.update { it.copy(quantity = quantity.filter(Char::isDigit)) }
    fun onDateSelected(date: LocalDate) = _state.update { it.copy(expirationDate = date) }
    fun onLocationChange(location: Location) = _state.update { it.copy(location = location) }

    fun save() {
        val s = _state.value
        val date = s.expirationDate
        if (!s.canSave || date == null) return
        // Set before launching so a fast double tap can't insert the product twice.
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            addProduct(Product(name = s.name, expirationDate = date, quantity = s.quantity.toInt(), location = s.location))
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FreskoApplication
                AddProductViewModel(AddProductUseCase(app.productRepository))
            }
        }
    }
}
