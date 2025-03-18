package com.example.recipeat.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.api.RetrofitClient
import com.example.recipeat.data.model.ApiReceta
import com.example.recipeat.data.model.CookHistory
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.SugerenciaReceta
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RecetasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

   // private val _apiRecetas = MutableStateFlow<List<ApiReceta>>(emptyList())
    //val apiRecetas: StateFlow<List<ApiReceta>> = _apiRecetas

    private val _recetasSugeridas = MutableStateFlow<List<SugerenciaReceta>>(emptyList())
    val recetasSugeridas: StateFlow<List<SugerenciaReceta>> = _recetasSugeridas

    private val _recetas = MutableLiveData<List<Receta>>(emptyList())
    val recetas: LiveData<List<Receta>> = _recetas

    private val _recetasUser = MutableLiveData<List<Receta>>(emptyList())
    val recetasUser: LiveData<List<Receta>> = _recetasUser

    private val _recetaSeleccionada = MutableLiveData<Receta>()
    val recetaSeleccionada: LiveData<Receta> = _recetaSeleccionada

    private var lastDocument: DocumentSnapshot? = null // para paginacion

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> get() = _isLoadingMore

    private val _recetasOriginales = MutableLiveData<List<Receta>>(emptyList())

    private val _esFavorito = MutableLiveData<Boolean>()
    val esFavorito: LiveData<Boolean> get() = _esFavorito


    fun verificarRecetasGuardadas(uid: String) {
        val configRef = db.collection("config").document("recetas_guardadas")

        // Verificar si las recetas ya fueron guardadas
        configRef.get().addOnSuccessListener { document ->
            if (document.exists() && document.getBoolean("ya_guardado") == true) {
                Log.d(
                    "RecetasViewModel",
                    "Las recetas ya fueron almacenadas. No se ejecuta nuevamente."
                )
            } else {
                if (!document.exists()) {
                    // Si el documento no existe, lo creamos con el valor inicial de 'ya_guardado' = false
                    configRef.set(hashMapOf("ya_guardado" to false))
                        .addOnSuccessListener {
                            Log.d(
                                "RecetasViewModel",
                                "Documento 'recetas_guardadas' creado exitosamente."
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecetasViewModel", "Error al crear documento en Firestore", e)
                        }
                }
                // Si no se han guardado, comenzamos a guardar las recetas aleatorias
                guardarRecetas(uid, configRef)
            }
        }.addOnFailureListener {
            Log.e("RecetasViewModel", "Error al verificar el estado de recetas en Firestore", it)
        }
    }

    /**
     * Guarda recetas en Firebase de la llamada random recipes de la API.
     * Se ha de ejecutar hasta obtener 500 recetas.
    */
    fun guardarRecetas(uid: String, configRef: DocumentReference) {
        viewModelScope.launch {
            try {
                delay(3000) // Delay para no sobrecargar las solicitudes
                // Obtener las recetas aleatorias
                val response = RetrofitClient.api.obtenerRecetasRandom()

                // Verificar si la respuesta contiene recetas
                if (response.recipes.isNotEmpty()) {
                    // Recorrer las recetas obtenidas
                    response.recipes.forEach { apiReceta ->
                        val recetaId = apiReceta.id.toString()

                        // Verificar si la receta ya existe en Firebase
                        db.collection("recetas").document(recetaId).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) { // Si no existe, la guardamos
                                    viewModelScope.launch {
                                        delay(3000) // Delay para no sobrecargar las solicitudes
                                        val analyzedInstructions =
                                            RetrofitClient.api.obtenerInstruccionesReceta(apiReceta.id)
                                        val receta = mapApiRecetaToReceta(apiReceta, uid, analyzedInstructions)

                                        val recetaData = hashMapOf(
                                            "id" to receta.id,
                                            "title" to receta.title,
                                            "image" to receta.image,
                                            "servings" to receta.servings,
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
                                            "user" to receta.userId,
                                            "glutenFree" to receta.glutenFree,
                                            "vegan" to receta.vegan,
                                            "vegetarian" to receta.vegetarian
                                        )

                                        // Guardar la receta en Firebase
                                        db.collection("recetas")
                                            .document(receta.id)
                                            .set(recetaData)
                                            .addOnSuccessListener {
                                                Log.d("RecetasViewModel", "Receta $recetaId guardada correctamente en Firebase")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("RecetasViewModel", "Error al guardar receta en Firebase", e)
                                            }
                                    }
                                } else {
                                    Log.d("RecetasViewModel", "Receta con ID $recetaId ya existe en Firebase, no se guarda.")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("RecetasViewModel", "Error al verificar receta en Firebase", e)
                            }
                    }

                    // Verificar cuántas recetas hay actualmente en Firebase
                    db.collection("recetas").get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.size() >= 500) { // Si hay 500 o más recetas
                                // Actualizar el estado de 'ya_guardado' en la configuración
                                configRef.update("ya_guardado", true)
                                    .addOnSuccessListener {
                                        Log.d("RecetasViewModel", "Recetas guardadas y estado actualizado a 'true'.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("RecetasViewModel", "Error al actualizar el estado en Firestore", e)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecetasViewModel", "Error al contar las recetas en Firebase", e)
                        }

                } else {
                    Log.d("RecetasViewModel", "No se obtuvieron recetas aleatorias.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RecetasViewModel", "Error al obtener recetas aleatorias: ${e.message}")
            }
        }
    }




//    fun guardarRecetas(uid: String, configRef: DocumentReference) {
//        viewModelScope.launch {
//            try {
//                var count = 0
//                while (count < maxEjecuciones) {  // TODO 5 veces (500 recipes)
//                    val response =
//                        RetrofitClient.api.obtenerRecetasRandom() // Obtiene las recetas aleatorias
//
//                    // Verificar que la respuesta sea válida y no esté vacía
//                    if (response.recipes.isNotEmpty()) {
//                        response.recipes.forEach { apiReceta ->
//                            val analyzedInstructions =
//                                RetrofitClient.api.obtenerInstruccionesReceta(apiReceta.id)
//
//                            val receta = mapApiRecetaToReceta(apiReceta, uid, analyzedInstructions)
//                            val recetaData = hashMapOf(
//                                "id" to receta.id,
//                                "title" to receta.title,
//                                "image" to receta.image,
//                                "servings" to receta.servings,
//                                "ingredients" to receta.ingredients.map {
//                                    hashMapOf(
//                                        "name" to it.name,
//                                        "amount" to it.amount,
//                                        "unit" to it.unit,
//                                        "image" to it.image
//                                    )
//                                },
//                                "steps" to receta.steps,
//                                "time" to receta.time,
//                                "dishTypes" to receta.dishTypes,
//                                "user" to receta.userId,
//                                "glutenFree" to receta.glutenFree,
//                                "vegan" to receta.vegan,
//                                "vegetarian" to receta.vegetarian
//                            )
//
//                            db.collection("recetas")
//                                .document("${receta.id}")
//                                .set(recetaData)
//                                .addOnSuccessListener {
//                                    println("Receta guardada correctamente en Firebase")
//                                }
//                                .addOnFailureListener { e ->
//                                    Log.e(
//                                        "RecetasViewModel",
//                                        "Error al guardar receta en Firebase",
//                                        e
//                                    )
//                                    println("Error al guardar receta: $e")
//                                }
//                        }
//                    }
//
//                    count++  // Incrementar el contador para repetir hasta x veces
//                }
//
//                // Una vez que se han guardado las recetas, actualizar el valor de 'ya_guardado'
//                configRef.update("ya_guardado", true)
//                    .addOnSuccessListener {
//                        Log.d("RecetasViewModel", "Recetas guardadas y estado actualizado.")
//                    }
//                    .addOnFailureListener {
//                        Log.e("RecetasViewModel", "Error al actualizar el estado en Firestore", it)
//                    }
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e("RecetasViewModel", "Error al obtener recetas aleatorias: ${e.message}")
//            }
//        }
//    }

    // Genera el ID único alfanumérico de la receta creada por el usuario
    fun generateRecipeId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    // Crea y guarda la receta del usuario en Firebase
    fun createRecipe(
        recipeName: String,
        image: String?,
        servings: Int,
        ingredients: List<Ingrediente>,
        steps: List<String>,
        time: Int,
        dishTypes: List<String>,
        userId: String, // ID del usuario que ha creado la receta
        glutenFree: Boolean,
        vegan: Boolean,
        vegetarian: Boolean
    ) {
        val db = FirebaseFirestore.getInstance()

        // Generar un ID único para la receta
        val recipeId = db.collection("my_recipes").document(userId).collection("recipes").document().id

        val newRecipe = Receta(
            id = recipeId,
            title = recipeName,
            image = image,
            servings = servings,
            ingredients = ingredients,
            steps = steps,
            time = time,
            userId = userId,
            dishTypes = dishTypes,
            usedIngredientCount = ingredients.size,
            glutenFree = glutenFree,
            vegan = vegan,
            vegetarian = vegetarian,
            date = System.currentTimeMillis(), // Fecha de creación,
            missingIngredientCount = 0,
            unusedIngredientCount = 0,
            unusedIngredients = emptyList()
        )

        // Guardar la receta directamente en `my_recipes/{uid}/{recipeId}`
        val userRecipesRef = db.collection("my_recipes").document(userId) // Documento del usuario
            .collection("recipes").document(recipeId) // Cada receta es un documento en `my_recipes/{uid}/`

        userRecipesRef.set(newRecipe)
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe successfully added!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding recipe", e)
            }
    }


    // Función para obtener las recetas del usuario
    // TODO PAGINACIÓN
    fun getRecetasUser(uid: String) {
        db.collection("my_recipes").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val recetasList = document.data?.values?.mapNotNull {
                        it as? Map<*, *> // Convertir los valores en mapas
                    }?.mapNotNull { recetaMap ->
                        try {
                            Receta(
                                id = recetaMap["id"] as String,
                                title = recetaMap["title"] as String,
                                image = recetaMap["image"] as? String,
                                servings = (recetaMap["servings"] as Long).toInt(),
                                ingredients = recetaMap["ingredients"] as List<Ingrediente>,
                                steps = recetaMap["steps"] as List<String>,
                                time = (recetaMap["time"] as Long).toInt(),
                                userId = recetaMap["userId"] as String,
                                dishTypes = recetaMap["dishTypes"] as List<String>,
                                usedIngredientCount = (recetaMap["usedIngredientCount"] as Long).toInt(),
                                glutenFree = recetaMap["glutenFree"] as Boolean,
                                vegan = recetaMap["vegan"] as Boolean,
                                vegetarian = recetaMap["vegetarian"] as Boolean,
                                date = recetaMap["date"] as Long,
                                missingIngredientCount = recetaMap["missingIngredientCount"] as Int,
                                unusedIngredientCount = recetaMap["unusedIngredientCount"] as Int,
                                unusedIngredients = recetaMap["unusedIngredients"] as List<IngredienteSimple>
                            )
                        } catch (e: Exception) {
                            Log.w("Firestore", "Error parsing recipe", e)
                            null
                        }
                    } ?: emptyList()

                    _recetasUser.value = recetasList // Guardar en la variable
                } else {
                    _recetasUser.value = emptyList() // No hay recetas
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching recipes", e)
                _recetasUser.value = emptyList() // En caso de error, retorna una lista vacía
            }
    }



    // Función para mapear ApiReceta a Receta
    fun mapApiRecetaToReceta(
        apiReceta: ApiReceta,
        uid: String,
        analyzedInstructions: List<Map<String, Any>>
    ): Receta {

        // Extraer solo los valores de "step" en una lista de Strings
        val pasos = analyzedInstructions.flatMap { instruction ->
            Log.d(
                "mapApiRecetaToReceta",
                "instruction: ${instruction}, steps: ${instruction["steps"].toString()}"
            )
            val steps = instruction["steps"] as? List<Map<String, Any>> ?: emptyList()
            steps.mapNotNull { step ->
                step["step"] as? String // Extraemos solo la descripción del paso
            }
        }

        return Receta(
            id = apiReceta.id.toString(),
            title = apiReceta.title,
            image = apiReceta.image,
            servings = apiReceta.servings,
            ingredients = apiReceta.extendedIngredients,  // Los ingredientes ya coinciden
            steps = pasos,
            time = apiReceta.readyInMinutes,
            dishTypes = apiReceta.dishTypes,
            userId = uid,  // ID del usuario que crea la receta
            glutenFree = apiReceta.glutenFree,
            vegan = apiReceta.vegan,
            vegetarian = apiReceta.vegetarian,
            date = System.currentTimeMillis(),
            //usedIngredientCount = apiReceta.usedIngredientCount,
            unusedIngredients = emptyList(),
            missingIngredientCount = 0,
            unusedIngredientCount = 0,
        )
    }



    //obtener parcialmente las recetas home de 15 en 15
    // TODO Obtener recetas (inicial o paginada)
    fun obtenerRecetasHome(uid: String, limpiarLista: Boolean = true) {
        if (_isLoadingMore.value == true) return // Salir si ya se está cargando

        _isLoadingMore.value = true // Indicar que está cargando

        Log.d("RecetasViewModel", "Obteniendo recetas para el usuario $uid")
        Log.d("RecetasViewModel", " !limpiar: $limpiarLista y lastDocactual: ${lastDocument}")

        // Iniciar la query de Firestore
        var query = db.collection("recetas")
            .orderBy("title") // Ordenar por un campo
            .limit(15) // Limitar a 15 recetas por página

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
                    Log.d("RecetasViewModel", "lastDoc actualizado: $lastDocument")

                    val nuevasRecetas = documents.mapNotNull { document ->
                        try {
                            Receta(
                                id = document.getString("id") ?: "",
                                title = document.getString("title") ?: "",
                                image = document.getString("image") ?: "",
                                servings = (document.get("servings") as? Number)?.toInt() ?: 0,
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
                                dishTypes = document.get("dishTypes") as? List<String>
                                    ?: emptyList(),
                                userId = document.getString("userId") ?: "",
                                glutenFree = document.getBoolean("glutenFree") ?: false,
                                vegan = document.getBoolean("vegan") ?: false,
                                vegetarian = document.getBoolean("vegetarian") ?: false,
                                date = (document.get("date") as? Long)
                                    ?: System.currentTimeMillis(),
                                //usedIngredientCount = 0,
                                unusedIngredients = emptyList(),
                                missingIngredientCount = 0,
                                unusedIngredientCount = 0,  // Obtener la fecha de creación o usar la fecha actual
                            )
                        } catch (e: Exception) {
                            Log.e("RecetasViewModel", "Error al mapear receta: ${e.message}")
                            null
                        }
                    }

                    // Actualizar la lista de recetas
                    if (limpiarLista) {
                        _recetas.value = nuevasRecetas
                    } else {
                        _recetas.value = _recetas.value?.plus(nuevasRecetas) ?: nuevasRecetas
                    }
                    Log.d("RecetasViewModel", "_recetas actualizado: ${_recetas.value}")
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener recetas: ${e.message}")
            } finally {
                _isLoadingMore.value = false // Terminar la carga
            }
        }
    }



    fun obtenerRecetaPorId(uid: String, recetaId: String) {
        Log.d("RecetasViewModel", "Obteniendo receta con ID: $recetaId para el usuario $uid")

        viewModelScope.launch {
            try {
                val document = db.collection("recetas")//.document(uid)
                    //.collection("recetas_aleatorias")
                    .document(recetaId)
                    .get()
                    .await()

                if (document.exists()) {
                    val receta = Receta(
                        id = document.getString("id") ?: "",
                        title = document.getString("title") ?: "",
                        image = document.getString("image") ?: "",
                        servings = (document.get("time") as? Number)?.toInt() ?: 0,
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
                        userId = document.getString("user") ?: "",
                        //usedIngredientCount = (document.get("usedIngredientCount") as? Number)?.toInt() ?: 0,
                        glutenFree = document.getBoolean("glutenFree") ?: false,
                        vegan = document.getBoolean("vegan") ?: false,
                        vegetarian = document.getBoolean("vegetarian") ?: false,
                        date = (document.get("date") as? Long) ?: System.currentTimeMillis(),
                        unusedIngredients = emptyList(),
                        missingIngredientCount = 0,
                        unusedIngredientCount = 0,
                    )

                    // Actualizar el LiveData con la receta obtenida
                    _recetaSeleccionada.value = receta
                    Log.d("RecetasViewModel", "receta selecc: ${recetaSeleccionada.value}")
                } else {
                    Log.e("RecetasViewModel", "No se encontró la receta con ID: $recetaId")
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener receta: ${e.message}")
            }
        }
    }


    /**
     * Devuelve las recetas que contienen todos o algunos de los ingredientes a buscar
     * Por defecto salen las rcetas con más coincidencias de ingredientes buscados
     */
    fun buscarRecetasPorIngredientes(ingredientes: List<IngredienteSimple>) {
        // Convertir la lista de ingredientes a minúsculas para hacer la búsqueda insensible a mayúsculas/minúsculas
        //val ingredientesNormalizados = ingredientes.map { it.lowercase() }

        db.collection("recetas")
            .get()
            .addOnSuccessListener { result ->
                val recetasCoincidentesTotales = mutableListOf<Receta>()
                val recetasCoincidentesParciales = mutableListOf<Pair<Receta, Int>>()

                for (document in result) {
                    val recetaNombre = document.getString("title")
                    val recetaIngredientes = document.get("ingredients") as? List<Map<String, Any>>

                    // Si la receta tiene ingredientes y el nombre de la receta no es nulo
                    if (recetaNombre != null && recetaIngredientes != null) {
                        // Obtener los ingredientes de la receta
                        val ingredientesReceta = recetaIngredientes.map {
                            Ingrediente(
                                name = (it["name"] as? String)?.lowercase() ?: "",
                                image = it["image"] as? String ?: "",
                                amount = (it["amount"] as? Double) ?: 0.0,
                                unit = it["unit"] as? String ?: ""
                            )
                        }

                        // Calcular los ingredientes faltantes (missing)
                        val missingIngredients = ingredientesReceta.filterNot { ingrediente ->
                            ingredientes.any { it.name == ingrediente.name }
                        }


                        // Crear la lista de ingredientes no utilizados (unused)
                        val unusedIngredients = ingredientes.filterNot { ingrediente ->
                            ingredientesReceta.any { it.name == ingrediente.name }
                        }


                        // Contar cuántos ingredientes de la receta coinciden con los ingredientes proporcionados
                        val coincidencias = ingredientes.count { ingrediente ->
                            ingredientesReceta.any { it.name == ingrediente.name }
                        }

                        // Calcular los campos relacionados con los ingredientes no utilizados y faltantes
                        val unusedIngredientCount = unusedIngredients.size
                        val missingIngredientCount = missingIngredients.size

                        // Si la receta contiene todos los ingredientes proporcionados, se agrega a la lista total
                        if (coincidencias == ingredientes.size) {
                            val receta = Receta(
                                id = document.id,
                                title = recetaNombre,
                                image = document.getString("image"),
                                servings = document.getLong("servings")?.toInt() ?: 0,
                                ingredients = ingredientesReceta,
                                steps = document.get("steps") as? List<String> ?: emptyList(),
                                time = document.getLong("time")?.toInt() ?: 0,
                                dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
                                userId = document.getString("userId") ?: "",
                                //usedIngredientCount = coincidencias,
                                unusedIngredients = unusedIngredients, // Ingredientes no utilizados del usuario
                                missingIngredientCount = missingIngredientCount, // Ingredientes faltantes de la receta
                                unusedIngredientCount = unusedIngredientCount, // Contamos los ingredientes no utilizados del usuario
                                glutenFree = document.getBoolean("glutenFree") ?: false,
                                vegan = document.getBoolean("vegan") ?: false,
                                vegetarian = document.getBoolean("vegetarian") ?: false,
                                date = document.getLong("date") ?: System.currentTimeMillis()
                            )
                            recetasCoincidentesTotales.add(receta)
                        } else if (coincidencias > 0) {
                            // Si no contiene todos, pero tiene algunas coincidencias, se guarda con el número de coincidencias
                            val receta = Receta(
                                id = document.id,
                                title = recetaNombre,
                                image = document.getString("image"),
                                servings = document.getLong("servings")?.toInt() ?: 0,
                                ingredients = ingredientesReceta,
                                steps = document.get("steps") as? List<String> ?: emptyList(),
                                time = document.getLong("time")?.toInt() ?: 0,
                                dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
                                userId = document.getString("userId") ?: "",
                                //usedIngredientCount = coincidencias,
                                unusedIngredients = unusedIngredients,
                                missingIngredientCount = missingIngredientCount,
                                unusedIngredientCount = unusedIngredientCount,
                                glutenFree = document.getBoolean("glutenFree") ?: false,
                                vegan = document.getBoolean("vegan") ?: false,
                                vegetarian = document.getBoolean("vegetarian") ?: false,
                                date = document.getLong("date") ?: System.currentTimeMillis()
                            )
                            recetasCoincidentesParciales.add(Pair(receta, coincidencias))
                        }
                    }
                }

                // Ordenar las recetas parciales por el número de coincidencias, de mayor a menor
                val recetasOrdenadas = recetasCoincidentesParciales
                    .sortedByDescending { it.second }
                    .map { it.first }

                // Unir las recetas totales (con todos los ingredientes) con las parciales (con mayor número de coincidencias)
                val recetasFinales = recetasCoincidentesTotales + recetasOrdenadas

                // Actualizar el LiveData con las recetas
                _recetas.value = recetasFinales
                _recetasOriginales.value = recetasFinales
                Log.d("buscarRecetasPorIngredientes()", "$recetasFinales")
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener recetas por ingredientes: ", exception)
            }
    }

    // Búsqueda estricta en tiempo real (requiere que el título contenga TODAS las palabras)
    fun obtenerSugerenciasPorNombre(name: String) {
        val palabrasClave = name.lowercase().split(" ")
            .filter { it.isNotBlank() && it !in listOf("and", "with", "all") }

        if (palabrasClave.isEmpty()) return

        db.collection("recetas")
            .get()
            .addOnSuccessListener { result ->
                val sugerencias = mutableListOf<SugerenciaReceta>()

                for (document in result) {
                    val recetaId = document.id
                    val recetaNombre = document.getString("title")?.lowercase()

                    recetaNombre?.let { nombre ->
                        // Buscar solo recetas que contengan TODAS las palabras ingresadas
                        if (palabrasClave.all { palabra -> nombre.contains(palabra) }) {
                            val coincidencias = palabrasClave.size
                            sugerencias.add(SugerenciaReceta(recetaId, document.getString("title") ?: "", coincidencias))
                        }
                    }
                }

                // Limitar a 7 sugerencias
                _recetasSugeridas.value = sugerencias
                    .sortedByDescending { it.coincidencias }
                    .take(7)
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener sugerencias en tiempo real: ", exception)
            }
    }

    // Recetas que contengan al menos una palabra
    fun obtenerRecetasPorNombre(prefix: String) {
        val palabrasClave = prefix.lowercase().split(" ")
            .filter { it.isNotBlank() && it !in listOf("and", "with", "all") }

        if (palabrasClave.isEmpty()) return

        db.collection("recetas")
            .get()
            .addOnSuccessListener { result ->
                val resultados = mutableListOf<Receta>()

                for (document in result) {
                    val recetaId = document.id
                    val recetaNombre = document.getString("title")?.lowercase()

                    recetaNombre?.let { nombre ->
                        val coincidencias = palabrasClave.count { palabra -> nombre.contains(palabra) }

                        if (coincidencias > 0) {
                            val receta = Receta(
                                id = recetaId,
                                title = document.getString("title") ?: "",
                                image = document.getString("image"),
                                servings = document.getLong("servings")?.toInt() ?: 0,
                                ingredients = (document.get("ingredients") as? List<Map<String, Any>>)?.map { ingrediente ->
                                    Ingrediente(
                                        name = (ingrediente["name"] as? String) ?: "",
                                        image = ingrediente["image"] as? String ?: "",
                                        amount = (ingrediente["amount"] as? Double) ?: 0.0,
                                        unit = ingrediente["unit"] as? String ?: ""
                                    )
                                } ?: emptyList(),
                                steps = document.get("steps") as? List<String> ?: emptyList(),
                                time = document.getLong("time")?.toInt() ?: 0,
                                dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
                                userId = document.getString("userId") ?: "",
                                glutenFree = document.getBoolean("glutenFree") ?: false,
                                vegan = document.getBoolean("vegan") ?: false,
                                vegetarian = document.getBoolean("vegetarian") ?: false,
                                date = document.getLong("date") ?: System.currentTimeMillis(),
                                unusedIngredients = emptyList(),
                                missingIngredientCount = 0,
                                unusedIngredientCount = 0,
                            )

                            resultados.add(receta)
                            Log.d("RecetasViewModel", "Receta añadida a sugerencias: ${receta.title}")
                        }
                    }
                }

                // Prioriza recetas con más palabras coincidentes
                _recetas.value = resultados.sortedByDescending { receta ->
                    palabrasClave.count { palabra -> receta.title.lowercase().contains(palabra) }
                }
                _recetasOriginales.value = _recetas.value
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener sugerencias de recetas: ", exception)
            }
    }


    // Definir la función de filtro
    fun filtrarRecetas(
        tiempoFiltro: Int?,
        maxIngredientesFiltro: Int?,
        maxFaltantesFiltro: Int?,
        maxPasosFiltro: Int?,
        //tipoDietaFiltro: String?,
        tipoPlatoFiltro: String?
    ) {
        val recetasFiltro = _recetasOriginales.value!!.filter { receta ->
            // Validación por tiempo, solo se aplica si tiempoFiltro no es null
            val tiempoValido = tiempoFiltro?.let { receta.time <= it } ?: true

            // Validación por cantidad de ingredientes, solo se aplica si maxIngredientesFiltro no es null
            val ingredientesValidos = maxIngredientesFiltro?.let { receta.ingredients.size <= it } ?: true

            // Validación por ingredientes faltantes, solo se aplica si maxFaltantesFiltro no es null
            val faltantesValidos = maxFaltantesFiltro?.let { receta.missingIngredientCount <= it } ?: true


            // Validación por número de pasos, solo se aplica si maxPasosFiltro no es null
            val pasosValidos = maxPasosFiltro?.let { receta.steps.size <= it } ?: true

            // Validación por tipo de plato, solo se aplica si tipoPlatoFiltro no es null
            val platoValido = tipoPlatoFiltro?.let { receta.dishTypes.contains(it) } ?: true

            /* val tipoDieta =  */

            // Aplicar todos los filtros
            tiempoValido && ingredientesValidos && faltantesValidos && pasosValidos /* && dietaValida*/ && platoValido
        }

        _recetas.value = recetasFiltro
    }

    fun restablecerRecetas(){
        _recetas.value = _recetasOriginales.value
        Log.d("RecetasViewModel", "Restableciendo recetas a las originales...\n ${_recetas.value} ")
    }

    fun restablecerRecetasSugeridas(){
        _recetasSugeridas.value = emptyList()
    }


    //
    fun toggleFavorito(uid: String?, recetaId: String) {
        if (uid == null) return

        val favoritosRef = db.collection("favoritos").document(uid).collection("recetas")

        favoritosRef.document(recetaId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Si ya está en favoritos, lo eliminamos
                    favoritosRef.document(recetaId).delete()
                        .addOnSuccessListener {
                            _esFavorito.value = false
                        }
                } else {
                    // Si no está en favoritos, lo añadimos
                    val favoritoData = hashMapOf("idReceta" to recetaId)

                    favoritosRef.document(recetaId).set(favoritoData)
                        .addOnSuccessListener {
                            _esFavorito.value = true
                        }
                }
            }
    }

    //
    fun verificarSiEsFavorito(uid: String?, recetaId: String) {
        if (uid == null) return

        val favoritosRef = db.collection("favoritos").document(uid).collection("recetas")

        favoritosRef.document(recetaId).get()
            .addOnSuccessListener { document ->
                _esFavorito.value = document.exists()
            }
    }

    fun añadirHistorial(uid: String?, recipeId: String) {
        if (uid == null) return

        val historialRef = db.collection("historial").document(uid).collection("cocinadas")
        val timestamp = System.currentTimeMillis()

        val historialEntry = CookHistory(
            recipeId = recipeId,
            timestamp = timestamp
        )

        historialRef.add(historialEntry)
            .addOnSuccessListener {
                Log.d("Historial", "Receta $recipeId añadida al historial correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("Historial", "Error al añadir receta $recipeId al historial", e)
            }
    }

















    //API



    //    // Obtener todas las recetas home de golpe
//    fun obtenerRecetasHome(uid: String) {
//        Log.d("RecetasViewModel", "Obteniendo recetas home para el usuario $uid")
//
//        // Usamos viewModelScope.launch para ejecutar la tarea en un hilo en segundo plano
//        viewModelScope.launch {
//            try {
//                val docRef = db.collection("recetas").document(uid)
//                    .collection("recetas_aleatorias")
//
//                // Usamos Firebase Firestore con una corrutina para obtener los datos
//                val documents = docRef.get().await() // Usamos .await() para esperar el resultado de la operación asíncrona
//
//                // Mapeamos los documentos obtenidos a objetos Receta
//                val recetasList = documents.mapNotNull { document ->
//                    try {
//                        Receta(
//                            id = document.getString("id") ?: "",
//                            title = document.getString("title") ?: "",
//                            image = document.getString("image") ?: "",
//                            ingredients = (document.get("ingredients") as? List<Map<String, Any>>)?.map { ing ->
//                                Ingrediente(
//                                    name = ing["name"] as? String ?: "",
//                                    amount = (ing["amount"] as? Number)?.toDouble() ?: 0.0,
//                                    unit = ing["unit"] as? String ?: "",
//                                    image = ing["image"] as? String ?: ""
//                                )
//                            } ?: emptyList(),
//                            steps = document.get("steps") as? List<String> ?: emptyList(),
//                            time = (document.get("time") as? Number)?.toInt() ?: 0,
//                            dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
//                            user = document.getString("user") ?: "",
//                            usedIngredientCount = (document.get("usedIngredientCount") as? Number)?.toInt()
//                                ?: 0,
//                            glutenFree = document.getBoolean("glutenFree") ?: false,
//                            vegan = document.getBoolean("vegan") ?: false,
//                            vegetarian = document.getBoolean("vegetarian") ?: false
//                        )
//                    } catch (e: Exception) {
//                        Log.e("RecetasViewModel", "Error al mapear receta: ${e.message}")
//                        null
//                    }
//                }
//
//                // Actualizamos el estado de recetas en el ViewModel
//                _recetas.value = recetasList
//                Log.d("RecetasViewModel", "_recetas: ${_recetas.value}")
//
//            } catch (e: Exception) {
//                Log.e("RecetasViewModel", "Error al obtener recetas: ${e.message}")
//            }
//        }
//    }




}



