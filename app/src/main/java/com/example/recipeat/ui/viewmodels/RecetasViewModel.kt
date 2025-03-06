package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.api.RetrofitClient
import com.example.recipeat.data.model.ApiReceta
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.Receta
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecetasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _apiRecetas = MutableStateFlow<List<ApiReceta>>(emptyList())
    val apiRecetas: StateFlow<List<ApiReceta>> = _apiRecetas

    private val _recetas = MutableLiveData<List<Receta>>(emptyList())
    val recetas: LiveData<List<Receta>> = _recetas


    // Función para mapear ApiReceta a Receta
    fun mapApiRecetaToReceta(
        apiReceta: ApiReceta,
        uid: String,
        analyzedInstructions: List<Map<String, Any>>
    ): Receta {

        // Extraer solo los valores de "step" en una lista de Strings
        val pasos = analyzedInstructions.flatMap { instruction ->
            Log.d("mapApiRecetaToReceta", "instruction: ${instruction}, steps: ${instruction["steps"].toString()}")
            val steps = instruction["steps"] as? List<Map<String, Any>> ?: emptyList()
            steps.mapNotNull { step ->
                step["step"] as? String // Extraemos solo la descripción del paso
            }
        }

        return Receta(
            id = apiReceta.id.toString(),
            title = apiReceta.title,
            image = apiReceta.image,
            ingredients = apiReceta.extendedIngredients,  // Los ingredientes ya coinciden
            steps = pasos,
            time = apiReceta.readyInMinutes,
            dishTypes = apiReceta.dishTypes,
            user = uid,  // ID del usuario que crea la receta
        )
    }


    // Obtener recetas aleatorias y guardarlas en Firebase
    fun guardarRecetasHome(uid: String) {
        viewModelScope.launch {
            try {
                val response =
                    RetrofitClient.api.obtenerRecetasRandom() // Número de recetas a obtener
                response.recipes.forEach { apiReceta ->
                    val analyzedInstructions = RetrofitClient.api.obtenerInstruccionesReceta(apiReceta.id)

                    val receta = mapApiRecetaToReceta(apiReceta, uid, analyzedInstructions)
                    val recetaData = hashMapOf(
                        "id" to receta.id,
                        "title" to receta.title,
                        "image" to receta.image,
                        "ingredients" to receta.ingredients.map {
                            hashMapOf(
                                "name" to it.name,
                                "amount" to it.amount,
                                "unit" to it.unit,
                                "image" to it.image
                            )
                        },
                        "steps" to receta.steps,
                        "time" to receta.time,
                        "dishTypes" to receta.dishTypes,
                        "user" to receta.user,
                        "usedIngredientCount" to receta.usedIngredientCount,
                    )

                    db.collection("recetas")
                        .document(receta.user)  // Guardar la receta bajo el usuario
                        .collection("recetas_aleatorias")
                        .document("${receta.id}")
                        .set(recetaData)
                        .addOnSuccessListener {
                            println("Receta guardada correctamente en Firebase")
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecetasViewModel", "Error al guardar receta en Firebase", e)
                            println("Error al guardar receta: $e")
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RecetasViewModel", "Error al obtener recetas aleatorias: ${e.message}")
            }
        }
    }

    // Obtener recetas home
    fun obtenerRecetasHome(uid: String) {
        Log.d("RecetasViewModel", "Obteniendo recetas home para el usuario $uid")

        // Usamos viewModelScope.launch para ejecutar la tarea en un hilo en segundo plano
        viewModelScope.launch {
            try {
                val docRef = db.collection("recetas").document(uid)
                    .collection("recetas_aleatorias")

                // Usamos Firebase Firestore con una corrutina para obtener los datos
                val documents = docRef.get().await() // Usamos .await() para esperar el resultado de la operación asíncrona

                // Mapeamos los documentos obtenidos a objetos Receta
                val recetasList = documents.mapNotNull { document ->
                    try {
                        Receta(
                            id = document.getString("id") ?: "",
                            title = document.getString("title") ?: "",
                            image = document.getString("image") ?: "",
                            ingredients = (document.get("ingredients") as? List<Map<String, Any>>)?.map { ing ->
                                Ingrediente(
                                    name = ing["name"] as? String ?: "",
                                    amount = (ing["amount"] as? Number)?.toDouble() ?: 0.0,
                                    unit = ing["unit"] as? String ?: "",
                                    image = ing["image"] as? String ?: ""
                                )
                            } ?: emptyList(),
                            steps = document.get("steps") as? List<String> ?: emptyList(),
                            time = (document.get("time") as? Number)?.toInt() ?: 0,
                            dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
                            user = document.getString("user") ?: "",
                            usedIngredientCount = (document.get("usedIngredientCount") as? Number)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e("RecetasViewModel", "Error al mapear receta: ${e.message}")
                        null
                    }
                }

                // Actualizamos el estado de recetas en el ViewModel
                _recetas.value = recetasList
                Log.d("RecetasViewModel", "_recetas: ${_recetas.value}")

            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener recetas: ${e.message}")
            }
        }
    }
}