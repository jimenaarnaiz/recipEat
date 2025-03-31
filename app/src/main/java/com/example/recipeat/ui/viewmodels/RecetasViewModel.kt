package com.example.recipeat.ui.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class RecetasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _numRecetas = 400

    private val _apiRecetas = MutableStateFlow<List<ApiReceta>>(emptyList())
    private val _apiRecetasIds = MutableStateFlow<List<ApiReceta>>(emptyList())

    private val _apiRecetasBulk = MutableStateFlow<List<ApiReceta>>(emptyList())

    // para name search
    private val _recetasSugeridas = MutableStateFlow<List<SugerenciaReceta>>(emptyList())
    val recetasSugeridas: StateFlow<List<SugerenciaReceta>> = _recetasSugeridas

    private val _recetas = MutableLiveData<List<Receta>>(emptyList())
    val recetas: LiveData<List<Receta>> = _recetas

    // para my recipes (user)
    private val _recetasUser = MutableLiveData<List<Receta>>(emptyList())
    val recetasUser: LiveData<List<Receta>> = _recetasUser

    private val _recetaSeleccionada = MutableLiveData<Receta>()
    val recetaSeleccionada: LiveData<Receta> = _recetaSeleccionada

    // para paginacion
    private var lastDocument: DocumentSnapshot? = null
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> get() = _isLoadingMore

    //recetas resultados busqueda sin filtros ni orden aplicado
    private val _recetasOriginales = MutableLiveData<List<Receta>>(emptyList())

    private val _esFavorito = MutableLiveData<Boolean>()
    val esFavorito: LiveData<Boolean> get() = _esFavorito

    private val _recetasFavoritas =  MutableLiveData<List<RecetaSimple>>(emptyList())
    val recetasFavoritas: LiveData<List<RecetaSimple>> = _recetasFavoritas

    private val _recetasHistorial =  MutableLiveData<List<RecetaSimple>>(emptyList())
    val recetasHistorial: LiveData<List<RecetaSimple>> = _recetasHistorial

    // MutableState para almacenar los pasos con las imágenes de los equipos
    private val _equipmentSteps =  MutableLiveData<List<List<String>>>(emptyList())
    val equipmentSteps: LiveData<List<List<String>>> = _equipmentSteps



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
                                                    "image" to it.image,
                                                    "aisle" to it.aisle
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


    // TODO CON PAGINACION:
    // Variable para el último documento obtenido de las recetas del usuario
    private var lastDocumentUser: DocumentSnapshot? = null

    // Función para obtener las recetas del usuario con paginación
    fun getRecetasUser(uid: String, limpiarLista: Boolean = true) {
        if (_isLoadingMore.value == true) return // Evita cargar más si ya se está cargando

        _isLoadingMore.value = true // Indica que está cargando

        // Crea la consulta inicial con un límite de 15
        var query = db.collection("my_recipes").document(uid)
            .collection("recipes")
            .orderBy("date", Query.Direction.DESCENDING) // Ordena por fecha de manera descendente
            .limit(15)

        // Si ya hay un documento anterior, usa startAfter para continuar desde ese punto
        if (lastDocumentUser != null && !limpiarLista) {
            Log.d("RecetasViewModel", "Recetas User: Paginar desde documento: ${lastDocumentUser?.id}")
            query = query.startAfter(lastDocumentUser!!)
        } else {
            Log.d("RecetasViewModel", "Recetas User: No hay documento previo, iniciando desde el principio")
        }

        // Ejecutar la consulta en el ViewModelScope para obtener las recetas
        viewModelScope.launch {
            try {
                val documents = query.get().await() // Obtener los documentos

                if (!documents.isEmpty) {
                    // Actualiza el último documento
                    lastDocumentUser = documents.documents.last()

                    // Mapea los documentos a una lista de recetas
                    val nuevasRecetas = documents.documents.mapNotNull { document ->
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
                                        image = ing["image"] as? String ?: "",
                                        aisle = ing["aisle"] as? String ?: ""
                                    )
                                } ?: emptyList(),
                                steps = document.get("steps") as? List<String> ?: emptyList(),
                                time = (document.get("time") as? Number)?.toInt() ?: 0,
                                dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
                                userId = document.getString("userId") ?: "",
                                glutenFree = document.getBoolean("glutenFree") ?: false,
                                vegan = document.getBoolean("vegan") ?: false,
                                vegetarian = document.getBoolean("vegetarian") ?: false,
                                date = (document.get("date") as? Long) ?: System.currentTimeMillis(),
                                unusedIngredients = emptyList(),
                                missingIngredientCount = 0,
                                unusedIngredientCount = 0,
                                esFavorita = null
                            )
                        } catch (e: Exception) {
                            Log.e("RecetasViewModel", "Recetas User: Error al mapear receta: ${e.message}")
                            null
                        }
                    }

                    // Si se debe limpiar la lista, se reemplaza, sino se agrega a la lista existente
                    if (limpiarLista) {
                        _recetasUser.value = nuevasRecetas
                    } else {
                        _recetasUser.value = _recetasUser.value.orEmpty() + nuevasRecetas
                    }

                    Log.d("RecetasViewModel", "Total recetas User: ${_recetasUser.value?.size}")
                } else {
                    Log.d("RecetasViewModel", "No hay más recetas User para cargar.")
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Recetas User: Error al obtener recetas: ${e.message}")
            } finally {
                _isLoadingMore.value = false // Indicar que terminó el proceso de carga
            }
        }
    }





    // Función para obtener las recetas del usuario
    // SIN PAGINACIÓN
