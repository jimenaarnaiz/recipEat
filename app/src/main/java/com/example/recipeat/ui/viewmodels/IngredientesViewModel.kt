package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.api.RetrofitClient
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.IngredienteSimple
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IngredientesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _ingredientesSugeridos = MutableStateFlow<List<IngredienteSimple>>(emptyList())
    val ingredientesSugeridos: MutableStateFlow<List<IngredienteSimple>> = _ingredientesSugeridos

    //ingredientes seleccionados
    private val _ingredientes = MutableStateFlow<List<IngredienteSimple>>(emptyList())
    val ingredientes: StateFlow<List<IngredienteSimple>> = _ingredientes

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


    fun extraerIngredientesYGuardar() {
        val recetasCollection = db.collection("recetas")
        val ingredientesCollection = db.collection("ingredientes")

        // Verificar cuántos documentos hay en "ingredientes" para numerar en orden
        ingredientesCollection.get()
            .addOnSuccessListener { ingredientesSnapshot ->
                if (!ingredientesSnapshot.isEmpty) {
                    Log.d("Firestore", "La colección 'ingredientes' ya contiene datos. No se ejecutará nuevamente.")
                    return@addOnSuccessListener // Salimos si ya hay datos
                }

                // Obtener todas las recetas
                recetasCollection.get()
                    .addOnSuccessListener { recetasSnapshot ->
                        val ingredientesSet = mutableSetOf<IngredienteSimple>()

                        // Extraer los ingredientes sin duplicados
                        for (document in recetasSnapshot.documents) {
                            val ingredientsList = document.get("ingredients") as? List<Map<String, Any>> ?: emptyList()

                            for (ingredient in ingredientsList) {
                                val name = ingredient["name"] as? String ?: ""
                                val image = ingredient["image"] as? String ?: ""

                                // Filtrar ingredientes cuyo nombre empieza con "add " o "all "
                                if (name.isNotEmpty() && !name.startsWith("add ", ignoreCase = true) && !name.startsWith("all ", ignoreCase = true)) {
                                    ingredientesSet.add(IngredienteSimple(name, image))
                                }
                            }
                        }

                        // Si no hay ingredientes, terminamos aquí
                        if (ingredientesSet.isEmpty()) {
                            Log.d("Firestore", "No se encontraron ingredientes en las recetas.")
                            return@addOnSuccessListener
                        }

                        // Obtener el número de documentos actuales (0 si la colección no existe aún)
                        val startingIndex = ingredientesSnapshot.size() + 1

                        // Insertar los ingredientes en la colección con numeración ordenada
                        ingredientesSet.sortedBy { it.name }.forEachIndexed { index, ingrediente ->
                            val ingredienteData = mapOf(
                                "name" to ingrediente.name,
                                "image" to ingrediente.image
                            )

                            val docId = (startingIndex + index).toString() // Mantiene orden sin saltos

                            ingredientesCollection.document(docId).set(ingredienteData)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Ingrediente ${ingrediente.name} guardado correctamente con ID $docId")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error guardando ingrediente ${ingrediente.name}", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error obteniendo recetas", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error verificando colección de ingredientes", e)
            }
    }


    // Métdo para buscar los ingredientes en Firebase
    fun buscarIngredientes(terminoBusqueda: String) {
        val ingredientesCollection = db.collection("ingredientes")

        // Filtrar ingredientes que contienen el término de búsqueda en su nombre
        ingredientesCollection
            .whereGreaterThanOrEqualTo("name", terminoBusqueda)
            .whereLessThanOrEqualTo("name", terminoBusqueda + "\uF8FF") // Búsqueda por prefijo
            .limit(9) // Limitar los resultados a 9
            .get()
            .addOnSuccessListener { querySnapshot ->
                val ingredientesEncontrados = mutableListOf<IngredienteSimple>()

                for (document in querySnapshot.documents) {
                    val nombre = document.getString("name") ?: ""
                    val imagen = document.getString("image") ?: ""
                    // Crear el objeto Ingrediente y añadirlo a la lista
                    if (nombre.contains(terminoBusqueda, ignoreCase = true)) {
                        ingredientesEncontrados.add(IngredienteSimple(nombre, imagen )
                        )
                    }
                }

                // Actualiza el StateFlow con los ingredientes encontrados
                _ingredientesSugeridos.value = ingredientesEncontrados
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al buscar ingredientes", e)
                // En caso de error, puedes asignar una lista vacía o manejar el error de alguna manera
                _ingredientesSugeridos.value = emptyList()
            }
    }







    //API

    // Buscar ingredientes
//    fun buscarIngredientes(ingrediente: String) {
//        viewModelScope.launch {
//            try {
//                val response = RetrofitClient.api.buscarIngredientesAutocompletado(ingrediente)
//                _ingredientesSugeridos.value = response
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
}

