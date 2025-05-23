package com.example.recipeat.ui.viewmodels

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.IngredienteCompra
import com.example.recipeat.data.model.PlanSemanal
import com.example.recipeat.data.repository.PlanRepository
import kotlinx.coroutines.launch

class PlanViewModel(application: Application, private val planRepository: PlanRepository) : AndroidViewModel(application) {

    private val _planSemanal = MutableLiveData<PlanSemanal?>()
    val planSemanal: MutableLiveData<PlanSemanal?> = _planSemanal

    private val _listaCompra = MutableLiveData<List<IngredienteCompra>>(emptyList())
    val listaCompra: LiveData<List<IngredienteCompra>> = _listaCompra


    // Llamar a la función que genera el plan semanal inicial cuando es la primera vez (se registra) y no es lunes
    @RequiresApi(Build.VERSION_CODES.O)
    fun iniciarGeneracionPlanSemanalInicial(userId: String) {
        viewModelScope.launch {
            planRepository.iniciarGeneracionPlanSemanalInicial(userId)
        }
    }


    // Llamar a la función que obtiene la lista de la compra desde Firebase
    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerListaDeLaCompraDeFirebase(uid: String) {
        planRepository.obtenerListaDeLaCompraDeFirebase(uid) { listaCompra ->
            // Aquí actualizo la UI con la lista de compra obtenida
            _listaCompra.value = listaCompra
        }
    }

    // Llamar a la función que actualiza el estado del ingrediente en Firebase
    @RequiresApi(Build.VERSION_CODES.O)
    fun actualizarEstadoIngredienteEnFirebase(uid: String, nombreIngrediente: String, estaComprado: Boolean) {
        planRepository.actualizarEstadoIngredienteEnFirebase(uid, nombreIngrediente, estaComprado) { success ->
            if (success) {
                Log.d("PlanViewModel", "Estado del ingrediente actualizado correctamente")
            } else {
                Log.e("PlanViewModel", "Error al actualizar el estado del ingrediente")
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerPlanSemanal(userId: String) {
        viewModelScope.launch {
            val plan = planRepository.obtenerPlanSemanal(userId)
            _planSemanal.value = plan
            if (plan != null) {
                Log.d("PlanViewModel", "Plan no null: ${plan.weekMeals.values}")
            }
        }
    }







}

