package com.example.recipeat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.api.RetrofitClient
import com.example.recipeat.data.model.Ingrediente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IngredientesViewModel : ViewModel() {
    private val _ingredientesSugeridos = MutableStateFlow<List<Ingrediente>>(emptyList())
    val ingredientesSugeridos: MutableStateFlow<List<Ingrediente>> = _ingredientesSugeridos

    //ingredientes seleccionados
    private val _ingredientes = MutableStateFlow<List<Ingrediente>>(emptyList())
    val ingredientes: StateFlow<List<Ingrediente>> = _ingredientes

    fun addIngredient(ingrediente: Ingrediente) {
        _ingredientes.value += ingrediente
    }

    fun removeIngredient(ingrediente: Ingrediente) {
        _ingredientes.value -= ingrediente
    }

    // Buscar ingredientes
    fun buscarIngredientes(ingrediente: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.buscarIngredientesAutocompletado(ingrediente)
                _ingredientesSugeridos.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

