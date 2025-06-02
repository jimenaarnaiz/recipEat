package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.repository.IngredienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IngredientesViewModel(private val ingredienteRepository: IngredienteRepository) : ViewModel() {

    private val _ingredientesSugeridos = MutableStateFlow<List<IngredienteSimple>>(emptyList())
    val ingredientesSugeridos: MutableStateFlow<List<IngredienteSimple>> = _ingredientesSugeridos

    //ingredientes seleccionados
    private val _ingredientes = MutableStateFlow<List<IngredienteSimple>>(emptyList())
    val ingredientes: StateFlow<List<IngredienteSimple>> = _ingredientes

    //ingredientes seleccionados
    private val _ingredientesValidos = MutableStateFlow<List<IngredienteSimple>>(emptyList())
    val ingredientesValidos: StateFlow<List<IngredienteSimple>> = _ingredientesValidos


    // Limpiar la lista de ingredientes sugeridos en la busqueda by ingredients
    fun clearIngredientesSugeridos() {
        _ingredientesSugeridos.value = emptyList()
    }

    fun addIngredient(ingrediente: IngredienteSimple) {
        _ingredientes.value += ingrediente
    }

    fun removeIngredient(ingrediente: IngredienteSimple) {
        _ingredientes.value -= ingrediente
    }

    fun clearIngredientes() {
        // Limpiar la lista de ingredientes seleccionados
        _ingredientes.value = emptyList()
    }


    fun buscarIngredientes(terminoBusqueda: String) {
        viewModelScope.launch {
            try {
                val resultados = ingredienteRepository.buscarIngredientes(terminoBusqueda)
                _ingredientesSugeridos.value = resultados
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al buscar ingredientes", e)
                _ingredientesSugeridos.value = emptyList()
            }
        }
    }

    fun loadIngredientsFromFirebase() {
        viewModelScope.launch {
            try {
                val ingredientes = ingredienteRepository.loadIngredientsFromFirebase()
                _ingredientesValidos.value = ingredientes
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al cargar ingredientes válidos", e)
            }
        }
    }








}