//    fun getRecetasUser(uid: String) {
//        db.collection("my_recipes").document(uid)
//            .collection("recipes")
//            .get()
//            .addOnSuccessListener { result ->
//                val recetasList = result.documents.mapNotNull { document ->
//                    try {
//                        // Si el documento tiene datos, intenta procesarlo
//                        val recetaMap = document.data // Recupera los datos del documento
//                        recetaMap?.let { // Si los datos no son nulos, los usamos
//                            Receta(
//                                id = it["id"] as String,
//                                title = it["title"] as String,
//                                image = it["image"] as? String,
//                                servings = (it["servings"] as Number).toInt(),  // Convierte el valor a Int, o usa 0 si es nulo
//                                ingredients = it["ingredients"] as List<Ingrediente>,
//                                steps = it["steps"] as List<String>,
//                                time = (it["time"] as Number).toInt(),
//                                userId = it["user"] as String,
//                                dishTypes = it["dishTypes"] as List<String>,
//                                //usedIngredientCount = it["usedIngredientCount"] as Int,
//                                glutenFree = it["glutenFree"] as Boolean,
//                                vegan = it["vegan"] as Boolean,
//                                vegetarian = it["vegetarian"] as Boolean,
//                                date = it["date"] as Long,
//                                unusedIngredients = emptyList(),
//                                missingIngredientCount = 0,
//                                unusedIngredientCount = 0,
//                                esFavorita = null,
//                            )
//                        }
//                    } catch (e: Exception) {
//                        Log.w("Firestore", "Error parsing recipe", e)
//                        null // Si ocurre un error, devolvemos null para que no se añada a la lista
//                    }
//                }
//
//                // Asignar la lista filtrada a la variable que almacena las recetas
//                _recetasUser.value = recetasList
//            }
//            .addOnFailureListener { e ->
//                Log.w("Firestore", "Error fetching recipes", e)
//                _recetasUser.value = emptyList() // En caso de error, retorna una lista vacía
//            }
//    }



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
                    "image" to it.image,
                    "aisle" to it.aisle
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

        Log.d("RecetasViewModel", "Referencia del documento de la receta: ${recipeRef.path}")

        // Guardar el HashMap en Firestore
        recipeRef.set(recipeMap)
            .addOnSuccessListener {
                Log.d("RecetasViewModel", "Receta ${receta.id} guardada correctamente en Firestore")
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al guardar receta ${receta.id} en Firestore: ${e.message}")
                onComplete(false, e.message)
            }
    }

    /**
     * Edita una receta en Firebase solo con los campos modificados.
     */
    fun editMyRecipe(uid: String, recetaModificada: Receta, onComplete: (Boolean, String?) -> Unit) {
        Log.d("RecetasViewModel", "Editando receta con id: ${recetaModificada.id}")

        val db = FirebaseFirestore.getInstance()

        // Referencia al documento de la receta original
        val recipeRef =
            db.collection("my_recipes").document(uid).collection("recipes").document(recetaModificada.id)

        // Obtener la receta original desde Firebase
        recipeRef.get().addOnSuccessListener { document ->
            if (document.exists()) {

                // Obtener los datos del documento como un mapa
                val recetaOriginalData = document.data

                // Si no existe, devolver un error
                if (recetaOriginalData == null) {
                    onComplete(false, "Receta no encontrada.")
                    return@addOnSuccessListener
                }

                // Convertir manualmente el mapa a un objeto Receta
                val recetaOriginal = Receta(
                    id = recetaOriginalData["id"] as String,
                    title = recetaOriginalData["title"] as String,
                    image = recetaOriginalData["image"] as? String,
                    servings = (recetaOriginalData["servings"] as Number).toInt(),  // Convierte el valor a Int, o usa 0 si es nulo
                    ingredients = recetaOriginalData["ingredients"] as List<Ingrediente>,
                    steps = recetaOriginalData["steps"] as List<String>,
                    time = (recetaOriginalData["time"] as Number).toInt(),
                    userId = recetaOriginalData["user"] as String,
                    dishTypes = recetaOriginalData["dishTypes"] as List<String>,
                    //usedIngredientCount = it["usedIngredientCount"] as Int,
                    glutenFree = recetaOriginalData["glutenFree"] as Boolean,
                    vegan = recetaOriginalData["vegan"] as Boolean,
                    vegetarian = recetaOriginalData["vegetarian"] as Boolean,
                    date = recetaOriginalData["date"] as Long,
                    unusedIngredients = emptyList(),
                    missingIngredientCount = 0,
                    unusedIngredientCount = 0,
                    esFavorita = null,
                )

                // Crear un mapa vacío para los cambios
                val updatesMap = mutableMapOf<String, Any>()

                // Comparar y agregar solo los campos modificados
                if (recetaModificada.title != recetaOriginal.title) {
                    updatesMap["title"] = recetaModificada.title
                }
                if (recetaModificada.image != recetaOriginal.image) {
                    updatesMap["image"] = recetaModificada.image.toString()
                }
                if (recetaModificada.servings != recetaOriginal.servings) {
                    updatesMap["servings"] = recetaModificada.servings
                }
                if (recetaModificada.ingredients != recetaOriginal.ingredients) {
                    updatesMap["ingredients"] = recetaModificada.ingredients.map {
                        hashMapOf(
                            "name" to it.name,
                            "amount" to it.amount,
                            "unit" to it.unit,
                            "image" to it.image,
                            "aisle" to it.aisle
                        )
                    }
                }
                if (recetaModificada.steps != recetaOriginal.steps) {
                    updatesMap["steps"] = recetaModificada.steps
                }
                if (recetaModificada.time != recetaOriginal.time) {
                    updatesMap["time"] = recetaModificada.time
                }
                if (recetaModificada.dishTypes != recetaOriginal.dishTypes) {
                    updatesMap["dishTypes"] = recetaModificada.dishTypes
                }
                if (recetaModificada.glutenFree != recetaOriginal.glutenFree) {
                    updatesMap["glutenFree"] = recetaModificada.glutenFree
                }
                if (recetaModificada.vegan != recetaOriginal.vegan) {
                    updatesMap["vegan"] = recetaModificada.vegan
                }
                if (recetaModificada.vegetarian != recetaOriginal.vegetarian) {
                    updatesMap["vegetarian"] = recetaModificada.vegetarian
                }
                if (recetaModificada.date != recetaOriginal.date) {
                    updatesMap["date"] = recetaModificada.date
                }

                // Verificar si hay cambios
                if (updatesMap.isNotEmpty()) {
                    // Actualizar solo los campos modificados en Firestore
                    recipeRef.update(updatesMap)
                        .addOnSuccessListener {
                            onComplete(true, null)
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, e.message)
                        }
                } else {
                    // Si no hay cambios, devolver éxito sin realizar ninguna actualización
                    onComplete(true, null)
                }
            } else {
                onComplete(false, "Receta no encontrada.")
            }
        }.addOnFailureListener { e ->
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
            esFavorita = null,
        )
    }


    fun obtenerRecetasHome(limpiarLista: Boolean = true) {
        if (_isLoadingMore.value == true) return // Salir si ya se está cargando

        _isLoadingMore.value = true // Indicar que está cargando

        var query = db.collection("recetas")
            .orderBy("id") // Ordena por ID
            .limit(15)

        if (lastDocument != null && !limpiarLista) {
            Log.d("RecetasViewModel", "Paginar desde documento: ${lastDocument?.id}")
            query = query.startAfter(lastDocument?.get("id"))
        } else {
            Log.d("RecetasViewModel", "No hay documento previo, iniciando desde el principio")
        }

        viewModelScope.launch {
            try {
                val documents = query.get().await()

                if (!documents.isEmpty) {
                    lastDocument = documents.documents.last() // Actualizar último documento cargado

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
                                        image = ing["image"] as? String ?: "",
                                        aisle = ing["aisle"] as? String ?: "",
                                    )
                                } ?: emptyList(),
                                steps = document.get("steps") as? List<String> ?: emptyList(),
                                time = (document.get("time") as? Number)?.toInt() ?: 0,
                                dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
                                userId = document.getString("userId") ?: "",
                                glutenFree = document.getBoolean("glutenFree") ?: false,
                                vegan = document.getBoolean("vegan") ?: false,
                                vegetarian = document.getBoolean("vegetarian") ?: false,
                                date = (document.get("date") as? Long) ?: System.currentTimeMillis(),
                                unusedIngredients = emptyList(),
                                missingIngredientCount = 0,
                                unusedIngredientCount = 0,
                                esFavorita = null,
                            )
                        } catch (e: Exception) {
                            Log.e("RecetasViewModel", "Error al mapear receta: ${e.message}")
                            null
                        }
                    }

                    if (limpiarLista) {
                        _recetas.value = nuevasRecetas
                    } else {
                        _recetas.value = _recetas.value.orEmpty() + nuevasRecetas
                    }
                    Log.d("RecetasViewModel", "Total recetas: ${_recetas.value?.size}")
                } else {
                    Log.d("RecetasViewModel", "No hay más recetas para cargar.")
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener recetas: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }



    /**
     * Obtener receta por id
     * deUser: Boolean para saber si se tiene que buscar en una coleccion u otra
     */
    fun obtenerRecetaPorId(uid: String, recetaId: String, deUser: Boolean) {
        Log.d("RecetasViewModel", "deUser: $deUser Obteniendo receta con ID: $recetaId para el usuario $uid")

        viewModelScope.launch {
            try {
                val documentRef = if (deUser) {
                    db.collection("my_recipes") // Colección de recetas del usuario
                        .document(uid) // Documento del usuario
                        .collection("recipes") // Subcolección de recetas
                        .document(recetaId) // Documento específico de la receta con recetaId
                } else {
                    db.collection("recetas")    // Colección de recetas de la API
                        .document(recetaId)
                }

                val document = documentRef.get().await()

                if (document.exists()) {
                    val receta = Receta(
                        id = document.getString("id") ?: "",
                        title = document.getString("title") ?: "",
                        image = document.getString("image") ?: "",
                        servings = (document.get("servings") as? Number)?.toInt() ?: 0,
                        ingredients = (document.get("ingredients") as? List<Map<String, Any>>)?.map { ing ->
                            Ingrediente(
                                name = ing["name"] as? String ?: "",
                                amount = (ing["amount"] as? Number)?.toDouble() ?: 0.0,
                                unit = ing["unit"] as? String ?: "",
                                image = ing["image"] as? String ?: "",
                                aisle = ing["aisle"] as? String ?: "",
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
                        esFavorita = null,
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


    fun eliminarReceta(uid: String, recetaId: String) {
        Log.d("RecetasViewModel", "Eliminando receta con ID: $recetaId para el usuario $uid")

        viewModelScope.launch {
            try {
                val documentRef =
                    db.collection("my_recipes") // Colección de recetas del usuario
                        .document(uid) // Documento del usuario
                        .collection("recipes") // Subcolección de recetas
                        .document(recetaId) // Documento específico de la receta con recetaId

                // Eliminar el documento
                documentRef.delete().await()

                Log.d("RecetasViewModel", "Receta con ID: $recetaId eliminada exitosamente")

                //_recetaSeleccionada.value = null

            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al eliminar receta: ${e.message}")
            }
        }
    }

    fun eliminarRecetaDelHistorial(uid: String, recetaId: String) {
        viewModelScope.launch {
            try {
                // Eliminar de las recetas del historial
                val historialRef = db.collection("favs_hist")
                    .document(uid)
                    .collection("historial")

                // Buscar si la receta está en el historial
                val historialSnapshot = historialRef.whereEqualTo("id", recetaId).get().await()

                if (historialSnapshot.isEmpty) {
                    Log.d("RecetasViewModel", "La receta no se encuentra en el historial.")
                    return@launch
                }

                // Eliminar todas las instancias de la receta en el historial
                for (document in historialSnapshot.documents) {
                    historialRef.document(document.id).delete().await()
                    Log.d("RecetasViewModel", "Receta $recetaId eliminada del historial.")
                }

            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al eliminar receta del historial: ${e.message}")
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
                                unit = it["unit"] as? String ?: "",
                                aisle = it["aisle"] as? String ?: "",
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
                                date = document.getLong("date") ?: System.currentTimeMillis(),
                                esFavorita = null,
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
                                date = document.getLong("date") ?: System.currentTimeMillis(),
                                esFavorita = null,
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
                                        unit = ingrediente["unit"] as? String ?: "",
                                        aisle = ingrediente["aisle"] as? String ?: ""
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
                                esFavorita = null,
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

    fun ordenarResultados(selectedOrder: String) {
        val recetasOrdenar = _recetasOriginales.value ?: return
        Log.d("RecetasViewModel", "Ordenar: $selectedOrder")

        // sortedBy devuelve una lista
        _recetas.value = when (selectedOrder) {
            "Time" -> recetasOrdenar.sortedBy { it.time }
            "Alphabetical" -> recetasOrdenar.sortedBy { it.title }
            "Number of Ingredients" -> recetasOrdenar.sortedBy { it.ingredients.size }
            "Recent Asc" -> recetasOrdenar.sortedBy { it.date }
            "Recent Desc" -> recetasOrdenar.sortedByDescending { it.date }
            else -> recetasOrdenar
        }
    }



    //
    fun toggleFavorito(uid: String?, userReceta: String, recetaId: String, title: String, image: String) {
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
                        "date" to Timestamp.now(),
                        "user" to userReceta
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

    fun añadirHistorial(uid: String, userReceta: String, recetaId: String, title: String, image: String) {

        val historialRef = db.collection("favs_hist").document(uid).collection("historial")

        val historialEntry = RecetaSimple(
            id = recetaId,
            title = title,
            image = image,
            date = Timestamp.now(),
            userReceta = userReceta
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
                        val image = document.getString("image") ?: ""
                        val date = document.getTimestamp("date")
                        val userId = document.getString("uid") ?: ""

                        Log.d("RecetasViewModel", "Receta encontrada: id=$recetaId, title=$title, date=$date, uid=$userId")

                        if (recetaId != null && title != null && date != null) {
                            RecetaSimple(id = recetaId, title = title, image = image, date = date, userReceta = userId )
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
                        val image = document.getString("image") ?: ""
                        val date = document.getTimestamp("date")
                        val userId = document.getString("user") ?: ""

                        if (recetaId != null && title != null && date != null) {
                            RecetaSimple(
                                id = recetaId,
                                title = title,
                                image = image,
                                date = date,
                                userReceta = userId
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




    // Lista de nombres de posibles equipos
    val equipmentNames = listOf(
        "sauce pan", "frying pan", "oven", "whisk", "bowl", "griddle", "food processor",
        "plastic wrap", "knife", "ladle", "spatula", "strainer", "grater", "hand mixer",
        "coffee maker", "microwave", "blender", "juicer", "teaspoon", "tablespoon",
        "mandoline", "vacuum sealer", "thermometer", "kettle", "toaster", "baking dish",
        "muffin tin", "timer", "cutting board", "mortar and pestle", "siphon", "meat grinder",
        "pasta maker", "pizza cutter", "stove", "juicer", "scale", "blow torch", "loaf pan",
        "casserole dish", "teapot", "espresso machine", "citrus press", "baking tray",
        "nonstick pan", "cooling rack", "drainer", "mixing bowl", "tongs", "baking sheet", "potato-masher", "fridge","grill"
    )

    fun checkAndSaveEquipmentImage(equipmentName: String) {
        // Lanzar la operación en una coroutine para no bloquear el hilo principal
        CoroutineScope(Dispatchers.IO).launch {
            val firestore = FirebaseFirestore.getInstance()
            val client = OkHttpClient()

            // Formatear el nombre del equipo para crear la URL
            val formattedName = equipmentName.lowercase().replace(" ", "-")
            val imageUrl = "https://img.spoonacular.com/equipment_100x100/$formattedName.jpg"

            // Crear la petición HTTP para verificar si la imagen está disponible
            val request = Request.Builder().url(imageUrl).build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        // Si la imagen existe, comprobar si ya está guardada en Firestore
                        val equipmentQuery = firestore.collection("equipment")
                            .whereEqualTo("imageUrl", imageUrl) // Comprobamos si ya existe la imagen

                        equipmentQuery.get().addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                // Si no existe, guardar el equipo
                                val equipmentData = hashMapOf(
                                    "name" to equipmentName,
                                    "imageUrl" to imageUrl
                                )

                                firestore.collection("equipment")
                                    .add(equipmentData)
                                    .addOnSuccessListener { documentReference ->
                                        println("Equipment saved successfully with ID: ${documentReference.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Error saving equipment: $e")
                                    }
                            } else {
                                // Si ya existe, no hacer nada
                                println("Equipment already exists in Firestore")
                            }
                        }.addOnFailureListener { e ->
                            println("Error checking if equipment exists: $e")
                        }
                    } else {
                        // Si la imagen no está disponible
                        println("No image found for $equipmentName")
                    }
                }
            } catch (e: Exception) {
                println("Error during network request: $e")
            }
        }
    }


    // Llamar la función para cada equipo
    fun saveAllEquipmentImages() {
        equipmentNames.forEach { equipmentName ->
            checkAndSaveEquipmentImage(equipmentName)
        }
    }


    fun updateEquipmentSteps(steps: List<String>) {
        db.collection("equipment").get()
            .addOnSuccessListener { querySnapshot ->
                val equipos = mutableMapOf<String, String>()

                querySnapshot.documents.forEach { document ->
                    val nombreEquipo = document.getString("name") ?: ""
                    val urlImagen = document.getString("imageUrl") ?: ""

                    if (nombreEquipo.isNotEmpty() && urlImagen.isNotEmpty()) {
                        equipos[nombreEquipo] = urlImagen
                    }
                }

                Log.d("RecetasViewModel", "Equipos obtenidos: $equipos")

                val stepsWithImages = mutableListOf<List<String>>()

                steps.forEach { step ->
                    val equipmentImages = mutableListOf<String>()

                    equipos.forEach { (equipo, imageUrl) ->
                        if (step.contains(equipo, ignoreCase = true)) {
                            equipmentImages.add(imageUrl)
                        }
                    }

                    Log.d("RecetasViewModel", "Paso: $step, Equipos detectados: $equipmentImages")

                    stepsWithImages.add(equipmentImages)
                }

                _equipmentSteps.value = stepsWithImages
                Log.d("RecetasViewModel", "Lista final de imágenes por paso: ${_equipmentSteps.value}")
            }
            .addOnFailureListener { exception ->
                Log.e("RecetasViewModel", "Error obteniendo los equipos: ", exception)
            }
    }

























    //API

    val ingredients = "avocado,cayenne,cauliflower head,celery,carrots,celery stalk,cheddar,cherries,almond,cherry tomato,chickpea,chicken,chicken breast,chicken broth,chicken sausage,chicken thigh,chili pepper,chocolate,chocolate chips,baking powder,cilantro,cinnamon,cocoa powder,coconut,condensed milk,cooking oil,corn,corn oil,cornstarch,couscous,crab,cranberries,cream,cream cheese,bacon,cumin,soy sauce,vinegar,double cream,dulce de leche,egg,egg white,egg yolk,eggplant,chocolate chips,evaporated milk,extra virgin olive oil,feta cheese,firm brown sugar,fish sauce,flour,parsley,ginger,garlic,garlic powder,gelatin,goat cheese,gorgonzola,greek yogurt,green bean,ground beef,ground cinnamon,ground ginger,ground pepper,ground pork,ham,honey,jalapeño,rice,kidney beans,leek,lime,macaroni,mascarpone,goat cheese,milk,mint,mushroom,mustard,mutton,navy beans,oats,oat,olive oil,onion,orange,lettuce,oregano,breadcrumbs,parmesan cheese,peaches,pear,peas,pepper,pie crust,pineapple,banana,pork tenderloin,potato,powdered milk,prawns,bread,quinoa,radish,raisins,raspberry jam,red wine,salad oil,salmon,salt,sausage,scallion,chocolate,shrimp,soy sauce,spinach,onion,squash,sugar,sundried tomatoes,sweet potato,tomato,tomato paste,tomato sauce,tuna,vanilla,vanilla extract,vegetable broth,vegetable oil,vinegar,nuts,water,white wine,bell pepper,yogurt,lentils,corn,collard greens,olivas,zucchini,beef,apple,apples,hake,anchovies,cucumber,mayonnaise,ketchup,chard,pumpkin,lemon,cabbage,octopus,strawberries,squid,cod,trout,sea bream,sardines,white fish,smoked salmon,surimi,clams,pork,lamb,turkey,quail,ground meat"

    fun eliminarRecetasNoExistentesEnBulkRecetas() {
        viewModelScope.launch {
            try {
                // Obtener las recetas de 'recetasIds' desde Firestore
                val db = FirebaseFirestore.getInstance()
                val recetasIdsSnapshot = db.collection("idsRecetas").get().await()
                val recetasIds = recetasIdsSnapshot.documents.map { it.id.toInt() }

                // Obtener las recetas de 'bulkRecetas' desde Firestore
                val bulkRecetasSnapshot = db.collection("bulkRecetas").get().await()
                val bulkRecetasIds = bulkRecetasSnapshot.documents.map { it.id.toInt() }

                // Filtrar las recetas en 'recetasIds' que no están en 'bulkRecetas'
                val recetasAEliminar = recetasIds.filterNot { it in bulkRecetasIds }

                // Eliminar las recetas que no están en 'bulkRecetas'
                if (recetasAEliminar.isNotEmpty()) {
                    val recetasCollection = db.collection("idsRecetas")

                    // Eliminar cada receta que no esté en 'bulkRecetas'
                    recetasAEliminar.forEach { recetaId ->
                        recetasCollection.document(recetaId.toString())
                            .delete()
                            .addOnSuccessListener {
                                Log.d("RecetasViewModel", "Receta con ID $recetaId eliminada correctamente de 'idsRecetas'.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("RecetasViewModel", "Error al eliminar la receta con ID $recetaId: ${e.message}")
                            }
                    }
                } else {
                    Log.d("RecetasViewModel", "No hay recetas para eliminar, todas están presentes en 'bulkRecetas'.")
                }

            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al eliminar recetas que no están en 'bulkRecetas': ${e.message}")
            }
        }
    }


    //TODO white beans,green beans,black beans,bread

    // crea coleccion bulkIngredients buscando la image y el title en bulkRecetas si coincide el ingrediente con el de la lista
    fun fetchAndStoreIngredients() {
        val db = FirebaseFirestore.getInstance()
        val allowedIngredients = setOf(
            "avocado", "cayenne", "cauliflower head", "celery", "carrots", "celery stalk", "cheddar", "cherries", "almond",
            "cherry tomato", "chickpea", "chicken", "chicken breast", "chicken broth", "chicken sausage", "chicken thigh",
            "chili pepper", "chocolate", "chocolate chips", "baking powder", "cilantro", "cinnamon", "cocoa powder", "coconut",
            "condensed milk", "cooking oil", "corn", "corn oil", "cornstarch", "couscous", "crab", "cranberries", "cream",
            "cream cheese", "bacon", "cumin", "soy sauce", "vinegar", "double cream", "dulce de leche", "egg", "egg white", "egg yolk",
            "eggplant", "evaporated milk", "extra virgin olive oil", "feta cheese", "firm brown sugar", "fish sauce", "flour", "parsley",
            "ginger", "garlic", "garlic powder", "gelatin", "goat cheese", "gorgonzola", "greek yogurt", "green bean", "ground beef",
            "ground cinnamon", "ground ginger", "ground pepper", "ground pork", "ham", "honey", "jalapeño", "rice", "kidney beans",
            "leek", "lime", "macaroni", "mascarpone", "milk", "mint", "mushroom", "mustard", "mutton", "navy beans", "oats", "olive oil",
            "onion", "orange", "lettuce", "oregano", "breadcrumbs", "parmesan cheese", "peaches", "pear", "peas", "pepper", "pie crust",
            "pineapple", "banana", "pork tenderloin", "potato", "powdered milk", "prawns", "bread", "quinoa", "radish", "raisins",
            "raspberry jam", "red wine", "salad oil", "salmon", "salt", "sausage", "scallion", "shrimp", "spinach", "squash", "sugar",
            "sundried tomatoes", "sweet potato", "tomato", "tomato paste", "tomato sauce", "tuna", "vanilla", "vanilla extract",
            "vegetable broth", "vegetable oil", "nuts", "water", "white wine", "bell pepper", "yogurt", "lentils", "corn", "collard greens",
            "olives", "zucchini", "beef", "apple", "apples", "hake", "anchovies", "cucumber", "mayonnaise", "ketchup", "chard", "pumpkin",
            "lemon", "cabbage", "octopus", "strawberries", "squid", "cod", "trout", "sea bream", "sardines", "white fish", "smoked salmon",
            "surimi", "clams", "pork", "lamb", "turkey", "quail", "ground meat"
        )

        db.collection("bulkRecetas").get().addOnSuccessListener { result ->
            val ingredientsToStore = mutableMapOf<String, String>()

            for (document in result) {
                val ingredients = document.get("ingredients") as? List<Map<String, Any>> ?: continue

                for (ingredient in ingredients) {
                    val name = ingredient["name"] as? String ?: continue
                    val image = ingredient["image"] as? String ?: ""

                    if (name in allowedIngredients) {
                        ingredientsToStore[name] = image
                    }
                }
            }

            if (ingredientsToStore.isNotEmpty()) {
                db.collection("bulkIngredients").get().addOnSuccessListener { existingDocs ->
                    val existingNames = existingDocs.documents.mapNotNull { it.getString("name") }.toSet()
                    val batch = db.batch()

                    ingredientsToStore.forEach { (name, image) ->
                        if (name !in existingNames) {
                            val newDocRef = db.collection("bulkIngredients").document()
                            batch.set(newDocRef, mapOf("name" to name, "image" to image))
                            Log.d("Firebase", "Adding ingredient: $name")
                        } else {
                            Log.d("Firebase", "Ingredient already exists: $name")
                        }
                    }

                    batch.commit().addOnSuccessListener {
                        Log.d("Firebase", "Batch commit successful")
                    }.addOnFailureListener { exception ->
                        Log.e("Firebase", "Error committing batch: ${exception.message}")
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error fetching bulkIngredients: ${exception.message}")
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error fetching bulkRecetas: ${exception.message}")
        }
    }

    // obtiene 14 recetas/ingrediente y las añade a recetasIDs
    fun buscarRecetasPorCadaIngrediente() {
        val ingredientList = ingredients.split(",") // Dividimos la cadena de ingredientes en una lista
        val db = FirebaseFirestore.getInstance()

        viewModelScope.launch {
            try {
                // Obtener el último ingrediente procesado desde Firestore (si existe)
                val lastProcessedIngredientDoc = db.collection("config")
                    .document("lastProcessedIngredient")
                    .get()
                    .await()

                var startIndex = 0
                if (lastProcessedIngredientDoc.exists()) {
                    val lastProcessedIngredient = lastProcessedIngredientDoc.getString("ingredient")
                    startIndex = ingredientList.indexOf(lastProcessedIngredient) + 1
                }

                // Iterar desde el índice donde quedó
                for (i in startIndex until ingredientList.size) {
                    val ingredient = ingredientList[i]

                    try {
                        // Llamar a la función con el ingrediente actual
                        val response = api.buscarRecetasPorIngredientes(ingredient)

                        if (response.isNotEmpty()) {
                            val filteredResponse = response
                            val currentRecipeIds = _apiRecetasIds.value.map { it.id }.toSet()
                            val uniqueRecipes = filteredResponse.filterNot { currentRecipeIds.contains(it.id) }

                            // Actualizar la lista de recetas
                            _apiRecetasIds.value = (_apiRecetasIds.value + uniqueRecipes).toList()

                            // Guardar en Firebase en la colección "idsRecetas"
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
                            Log.e("RecetasViewModel", "Respuesta vacía o nula de la API para ingrediente: $ingredient")
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is HttpException -> {
                                when (e.code()) {
                                    429 -> {
                                        // Error HTTP 429: Demasiadas solicitudes
                                        Log.e("RecetasViewModel", "Limite de peticiones alcanzado. Esperando antes de continuar...")
                                        delay(60000)  // Espera de 1 minuto antes de intentar nuevamente
                                    }
                                    402 -> {
                                        // Error HTTP 402: Pago requerido
                                        Log.e("RecetasViewModel", "Error 402: Necesitas pagar para continuar con el servicio.")
                                        // Detener el proceso si se alcanza el error 402
                                        return@launch
                                    }
                                    else -> {
                                        Log.e("RecetasViewModel", "Error desconocido en la solicitud API: ${e.message}")
                                    }
                                }
                            }
                            else -> {
                                Log.e("RecetasViewModel", "Error al obtener las recetas para $ingredient: ${e.message}")
                            }
                        }
                    }

                    // Guardar el último ingrediente procesado en Firestore
                    try {
                        db.collection("config")
                            .document("lastProcessedIngredient")
                            .set(mapOf("ingredient" to ingredient))
                            .addOnSuccessListener {
                                Log.d("RecetasViewModel", "Último ingrediente procesado guardado en config: $ingredient")
                            }
                            .addOnFailureListener { e ->
                                Log.e("RecetasViewModel", "Error al guardar el último ingrediente procesado en config: ${e.message}")
                            }
                    } catch (e: Exception) {
                        Log.e("RecetasViewModel", "Error al guardar el último ingrediente procesado: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener las recetas: ${e.message}")
            }
        }
    }






    // 390 recetas (14 por cada 5 ingredientes)
    fun buscarRecetasPorIngredientes0() {
        val ingredientList = ingredients.split(",") // Dividimos la cadena de ingredientes en una lista
        val batchSize = 3 // de 3 en 3
        //val batchSize = 5
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


    fun logRecetasCount() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // Obtener los IDs de las recetas en 'bulkRecetas'
                val bulkRecetasSnapshot = db.collection("bulkRecetas").get().await()
                val bulkRecetasIds = bulkRecetasSnapshot.documents.map { it.id.toInt() }

                // Obtener los IDs de las recetas en 'idsRecetas'
                val idsRecetasSnapshot = db.collection("idsRecetas").get().await()
                val idsRecetas = idsRecetasSnapshot.documents.mapNotNull { it.getLong("id")?.toInt() }

                // Imprimir el número de recetas en ambas colecciones
                Log.d("RecetasViewModel", "Número de recetas en 'bulkRecetas': ${bulkRecetasIds.size}")
                Log.d("RecetasViewModel", "Número de recetas en 'idsRecetas': ${idsRecetas.size}")

                // Encontrar los IDs que faltan en 'bulkRecetas'
                val missingIds = idsRecetas.filterNot { it in bulkRecetasIds }

                // Imprimir los IDs que faltan
                if (missingIds.isNotEmpty()) {
                    Log.d("RecetasViewModel", "IDs que faltan en 'bulkRecetas': $missingIds")
                } else {
                    Log.d("RecetasViewModel", "No faltan recetas en 'bulkRecetas'.")
                }

                val missingInIdsRecetas = bulkRecetasIds.filterNot { idsRecetas.contains(it) }
                Log.d("RecetasViewModel", "IDs de recetas en 'bulkRecetas' que no están en 'idsRecetas': $missingInIdsRecetas")

                // Obtener y loggear el número de ingredientes en 'bulkIngredients'
                val bulkIngredientsSnapshot = db.collection("bulkIngredients").get().await()
                val bulkIngredientsCount = bulkIngredientsSnapshot.documents.size
                Log.d("RecetasViewModel", "Número de ingredientes en 'bulkIngredients': $bulkIngredientsCount")

            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener los conteos de recetas e ingredientes: ${e.message}")
            }
        }
    }





    // TODO
    fun guardarRecetasBulk() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // Cargar batchIndex desde Firebase antes de iniciar
                loadBatchIndexFromFirebase { loadedBatchIndex ->
                    var batchIndex = loadedBatchIndex // Usar el valor cargado

                    db.collection("idsRecetas").get()
                        .addOnSuccessListener { documents ->
                            val recetaIds = documents.mapNotNull { it.getLong("id")?.toInt() }

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

                            var batch = batches[batchIndex]

                            // Obtener el último ID procesado y filtrar los que ya se guardaron
                            loadLastProcessedRecipeId { lastProcessedId ->
                                if (lastProcessedId != null) {
                                    batch = batch.dropWhile { it != lastProcessedId }.drop(1) // Continuar después del último exitoso
                                }

                                if (batch.isEmpty()) {
                                    Log.d("RecetasViewModel", "No hay nuevas recetas por procesar en este lote.")
                                    return@loadLastProcessedRecipeId
                                }

                                // Solo intentamos guardar el batch si tdo va bien
                                viewModelScope.launch {
                                    try {
                                        val response = api.obtenerRecetasBulk(recetas_ids = batch.joinToString(","))

                                        if (response.isNotEmpty()) {
                                            for (apiReceta in response) {
                                                val recetaId = apiReceta.id.toString()
                                                val documentSnapshot = db.collection("bulkRecetas").document(recetaId).get().await()

                                                if (!documentSnapshot.exists()) {
                                                   val receta = mapApiRecetaToReceta(apiReceta, "", api.obtenerInstruccionesReceta(apiReceta.id))

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
                                                                "image" to it.image,
                                                                "aisle" to it.aisle

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
                                                        .await()

                                                    Log.d("RecetasViewModel", "Receta guardada en Firebase con ID: $recetaId")

                                                    // Guardar este ID como el último procesado
                                                    saveLastProcessedRecipeId(recetaId.toInt())
                                                } else {
                                                    Log.d("RecetasViewModel", "Receta con ID $recetaId ya existe en Firebase, no se guarda.")
                                                }
                                            }

                                            // Si se completó el lote sin errores, avanzar al siguiente batchIndex
                                            batchIndex++
                                            saveBatchIndexToFirebase(batchIndex)
                                        } else {
                                            Log.e("RecetasViewModel", "No se encontraron recetas con los IDs: $batch")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RecetasViewModel", "Error al procesar recetas del lote $batch: ${e.message}")
                                    }
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


    // guardar las rccetas en recetasBulk que faltan de idsRecetas
    fun guardarRecetasBulk2() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                // Cargar batchIndex desde Firebase antes de iniciar
                loadBatchIndexFromFirebase { loadedBatchIndex ->
                    var batchIndex = loadedBatchIndex // Usar el valor cargado

                    db.collection("idsRecetas").get()
                        .addOnSuccessListener { documents ->
                            val recetaIds = documents.mapNotNull { it.getLong("id")?.toInt() }

                            if (recetaIds.isEmpty()) {
                                Log.e("RecetasViewModel", "No hay IDs de recetas en Firebase.")
                                return@addOnSuccessListener
                            }

                            // Obtener los IDs ya existentes en bulkRecetas
                            db.collection("bulkRecetas").get()
                                .addOnSuccessListener { bulkRecetasSnapshot ->
                                    val bulkRecetasIds = bulkRecetasSnapshot.documents.mapNotNull { it.id.toIntOrNull() }

                                    // Filtrar solo los IDs que no están en bulkRecetas
                                    val missingRecetaIds = recetaIds.filterNot { it in bulkRecetasIds }

                                    if (missingRecetaIds.isEmpty()) {
                                        Log.d("RecetasViewModel", "No hay recetas faltantes por procesar.")
                                        return@addOnSuccessListener
                                    }

                                    // Crear la cadena de IDs separados por comas
                                    val recetaIdsString = missingRecetaIds.joinToString(",") { it.toString() }

                                    // Procesar las recetas faltantes
                                    viewModelScope.launch {
                                        try {
                                            val response = api.obtenerRecetasBulk(recetas_ids = recetaIdsString)

                                            if (response.isNotEmpty()) {
                                                for (apiReceta in response) {
                                                    val recetaId = apiReceta.id.toString()
                                                    val documentSnapshot = db.collection("bulkRecetas").document(recetaId).get().await()

                                                    if (!documentSnapshot.exists()) {
                                                        val receta = mapApiRecetaToReceta(apiReceta, "", api.obtenerInstruccionesReceta(apiReceta.id))

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
                                                                    "image" to it.image,
                                                                    "aisle" to it.aisle
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
                                                            .await()

                                                        Log.d("RecetasViewModel", "Receta guardada en Firebase con ID: $recetaId")

                                                        // Guardar este ID como el último procesado
                                                        saveLastProcessedRecipeId(recetaId.toInt())
                                                    } else {
                                                        Log.d("RecetasViewModel", "Receta con ID $recetaId ya existe en Firebase, no se guarda.")
                                                    }
                                                }

                                                // Si se completó la operación sin errores, guardar el batchIndex
                                                saveBatchIndexToFirebase(batchIndex + 1)
                                            } else {
                                                Log.e("RecetasViewModel", "No se encontraron recetas con los IDs: $recetaIdsString")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RecetasViewModel", "Error al procesar recetas con los IDs: $recetaIdsString: ${e.message}")
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("RecetasViewModel", "Error al obtener las recetas de bulkRecetas: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecetasViewModel", "Error al obtener los IDs de recetas: ${e.message}")
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


    fun saveLastProcessedRecipeId(recipeId: Int) {
        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf("lastProcessedId" to recipeId)

        db.collection("config")
            .document("lastProcessedRecipe")
            .set(data)
            .addOnSuccessListener {
                Log.d("RecetasViewModel", "Último ID procesado guardado: $recipeId")
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al guardar último ID procesado: ${e.message}")
            }
    }

    fun loadLastProcessedRecipeId(callback: (Int?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("config")
            .document("lastProcessedRecipe")
            .get()
            .addOnSuccessListener { document ->
                val lastId = document.getLong("lastProcessedId")?.toInt()
                callback(lastId)
            }
            .addOnFailureListener { e ->
                Log.e("RecetasViewModel", "Error al cargar último ID procesado: ${e.message}")
                callback(null)
            }
    }


    fun obtenerValoresAisleUnicos(): List<String> {
        val aislesUnicos = mutableSetOf<String>()  // Usamos un Set para evitar duplicados

        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            try {
                // Obtener todas las recetas
                val recetaDocs = db.collection("bulkRecetas").get().await()
                val recetaIds = recetaDocs.mapNotNull { it.id.toIntOrNull() }

                // Recorrer las recetas y extraer los valores de aisle
                for (recetaId in recetaIds) {
                    val recetaDoc = db.collection("bulkRecetas").document(recetaId.toString()).get().await()
                    val ingredientes = recetaDoc.get("ingredients") as? List<Map<String, Any>> ?: continue

                    // Extraer los valores únicos de aisle
                    for (ingrediente in ingredientes) {
                        val aisle = ingrediente["aisle"] as? String
                        if (!aisle.isNullOrEmpty()) {
                            aislesUnicos.add(aisle)  // Añadir al Set, evitando duplicados
                        }
                    }
                }

                // Convertir el Set a lista y mostrarla
                val aislesList = aislesUnicos.toList()
                Log.d("ObtenerAisleUnicos", "Valores únicos de aisle: $aislesList")

            } catch (e: Exception) {
                Log.e("ObtenerAisleUnicos", "Error al obtener los valores de aisle: ${e.message}")
            }
        }

        return aislesUnicos.toList()  // Devolver la lista de aisles únicos
    }


    fun actualizarAisleEnRecetas() {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            try {
                // Obtener el progreso previo
                val progressDoc = db.collection("config").document("aisleProgress").get().await()
                var lastProcessedId = progressDoc.getLong("idParaAisle")?.toInt()
                var lastBatchIndex = progressDoc.getLong("batchAisle")?.toInt() ?: 0

                // Obtener todas las recetas
                val recetaDocs = db.collection("bulkRecetas").get().await()
                val recetaIds = recetaDocs.mapNotNull { it.id.toIntOrNull() }

                val batches = recetaIds.chunked(100)
                if (batches.isEmpty()) {
                    Log.d("ActualizarAisle", "No hay recetas en Firebase.")
                    return@launch
                }

                // Si ya procesamos todos los batches, reiniciamos el progreso
                if (lastBatchIndex >= batches.size) {
                    Log.d("ActualizarAisle", "Se procesaron todas las recetas. Reiniciando progreso.")
                    db.collection("config").document("aisleProgress")
                        .set(mapOf("idParaAisle" to null, "batchAisle" to 0), SetOptions.merge())
                        .await()
                    lastBatchIndex = 0
                    lastProcessedId = null
                }

                // Avanzar al siguiente batch si el actual está vacío o el último ID era el último del batch
                while (lastBatchIndex < batches.size) {
                    var batch = batches[lastBatchIndex]

                    // Si hay un lastProcessedId, quitar las recetas ya procesadas dentro del batch
                    if (lastProcessedId != null) {
                        val index = batch.indexOf(lastProcessedId)
                        if (index != -1) {
                            batch = batch.drop(index + 1)
                        }
                    }

                    // Si el batch sigue vacío, avanzar al siguiente batch
                    if (batch.isEmpty()) {
                        lastBatchIndex++
                        continue
                    }

                    // Obtener recetas desde la API
                    val response = api.obtenerRecetasBulk(recetas_ids = batch.joinToString(","))
                    if (response.isEmpty()) {
                        Log.e("ActualizarAisle", "No se encontraron recetas en la API para batch $lastBatchIndex")
                        lastBatchIndex++
                        continue
                    }

                    // Actualizar en Firebase
                    for ((index, apiReceta) in response.withIndex()) {
                        val recetaId = apiReceta.id.toString()
                        val updatedIngredients = apiReceta.extendedIngredients.map { apiIng ->
                            mapOf(
                                "name" to apiIng.name,
                                "amount" to apiIng.amount,
                                "unit" to apiIng.unit,
                                "image" to apiIng.image,
                                "aisle" to apiIng.aisle
                            )
                        }

                        db.collection("bulkRecetas").document(recetaId)
                            .update("ingredients", updatedIngredients)
                            .await()
                        Log.d("ActualizarAisle", "Receta $recetaId actualizada con aisle.")

                        // Guardar el progreso
                        db.collection("config").document("aisleProgress")
                            .set(mapOf("idParaAisle" to apiReceta.id, "batchAisle" to lastBatchIndex), SetOptions.merge())
                            .await()

                        // Si es la última receta del batch, avanzar al siguiente
                        if (index == response.lastIndex) {
                            lastBatchIndex++
                        }
                    }

                    // Guardar el nuevo batch index
                    db.collection("config").document("aisleProgress")
                        .set(mapOf("batchAisle" to lastBatchIndex), SetOptions.merge())
                        .await()

                    return@launch // Salir después de procesar un batch
                }

                Log.d("ActualizarAisle", "Todos los batches han sido procesados.")

            } catch (e: Exception) {
                Log.e("ActualizarAisle", "Error en el proceso de actualización: ${e.message}")
            }
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






}



