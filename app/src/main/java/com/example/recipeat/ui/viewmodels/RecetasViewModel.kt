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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecetasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _apiRecetas = MutableStateFlow<List<ApiReceta>>(emptyList())
    val apiRecetas: StateFlow<List<ApiReceta>> = _apiRecetas

    private val _apiRecetasSugeridas = MutableStateFlow<List<ApiReceta>>(emptyList())
    val apiRecetasSugeridas: StateFlow<List<ApiReceta>> = _apiRecetasSugeridas

    private val _recetas = MutableLiveData<List<Receta>>(emptyList())
    val recetas: LiveData<List<Receta>> = _recetas

    private val _apiReceta = MutableLiveData<ApiReceta>()
    val apiReceta: LiveData<ApiReceta> = _apiReceta


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

    // Obtener todas las recetas home
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

    //obtener parcialmente las recetas home
    private var lastDocument: DocumentSnapshot? = null

    // Obtener recetas (inicial o paginada)
    fun obtenerRecetasHome(uid: String, limpiarLista: Boolean = true) {
        Log.d("RecetasViewModel", "Obteniendo recetas para el usuario $uid")

        // Iniciar la query de Firestore
        var query = db.collection("recetas").document(uid)
            .collection("recetas_aleatorias")
            .orderBy("title") // Ordenar por un campo
            .limit(15) // Limitar a 15 recetas

        // Si no es la primera carga, empezar después del último documento cargado
        if (lastDocument != null && !limpiarLista) {
            query = query.startAfter(lastDocument)
        }

        // Ejecutar la consulta en el hilo de background
        viewModelScope.launch {
            try {
                val documents = query.get().await()

                // Si hay documentos, actualizamos lastDocument
                if (!documents.isEmpty) {
                    lastDocument = documents.documents.last() // Guardar el último documento

                    val nuevasRecetas = documents.mapNotNull { document ->
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

                    // Actualizar la lista de recetas
                    _recetas.value = if (limpiarLista) nuevasRecetas else _recetas.value?.plus(
                        nuevasRecetas
                    )
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener recetas: ${e.message}")
            }
        }
    }

    // Buscar recetas con ingredientes
    fun buscarRecetasPorIngredientes(ingredientes: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.buscarRecetasPorIngredientes(ingredientes)
                _apiRecetas.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun buscarRecetasPorNombreAutocompletado(nombreReceta: String){
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.buscarRecetasPorNombreAutocompletado(nombreReceta)
                _apiRecetasSugeridas.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun buscarRecetasPorNombre(nombreReceta: String){
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.buscarRecetasPorNombre(nombreReceta)
                _apiRecetas.value = response.results
                Log.d("RecetasViewModel", "${_apiRecetas.value}")
            } catch (e: Exception) {
                Log.d("RecetasViewModel", "no pasa")
                e.printStackTrace()
                Log.d("RecetasViewModel", "no pasa")
            }
        }
    }

    fun obtenerDetallesReceta(idReceta: Int){
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.obtenerDetallesReceta(idReceta)
                _apiReceta.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }














}