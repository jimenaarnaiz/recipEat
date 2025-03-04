package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.api.RetrofitClient
import com.example.recipeat.data.model.mapApiRecetaToReceta
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RecetasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Obtener recetas aleatorias y guardarlas en Firebase
    fun guardarRecetasHome(uid: String) {
        viewModelScope.launch {
            try {
                val response =
                    RetrofitClient.api.obtenerRecetasRandom() // Número de recetas a obtener
                response.recipes.forEach { apiReceta ->
                    val receta = mapApiRecetaToReceta(apiReceta, uid)
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

}