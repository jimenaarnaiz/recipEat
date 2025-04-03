package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FiltrosViewModel : ViewModel() {

    // Almacenamos los filtros aplicados
    var maxTiempo = mutableStateOf<Int?>(null)
    var maxIngredientes = mutableStateOf<Int?>(null)
    var maxFaltantes = mutableStateOf<Int?>(null)
    var maxPasos = mutableStateOf<Int?>(null)
    var tipoPlato = mutableStateOf<String?>(null)
    // Set para almacenar múltiples opciones
    var tipoDieta = mutableStateOf<Set<String>?>(emptySet())

    var orden = mutableStateOf("Default")


    // Función para aplicar los filtros
    fun aplicarFiltros(
        tiempo: Int?,
        ingredientes: Int?,
        faltantes: Int?,
        pasos: Int?,
        plato: String?,
        dietas: Set<String>?
    ) {
        maxTiempo.value = tiempo
        maxIngredientes.value = ingredientes
        maxFaltantes.value = faltantes
        maxPasos.value = pasos
        tipoPlato.value = plato
        tipoDieta.value = dietas
    }

    // Función para restablecer los filtros
    fun restablecerFiltros() {
        maxTiempo.value = null
        maxIngredientes.value = null
        maxFaltantes.value = null
        maxPasos.value = null
        tipoPlato.value = null
        tipoDieta.value = emptySet()

        Log.d("FiltrosViewModel", "Restableciendo filtros...")
    }


    fun setOrden(nuevoOrden: String) {
        orden.value = nuevoOrden
        Log.d("FiltrosViewModel", "Estableciendo orden a $nuevoOrden...")
    }

    fun restablecerOrden() {
        orden.value = "Default"
        Log.d("FiltrosViewModel", "Restableciendo orden a Default...")
    }
}
