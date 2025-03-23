package com.example.recipeat.ui.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.api.RetrofitClient.api
import com.example.recipeat.data.model.ApiReceta
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.data.model.SugerenciaReceta
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class RecetasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _numRecetas = 400

    private val _apiRecetas = MutableStateFlow<List<ApiReceta>>(emptyList())
    private val _apiRecetasIds = MutableStateFlow<List<ApiReceta>>(emptyList())

    private val _apiRecetasBulk = MutableStateFlow<List<ApiReceta>>(emptyList())

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

    private val _recetasFavoritas =  MutableLiveData<List<RecetaSimple>>(emptyList())
    val recetasFavoritas: LiveData<List<RecetaSimple>> = _recetasFavoritas

    private val _recetasHistorial =  MutableLiveData<List<RecetaSimple>>(emptyList())
    val recetasHistorial: LiveData<List<RecetaSimple>> = _recetasHistorial


    fun verificarRecetasGuardadasApi() {
        // Verificar cuántas recetas hay actualmente en Firebase
        db.collection("recetas").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.size() >= _numRecetas) { // Si hay 500 o más recetas
                    Log.d("RecetasViewModel", "Ya se tienen $_numRecetas recetas. No es necesario guardar más.")
                } else {
                    Log.d("RecetasViewModel", "Menos de $_numRecetas recetas (${snapshot.size()}). Se proceden a guardar nuevas recetas.")
                    // Si no se ha alcanzado el límite de 500 recetas, se guardan las recetas aleatorias
                    guardarRecetasApi()
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al contar las recetas en Firebase", e)
            }
    }


    /**
     * Guarda recetas en Firebase de la llamada random recipes de la API.
     * Se ha de ejecutar hasta obtener 400 recetas.
    */
    fun guardarRecetasApi() {
        viewModelScope.launch {
            try {
                delay(3000) // Delay para no sobrecargar las solicitudes
                // Obtener las recetas aleatorias
                val response = api.obtenerRecetasRandom()

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
                                            api.obtenerInstruccionesReceta(apiReceta.id)
                                        val receta = mapApiRecetaToReceta(apiReceta, "", analyzedInstructions)

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
                                            //"user" to receta.userId,
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
                } else {
                    Log.d("RecetasViewModel", "No se obtuvieron recetas aleatorias.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RecetasViewModel", "Error al obtener recetas aleatorias: ${e.message}")
            }
        }
    }



    // Genera el ID único alfanumérico de la receta creada por el usuario
    fun generateRecipeId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }



    // Función para obtener las recetas del usuario
    // TODO PAGINACIÓN
    fun getRecetasUser(uid: String) {
        db.collection("my_recipes").document(uid)
            .collection("recipes")
            .get()
            .addOnSuccessListener { result ->
                val recetasList = result.documents.mapNotNull { document ->
                    try {
                        // Si el documento tiene datos, intenta procesarlo
                        val recetaMap = document.data // Recupera los datos del documento
                        recetaMap?.let { // Si los datos no son nulos, los usamos
                            Receta(
                                id = it["id"] as String,
                                title = it["title"] as String,
                                image = it["image"] as? String,
                                servings = (it["servings"] as Number).toInt() ,  // Convierte el valor a Int, o usa 0 si es nulo
                                ingredients = it["ingredients"] as List<Ingrediente>,
                                steps = it["steps"] as List<String>,
                                time = (it["time"] as Number).toInt(),
                                userId = it["user"] as String,
                                dishTypes = it["dishTypes"] as List<String>,
                                //usedIngredientCount = it["usedIngredientCount"] as Int,
                                glutenFree = it["glutenFree"] as Boolean,
                                vegan = it["vegan"] as Boolean,
                                vegetarian = it["vegetarian"] as Boolean,
                                date = it["date"] as Long,
                                unusedIngredients = emptyList(),
                                missingIngredientCount = 0,
                                unusedIngredientCount = 0,
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("Firestore", "Error parsing recipe", e)
                        null // Si ocurre un error, devolvemos null para que no se añada a la lista
                    }
                }

                // Asignar la lista filtrada a la variable que almacena las recetas
                _recetasUser.value = recetasList
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching recipes", e)
                _recetasUser.value = emptyList() // En caso de error, retorna una lista vacía
            }
    }




    /**
     * Añade a Firebase una receta creada por el user.
     */
    fun addMyRecipe(uid: String, receta: Receta, onComplete: (Boolean, String?) -> Unit) {
        Log.d("RecetasViewModel", "recetaId generado: ${receta.id}")

        val db = FirebaseFirestore.getInstance()

        // Convertir el objeto `Receta` a un HashMap
        val recipeMap = hashMapOf(
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
            "vegetarian" to receta.vegetarian,
            "date" to receta.date
        )

        // Referencia al documento de la receta
        val recipeRef =
            db.collection("my_recipes").document(uid).collection("recipes").document(receta.id)

        // Guardar el HashMap en Firestore
        recipeRef.set(recipeMap)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
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
                val document = db.collection("recetas")
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
    fun toggleFavorito(uid: String?, recetaId: String, title: String, image: String) {
        if (uid == null) return

        val favoritosRef = db.collection("favs_hist").document(uid).collection("favoritos")

        favoritosRef.document(recetaId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Si ya está en favoritos, lo eliminamos
                    favoritosRef.document(recetaId).delete()
                        .addOnSuccessListener {
                            _esFavorito.value = false
                        }
                } else {
                    // Si no está en favoritos, lo añadimos con más información
                    val favoritoData = hashMapOf(
                        "idReceta" to recetaId,
                        "title" to title,
                        "image" to image,
                        "date" to Timestamp.now()
                    )

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

        val favoritosRef = db.collection("favs_hist").document(uid).collection("favoritos")

        favoritosRef.document(recetaId).get()
            .addOnSuccessListener { document ->
                _esFavorito.value = document.exists()
            }
    }

    fun añadirHistorial(uid: String?, recetaId: String, title: String, image: String) {
        if (uid == null) return

        val historialRef = db.collection("favs_hist").document(uid).collection("historial")

        val historialEntry = RecetaSimple(
            id = recetaId,
            title = title,
            image = image,
            date = Timestamp.now()
        )

        historialRef.add(historialEntry)
            .addOnSuccessListener {
                Log.d("RecetasViewModel", "Receta $recetaId añadida al historial correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al añadir receta $recetaId al historial", e)
            }
    }



    fun obtenerRecetasPorRangoDeFecha(uid: String, rango: Int) {
        val currentDate = Timestamp.now() // Fecha actual
        val startDate = when (rango) {
            30 -> Timestamp(currentDate.seconds - 30L * 24 * 60 * 60, 0) // Restamos 30 días en segundos
            7 -> Timestamp(currentDate.seconds - 7L * 24 * 60 * 60, 0) // Restamos 7 días en segundos
            else -> return
        }

        // Depuración
        Log.d("RecetasViewModel", "Fecha actual: ${currentDate.toDate()}, Fecha inicio: ${startDate.toDate()}")

        val recetasRef = db.collection("favs_hist")
            .document(uid)
            .collection("historial")
            .whereGreaterThanOrEqualTo("date", startDate)
            .orderBy("date", Query.Direction.DESCENDING)

        recetasRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val recetas = snapshot.documents.mapNotNull { document ->
                        val recetaId = document.getString("id")
                        val title = document.getString("title")
                        val image = document.getString("image")
                        val date = document.getTimestamp("date")

                        Log.d("RecetasViewModel", "Receta encontrada: id=$recetaId, title=$title, date=$date")

                        if (recetaId != null && title != null && image != null && date != null) {
                            RecetaSimple(id = recetaId, title = title, image = image, date = date)
                        } else null
                    }
                    _recetasHistorial.value = recetas
                    Log.d("RecetasViewModel", "Recetas obtenidas: ${_recetasHistorial.value}")
                } else {
                    _recetasHistorial.value = emptyList()
                    Log.d("RecetasViewModel", "No hay recetas en este rango de fechas")
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al obtener recetas", e)
            }
    }

    // Filtrar las recetas según la fecha seleccionada
    @RequiresApi(Build.VERSION_CODES.O)
    fun filtrarRecetasPorDiaSelecc(selectedDate: LocalDate): List<RecetaSimple> {
        return _recetasHistorial.value?.filter {
            val recetaDate = it.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            recetaDate.isEqual(selectedDate) // Comparar solo con el día seleccionado
        } ?: emptyList()
    }


    fun obtenerRecetasFavoritas(uid: String) {
        val favoritosRef = db.collection("favs_hist")
            .document(uid)
            .collection("favoritos")
            .orderBy("date", Query.Direction.DESCENDING) // Ordenamos por fecha de añadido

        favoritosRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val recetas = snapshot.documents.mapNotNull { document ->
                        val recetaId = document.getString("idReceta")
                        val title = document.getString("title")
                        val image = document.getString("image")
                        val date = document.getTimestamp("date")

                        if (recetaId != null && title != null && image != null && date != null) {
                            RecetaSimple(
                                id = recetaId,
                                title = title,
                                image = image,
                                date = date
                            )
                        } else {
                            null
                        }
                    }
                        _recetasFavoritas.value = recetas
                        Log.d("RecetasViewModel", "Recetas favs actualizadas: ${_recetasFavoritas.value}")

                } else {
                    // Si no hay documentos, se asigna una lista vacía
                        _recetasFavoritas.value = emptyList()
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al obtener los favoritos del usuario", e)
            }
    }











    //API

//    avocado, cayenne, celery, celery stalk, cheddar, cherries, almond, cherry tomato, chickpea, chicken, chicken breast, chicken broth, chicken sausage, chicken thigh, chili pepper, chocolate, chocolate chips, baking powder, cilantro, cinnamon, cocoa powder, coconut, condensed milk, cooking oil, corn, corn oil, cornstarch, couscous, crab, cranberries, cream, cream cheese, bacon, cumin, soy sauce, vinegar, double cream, dulce de leche, egg, egg white, egg yolk, eggplant, chocolate chips, evaporated milk, extra virgin olive oil, feta cheese, firm brown sugar, fish sauce, flour, parsley, ginger, garlic, garlic powder, gelatin, goat cheese, gorgonzola,
//    greek yogurt, green bean, ground almonds, ground beef, ground cinnamon, ground ginger, ground pepper, ground pork, ham, honey, jalapeño, rice, kidney beans, leek, lime, macaroni, mascarpone, goat cheese, milk, mint, mushroom, mustard, mutton, navy beans, oats, oat, olive oil, onion, orange, lettuce,
//    oregano, breadcrumbs, parmesan cheese, peaches, pear, peas, pepper, pie crust, pineapple, banana, pork tenderloin, potato, powdered milk, prawns, bread, quinoa, radish, raisins, raspberry jam, red wine, salad oil, salmon, salt,
//    sausage, scallion, chocolate, shrimp, soy sauce, spinach, onion, squash, sugar, sundried tomatoes, sweet potato, tomato, tomato paste, tomato sauce, tuna, vanilla, vanilla extract, vegetable broth, vegetable oil, vinegar, nuts, water, white wine, bell pepper, yogurt, lentils, corn, collard greens, olivas, zucchini, beef, apple, apples, hake

    val ingredients = "avocado,cayenne,cauliflower head,celery,carrots,celery stalk,cheddar,cherries,almond,cherry tomato,chickpea,chicken,chicken breast,chicken broth,chicken sausage,chicken thigh,chili pepper,chocolate,chocolate chips,baking powder,cilantro,cinnamon,cocoa powder,coconut,condensed milk,cooking oil,corn,corn oil,cornstarch,couscous,crab,cranberries,cream,cream cheese,bacon,cumin,soy sauce,vinegar,double cream,dulce de leche,egg,egg white,egg yolk,eggplant,chocolate chips,evaporated milk,extra virgin olive oil,feta cheese,firm brown sugar,fish sauce,flour,parsley,ginger,garlic,garlic powder,gelatin,goat cheese,gorgonzola,greek yogurt,green bean,ground beef,ground cinnamon,ground ginger,ground pepper,ground pork,ham,honey,jalapeño,rice,kidney beans,leek,lime,macaroni,mascarpone,goat cheese,milk,mint,mushroom,mustard,mutton,navy beans,oats,oat,olive oil,onion,orange,lettuce,oregano,breadcrumbs,parmesan cheese,peaches,pear,peas,pepper,pie crust,pineapple,banana,pork tenderloin,potato,powdered milk,prawns,bread,quinoa,radish,raisins,raspberry jam,red wine,salad oil,salmon,salt,sausage,scallion,chocolate,shrimp,soy sauce,spinach,onion,squash,sugar,sundried tomatoes,sweet potato,tomato,tomato paste,tomato sauce,tuna,vanilla,vanilla extract,vegetable broth,vegetable oil,vinegar,nuts,water,white wine,bell pepper,yogurt,lentils,corn,collard greens,olivas,zucchini,beef,apple,apples,hake"

    // 390 recetas (14 por cada 5 ingredientes)
    fun buscarRecetasPorIngredientes() {
        val ingredientList = ingredients.split(",") // Dividimos la cadena de ingredientes en una lista
        val batchSize = 5
        val totalBatches = ingredientList.size / batchSize + if (ingredientList.size % batchSize == 0) 0 else 1

        viewModelScope.launch {
            try {
                for (i in 0 until totalBatches) {
                    val startIndex = i * batchSize
                    val endIndex = minOf((i + 1) * batchSize, ingredientList.size)
                    val ingredientBatch = ingredientList.subList(startIndex, endIndex).joinToString(",")

                    val response = api.buscarRecetasPorIngredientes(ingredientBatch)

                    if (response.isNotEmpty()) {
                        val filteredResponse = response
                        val currentRecipeIds = _apiRecetasIds.value.map { it.id }.toSet()
                        val uniqueRecipes = filteredResponse.filterNot { currentRecipeIds.contains(it.id) }

                        // Actualizar la lista de recetas
                        _apiRecetasIds.value = (_apiRecetasIds.value + uniqueRecipes).toList()

                        // Guardar en Firebase en la colección "idsRecetas"
                        val db = FirebaseFirestore.getInstance()
                        val recetasCollection = db.collection("idsRecetas")

                        _apiRecetasIds.value.forEach { receta ->
                            val recetaData = mapOf("id" to receta.id)
                            recetasCollection.document(receta.id.toString())
                                .set(recetaData)
                                .addOnSuccessListener {
                                    Log.d("RecetasViewModel", "ID de receta ${receta.id} guardado en Firebase correctamente.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("RecetasViewModel", "Error al guardar el ID en Firebase: ${e.message}")
                                }
                        }

                        Log.d("RecetasViewModel", "Recetas después de agregar nuevas: ${_apiRecetasIds.value.size}")
                    } else {
                        Log.e("RecetasViewModel", "Respuesta vacía o nula de la API para ingredientes: $ingredientBatch")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RecetasViewModel", "Error al obtener las recetas: ${e.message}")
            }
        }
    }



    fun guardarRecetasBulk() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // Cargar batchIndex desde Firebase antes de iniciar
                loadBatchIndexFromFirebase { loadedBatchIndex ->
                    var batchIndex = loadedBatchIndex // Usar el valor cargado

                    db.collection("idsRecetas").get()
                        .addOnSuccessListener { documents ->
                            val recetaIds = documents.mapNotNull { it.getLong("id")?.toInt() }.joinToString(",")

                            if (recetaIds.isEmpty()) {
                                Log.e("RecetasViewModel", "No hay IDs de recetas en Firebase.")
                                return@addOnSuccessListener
                            }

                            val batchSize = 130
                            val batches = recetaIds.chunked(batchSize)

                            if (batchIndex >= batches.size) {
                                Log.d("RecetasViewModel", "Todos los lotes han sido procesados.")
                                return@addOnSuccessListener
                            }

                            val batch = batches[batchIndex]
                            batchIndex++ // Incrementar
                            saveBatchIndexToFirebase(batchIndex) // Guardar el nuevo valor en Firebase

                            viewModelScope.launch {
                                delay(3000)
                                val response = api.obtenerRecetasBulk(recetas_ids = batch)

                                if (response.isNotEmpty()) {
                                    response.forEach { apiReceta ->
                                        val recetaId = apiReceta.id.toString()
                                        db.collection("bulkRecetas").document(recetaId).get()
                                            .addOnSuccessListener { document ->
                                                if (!document.exists()) {
                                                    viewModelScope.launch {
                                                        delay(3000)
                                                        val analyzedInstructions = api.obtenerInstruccionesReceta(apiReceta.id)
                                                        val receta = mapApiRecetaToReceta(apiReceta, "", analyzedInstructions)

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
                                                            "glutenFree" to receta.glutenFree,
                                                            "vegan" to receta.vegan,
                                                            "vegetarian" to receta.vegetarian
                                                        )

                                                        db.collection("bulkRecetas")
                                                            .document(recetaId)
                                                            .set(recetaData)
                                                            .addOnSuccessListener {
                                                                Log.d("RecetasViewModel", "Receta $recetaId guardada en Firebase")
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
                                } else {
                                    Log.e("RecetasViewModel", "No se encontraron recetas con los IDs: $batch")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecetasViewModel", "Error al obtener los IDs de Firebase: ${e.message}")
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RecetasViewModel", "Error al guardar recetas bulk: ${e.message}")
            }
        }
    }



    private fun saveBatchIndexToFirebase(index: Int) {
        val db = FirebaseFirestore.getInstance()
        val batchData = hashMapOf("batchIndex" to index)

        db.collection("config")
            .document("batchIndex")
            .set(batchData)
            .addOnSuccessListener {
                Log.d("RecetasViewModel", "batchIndex actualizado en Firebase: $index")
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al guardar batchIndex en Firebase", e)
            }
    }


    private fun loadBatchIndexFromFirebase(onComplete: (Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("config")
            .document("batchIndex")
            .get()
            .addOnSuccessListener { document ->
                val index = document.getLong("batchIndex")?.toInt() ?: 0
                Log.d("RecetasViewModel", "batchIndex cargado desde Firebase: $index")
                onComplete(index)
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al cargar batchIndex desde Firebase", e)
                onComplete(0) // Si hay error, usa 0 como valor por defecto
            }
    }




//    fun nombresIngredientes() {
//        val db = FirebaseFirestore.getInstance()
//        val ingredientesRef = db.collection("ingredientes")
//
//        ingredientesRef.get()
//            .addOnSuccessListener { documents ->
//                val nombres = documents.mapNotNull { it.getString("name") }
//
//                // Dividir la lista en fragmentos de 90 elementos
//                val chunkSize = 90
//                val chunks = nombres.chunked(chunkSize)
//
//                chunks.forEachIndexed { index, chunk ->
//                    println("Nombres ingredientes (${index + 1}/${chunks.size}): $chunk")
//                }
//            }
//            .addOnFailureListener { e ->
//                println("Error obteniendo ingredientes: ${e.message}")
//            }
//    }



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



