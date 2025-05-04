package com.example.recipeat.data.repository

import android.util.Log
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
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

class RecetaRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // para paginacion
    private var lastDocument: DocumentSnapshot? = null

    // Genera un ID único para la receta
    fun generateRecipeId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    // Obtiene una receta desde un DocumentSnapshot de Firebase
    fun obtenerRecetaDesdeSnapshot(document: DocumentSnapshot): Receta {
        return Receta(
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
            unusedIngredientCount = 0
        )
    }

    private var lastDocumentUser: DocumentSnapshot? = null
    // Función para obtener las recetas del usuario con paginación
    suspend fun getRecetasUser(uid: String, limpiarLista: Boolean = true): List<Receta> {
        val recetas = mutableListOf<Receta>()

        // Crea la consulta inicial con un límite de 15
        var query = db.collection("my_recipes").document(uid)
            .collection("recipes")
            .orderBy("date", Query.Direction.DESCENDING) // Ordena por fecha de manera descendente
            .limit(15)

        // Si ya hay un documento anterior, usa startAfter para continuar desde ese punto
        if (lastDocumentUser != null && !limpiarLista) {
            Log.d("RecetaRepository", "Recetas User: Paginar desde documento: ${lastDocumentUser?.id}")
            query = query.startAfter(lastDocumentUser!!)
        } else {
            Log.d("RecetaRepository", "Recetas User: No hay documento previo, iniciando desde el principio")
        }

        // Ejecutar la consulta en un coroutine
        try {
            val documents = query.get().await() // Obtener los documentos

            if (!documents.isEmpty) {
                // Actualiza el último documento
                lastDocumentUser = documents.documents.last()

                // Mapea los documentos a una lista de recetas
                recetas.addAll(documents.documents.mapNotNull { document ->
                    try {
                        obtenerRecetaDesdeSnapshot(document) // Función para mapear el documento a un objeto Receta
                    } catch (e: Exception) {
                        Log.e("RecetaRepository", "Recetas User: Error al mapear receta: ${e.message}")
                        null
                    }
                })

                Log.d("RecetaRepository", "Total recetas User: ${recetas.size}")
            } else {
                Log.d("RecetaRepository", "No hay más recetas User para cargar.")
            }
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Recetas User: Error al obtener recetas: ${e.message}")
        }

        return recetas
    }

    /**
     * Añade a Firebase una receta creada por el user.
     */
    fun addMyRecipe(uid: String, receta: Receta, onComplete: (Boolean, String?) -> Unit) {
        Log.d("RecetaRepository", "recetaId generado: ${receta.id}")

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
            "userId" to receta.userId,
            "glutenFree" to receta.glutenFree,
            "vegan" to receta.vegan,
            "vegetarian" to receta.vegetarian,
            "date" to receta.date
        )

        // Referencia al documento de la receta
        val recipeRef =
            db.collection("my_recipes").document(uid).collection("recipes").document(receta.id)

        Log.d("RecetaRepository", "Referencia del documento de la receta: ${recipeRef.path}")

        // Guardar el HashMap en Firestore
        recipeRef.set(recipeMap)
            .addOnSuccessListener {
                Log.d("RecetaRepository", "Receta ${receta.id} guardada correctamente en Firestore")
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("RecetaRepository", "Error al guardar receta ${receta.id} en Firestore: ${e.message}")
                onComplete(false, e.message)
            }
    }


    /**
     * Edita una receta en Firebase solo con los campos modificados.
     */
    fun editMyRecipe(uid: String, recetaModificada: Receta, onComplete: (Boolean, String?) -> Unit) {
        Log.d("RecetaRepository", "Editando receta con id: ${recetaModificada.id}")


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

                // Convertir manualmente el mapa a un objeto Receta de User
                val recetaOriginal = Receta(
                    id = recetaOriginalData["id"] as String,
                    title = recetaOriginalData["title"] as String,
                    image = recetaOriginalData["image"] as? String,
                    servings = (recetaOriginalData["servings"] as Number).toInt(),  // Convierte el valor a Int, o usa 0 si es nulo
                    ingredients = recetaOriginalData["ingredients"] as List<Ingrediente>,
                    steps = recetaOriginalData["steps"] as List<String>,
                    time = (recetaOriginalData["time"] as Number).toInt(),
                    userId = recetaOriginalData["userId"] as String,
                    dishTypes = recetaOriginalData["dishTypes"] as List<String>,
                    //usedIngredientCount = it["usedIngredientCount"] as Int,
                    glutenFree = recetaOriginalData["glutenFree"] as Boolean,
                    vegan = recetaOriginalData["vegan"] as Boolean,
                    vegetarian = recetaOriginalData["vegetarian"] as Boolean,
                    date = recetaOriginalData["date"] as Long,
                    unusedIngredients = emptyList(),
                    missingIngredientCount = 0,
                    unusedIngredientCount = 0
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

                // Verificar si hay cambios en los campos de la receta
                if (updatesMap.isNotEmpty()) {
                    // Actualizar solo los campos modificados en Firestore
                    recipeRef.update(updatesMap)
                        .addOnSuccessListener {
                            // Ahora, actualizar la colección de favoritos (favs_hist/uid)
                            val favoritoData = hashMapOf(
                                "idReceta" to recetaModificada.id,
                                "title" to recetaModificada.title,
                                "image" to recetaModificada.image.toString(),
                                "date" to Timestamp.now(),
                                "user" to uid
                            )

                            // Actualizamos el favorito en la colección de favoritos
                            val favHistRef = db.collection("favs_hist").document(uid).collection("favoritos").document(recetaModificada.id)
                            favHistRef.set(favoritoData)
                                .addOnSuccessListener {
                                    onComplete(true, null)
                                }
                                .addOnFailureListener { e ->
                                    onComplete(false, e.message)
                                }
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
    fun mapApiRecetaToReceta(apiReceta: ApiReceta, uid: String, analyzedInstructions: List<Map<String, Any>> ): Receta {

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
            ingredients = apiReceta.extendedIngredients,
            steps = pasos,
            time = apiReceta.readyInMinutes,
            dishTypes = apiReceta.dishTypes,
            userId = uid,  // ID del usuario que crea la receta
            glutenFree = apiReceta.glutenFree,
            vegan = apiReceta.vegan,
            vegetarian = apiReceta.vegetarian,
            date = System.currentTimeMillis(),
            unusedIngredients = emptyList(),
            missingIngredientCount = 0,
            unusedIngredientCount = 0
        )
    }



    /**
     * Obtener recetas para la pantalla principal (Home)
     * @param limpiarLista Boolean para saber si se debe limpiar la lista antes de cargar las nuevas recetas
     */
    suspend fun obtenerRecetasHome(limpiarLista: Boolean = true): Result<List<Receta>> {
        return try {
            var query = db.collection("bulkRecetas")
                .orderBy("id") // Ordena por ID
                .limit(15)

            // Si no es la primera vez que se cargan recetas, se realiza paginación.
            if (lastDocument != null && !limpiarLista) {
                query = query.startAfter(lastDocument?.get("id"))
            }

            val documents = query.get().await()

            // Si se encuentran documentos, los procesamos.
            if (!documents.isEmpty) {
                lastDocument = documents.documents.last() // Actualizar último documento cargado

                val nuevasRecetas = documents.mapNotNull { document ->
                    try {
                        obtenerRecetaDesdeSnapshot(document) // Convertir el snapshot a un objeto Receta
                    } catch (e: Exception) {
                        Log.e("RecetaRepository", "Error al mapear receta: ${e.message}")
                        null
                    }
                }

                Result.success(nuevasRecetas)
            } else {
                // Si no se encontraron documentos, devolvemos una lista vacía.
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            // En caso de error, se captura y devuelve el error en un Result.failure.
            Result.failure(e)
        }
    }

    /**
     * Obtener receta por ID
     * @param uid ID del usuario (si es una receta del usuario)
     * @param recetaId ID de la receta
     * @param deUser Booleano para saber si se busca en las recetas del usuario o en las recetas generales
     */
    suspend fun obtenerRecetaPorId(uid: String, recetaId: String, deUser: Boolean): Result<Receta> {
        return try {
            // Se construye la referencia al documento según si es una receta del usuario o no.
            val documentRef = if (deUser) {
                db.collection("my_recipes") // Colección de recetas del usuario
                    .document(uid) // Documento del usuario
                    .collection("recipes") // Subcolección de recetas
                    .document(recetaId) // Documento específico de la receta
            } else {
                db.collection("bulkRecetas") // Colección de recetas generales
                    .document(recetaId)
            }

            // Obtenemos el documento.
            val document = documentRef.get().await()

            // Si el documento existe, lo convertimos a una receta y la devolvemos.
            if (document.exists()) {
                val receta = obtenerRecetaDesdeSnapshot(document)
                Result.success(receta)
            } else {
                // Si no existe el documento, devolvemos un error.
                Result.failure(Throwable("Receta no encontrada"))
            }
        } catch (e: Exception) {
            // En caso de error, se captura y devuelve el error en un Result.failure.
            Result.failure(e)
        }
    }



    suspend fun eliminarReceta(uid: String, recetaId: String) {
        try {
            val documentRef = db.collection("my_recipes")
                .document(uid)
                .collection("recipes")
                .document(recetaId)

            // Eliminar el documento
            documentRef.delete().await()

            Log.d("RecetaRepository", "Receta con ID: $recetaId eliminada exitosamente")
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error al eliminar receta: ${e.message}")
            throw e
        }
    }

    suspend fun eliminarRecetaDelHistorial(uid: String, recetaId: String) {
        try {
            val historialRef = db.collection("favs_hist")
                .document(uid)
                .collection("historial")

            val historialSnapshot = historialRef.whereEqualTo("id", recetaId).get().await()

            if (historialSnapshot.isEmpty) {
                Log.d("RecetaRepository", "La receta no se encuentra en el historial.")
                return
            }

            // Eliminar todas las instancias de la receta en el historial
            for (document in historialSnapshot.documents) {
                historialRef.document(document.id).delete().await()
                Log.d("RecetaRepository", "Receta $recetaId eliminada del historial.")
            }
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error al eliminar receta del historial: ${e.message}")
            throw e
        }
    }


    /**
     * Devuelve las recetas (tanto las de Firebase como las creadas por el usuario) que contienen todos o algunos de los ingredientes a buscar
     * Por defecto salen las rcetas con más coincidencias de ingredientes buscados, 1º las de
     * firebase y luego las del user.
     **/
    suspend fun buscarRecetasPorIngredientes(
        ingredientes: List<IngredienteSimple>,
        userId: String
    ): List<Receta> {
        val recetasCoincidentesTotales = mutableListOf<Receta>()
        val recetasCoincidentesParciales = mutableListOf<Pair<Receta, Int>>()

        val documentosPublicos = db.collection("bulkRecetas").get().await()
        val documentosUsuario = try {
            db.collection("my_recipes")
                .document(userId)
                .collection("recipes")
                .get()
                .await()
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error al obtener recetas personales", e)
            null
        }

        val todosLosDocumentos = documentosPublicos.documents +
                (documentosUsuario?.documents ?: emptyList())

        for (document in todosLosDocumentos) {
            val recetaNombre = document.getString("title")
            val recetaIngredientes = document.get("ingredients") as? List<Map<String, Any>>

            if (recetaNombre != null && recetaIngredientes != null) {
                val ingredientesReceta = recetaIngredientes.map {
                    Ingrediente(
                        name = (it["name"] as? String)?.lowercase() ?: "",
                        image = it["image"] as? String ?: "",
                        amount = (it["amount"] as? Double) ?: 0.0,
                        unit = it["unit"] as? String ?: "",
                        aisle = it["aisle"] as? String ?: "",
                    )
                }

                val missingIngredients = ingredientesReceta.filterNot { ingrediente ->
                    ingredientes.any { it.name == ingrediente.name }
                }

                val unusedIngredients = ingredientes.filterNot { ingrediente ->
                    ingredientesReceta.any { it.name == ingrediente.name }
                }

                val coincidencias = ingredientes.count { ingrediente ->
                    ingredientesReceta.any { it.name == ingrediente.name }
                }

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
                    unusedIngredients = unusedIngredients,
                    missingIngredientCount = missingIngredients.size,
                    unusedIngredientCount = unusedIngredients.size,
                    glutenFree = document.getBoolean("glutenFree") ?: false,
                    vegan = document.getBoolean("vegan") ?: false,
                    vegetarian = document.getBoolean("vegetarian") ?: false,
                    date = document.getLong("date") ?: System.currentTimeMillis()
                )

                if (coincidencias == ingredientes.size) {
                    recetasCoincidentesTotales.add(receta)
                } else if (coincidencias > 0) {
                    recetasCoincidentesParciales.add(Pair(receta, coincidencias))
                }
            }
        }

        val recetasOrdenadas = recetasCoincidentesParciales
            .sortedByDescending { it.second }
            .map { it.first }

        return recetasCoincidentesTotales + recetasOrdenadas
    }


    // Búsqueda estricta en tiempo real (requiere que el título contenga TODAS las palabras)
    suspend fun obtenerSugerenciasPorNombre(name: String): List<SugerenciaReceta> {
        val palabrasClave = name.lowercase().split(" ")
            .filter { it.isNotBlank() && it !in listOf("and", "with", "all") }

        if (palabrasClave.isEmpty()) return emptyList()

        return try {
            val result = db.collection("bulkRecetas").get().await()
            result.documents.mapNotNull { document ->
                val recetaId = document.id
                val recetaNombre = document.getString("title")?.lowercase()

                // Buscar solo recetas que contengan TODAS las palabras ingresadas
                recetaNombre?.takeIf { palabrasClave.all { palabra -> it.contains(palabra) } }?.let {
                    val coincidencias = palabrasClave.size
                    SugerenciaReceta(recetaId, document.getString("title") ?: "", coincidencias)
                }
            }.sortedByDescending { it.coincidencias }
                .take(7)  // Limitar a 7 sugerencias
        } catch (e: Exception) {
            Log.e("Firebase", "Error al obtener sugerencias en tiempo real: ", e)
            emptyList()
        }
    }


    /**
     * Se hace una doble consulta: primero a bulkRecetas, luego a my_recipes/userId/recipes.
     * Ambas listas se combinan (documents + documents) y se procesan con la misma lógica.
     * Si una receta tiene al menos una palabra coincidente, se añade a los resultados.
     *
     * Las recetas del user van al final
     **/
    suspend fun obtenerRecetasPorNombre(prefix: String, userId: String): List<Receta> {
        // Se dividen las palabras clave ignorando conectores comunes
        val palabrasClave = prefix.lowercase().split(" ")
            .filter { it.isNotBlank() && it !in listOf("and", "with", "all") }

        // Si no hay palabras clave válidas, se retorna una lista vacía
        if (palabrasClave.isEmpty()) return emptyList()

        return try {
            // Obtener recetas públicas y del usuario de Firebase
            val result1 = db.collection("bulkRecetas").get().await()
            val result2 = db.collection("my_recipes").document(userId).collection("recipes").get().await()

            val todosLosDocumentos = result1.documents + result2.documents

            todosLosDocumentos.mapNotNull { document ->
                val recetaId = document.id
                val recetaNombre = document.getString("title")?.lowercase()

                recetaNombre?.let { nombre ->
                    // Contar cuántas palabras clave están presentes en el título
                    val coincidencias = palabrasClave.count { palabra -> nombre.contains(palabra) }

                    // Solo incluir recetas con al menos una coincidencia
                    if (coincidencias > 0) {
                        Receta(
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
                            unusedIngredientCount = 0
                        )
                    } else null
                }
            }.sortedByDescending { receta ->
                // Ordenar recetas por la cantidad de coincidencias en el título
                palabrasClave.count { palabra -> receta.title.lowercase().contains(palabra) }
            }

        } catch (e: Exception) {
            Log.e("Firebase", "Error al obtener recetas: ", e)
            emptyList()
        }
    }


    /**
     * Añade o elimina una receta de favoritos para el usuario.
     * Si la receta ya está en favoritos, la elimina. Si no, la añade con metadatos.
     */
    suspend fun toggleFavorito(uid: String, userReceta: String, recetaId: String, title: String, image: String): Boolean {
        val favoritosRef = db.collection("favs_hist").document(uid).collection("favoritos")
        val doc = favoritosRef.document(recetaId).get().await()

        return if (doc.exists()) {
            // Eliminar si ya está en favoritos
            favoritosRef.document(recetaId).delete().await()
            false
        } else {
            // Añadir a favoritos con datos
            val favoritoData = hashMapOf(
                "id" to recetaId,
                "title" to title,
                "image" to image,
                "date" to Timestamp.now(),
                "userId" to userReceta
            )
            favoritosRef.document(recetaId).set(favoritoData).await()
            true
        }
    }

    /**
     * Verifica si una receta está marcada como favorita por el usuario.
     */
    suspend fun verificarSiEsFavorito(uid: String, recetaId: String): Boolean {
        val favoritosRef = db.collection("favs_hist").document(uid).collection("favoritos")
        val doc = favoritosRef.document(recetaId).get().await()
        return doc.exists()
    }

    /**
     * Añade una entrada al historial del usuario con los datos básicos de la receta.
     */
    suspend fun añadirHistorial(uid: String, userReceta: String, recetaId: String, title: String, image: String) {
        Log.d("RecetaRepository", "Añadiendo receta al historial para el usuario $uid, receta: $title")
        val historialRef = db.collection("favs_hist").document(uid).collection("historial")
        val historialEntry = RecetaSimple(
            id = recetaId,
            title = title,
            image = image,
            date = Timestamp.now(),
            userId = userReceta
        )

        try {
            historialRef.add(historialEntry).await()
            Log.d("RecetaRepository", "Receta añadida al historial exitosamente: $title")
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error al añadir la receta al historial: $e")
        }
    }

    /**
     * Obtiene las recetas favoritas del usuario ordenadas por la fecha de añadido.
     */
    suspend fun obtenerRecetasFavoritas(uid: String): List<RecetaSimple> {
        Log.d("RecetaRepository", "Obteniendo recetas favoritas del usuario $uid")
        val favoritosRef = db.collection("favs_hist")
            .document(uid)
            .collection("favoritos")
            .orderBy("date", Query.Direction.DESCENDING)

        return try {
            val snapshot = favoritosRef.get().await()
            if (snapshot.isEmpty) {
                Log.d("RecetaRepository", "No se encontraron recetas favoritas para el usuario $uid")
                emptyList()
            } else {
                val recetas = snapshot.documents.mapNotNull { document ->
                    val recetaId = document.getString("id")
                    val title = document.getString("title")
                    val image = document.getString("image") ?: ""
                    val date = document.getTimestamp("date")
                    val userId = document.getString("userId") ?: ""

                    if (recetaId != null && title != null && date != null) {
                        RecetaSimple(
                            id = recetaId,
                            title = title,
                            image = image,
                            date = date,
                            userId = userId
                        )
                    } else {
                        null
                    }
                }
                Log.d("RecetaRepository", "Recetas favoritas obtenidas: ${recetas.size}")
                recetas
            }
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error al obtener recetas favoritas: $e")
            emptyList()
        }
    }

    /**
     * Obtiene las recetas en el historial del usuario dentro de un rango de fechas.
     */
    suspend fun obtenerRecetasPorRangoDeFecha(uid: String, rango: Int): List<RecetaSimple> {
        Log.d("RecetaRepository", "Obteniendo recetas en el historial del usuario $uid dentro del rango de $rango días")
        val currentDate = Timestamp.now()
        val startDate = when (rango) {
            30 -> Timestamp(currentDate.seconds - 30L * 24 * 60 * 60, 0) // Rango de 30 días (Restamos 30 días en seg)
            7 -> Timestamp(currentDate.seconds - 7L * 24 * 60 * 60, 0) // Rango de 7 días
            else -> return emptyList()
        }

        try {
            val recetasRef = db.collection("favs_hist")
                .document(uid)
                .collection("historial")
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.DESCENDING)

            val snapshot = recetasRef.get().await()
            if (snapshot.isEmpty) {
                Log.d("RecetaRepository", "No se encontraron recetas en el historial para el rango de fechas $rango días")
                return emptyList()
            } else {
                val recetas = snapshot.documents.mapNotNull { document ->
                    val recetaId = document.getString("id")
                    val title = document.getString("title")
                    val image = document.getString("image") ?: ""
                    val date = document.getTimestamp("date")
                    val userId = document.getString("userId") ?: ""

                    if (recetaId != null && title != null && date != null) {
                        RecetaSimple(
                            id = recetaId,
                            title = title,
                            image = image,
                            date = date,
                            userId = userId
                        )
                    } else {
                        null
                    }
                }
                Log.d("RecetaRepository", "Recetas en el historial obtenidas: ${recetas.size}")
                return recetas
            }
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error al obtener recetas por rango de fechas: $e")
            return emptyList()
        }
    }

    /**
     * Función que verifica si la imagen de un equipo está disponible en Spoonacular
     * y guarda la información del equipo en Firestore si la imagen no está guardada aún.
     */
    suspend fun checkAndSaveEquipmentImage(equipmentName: String) {
        Log.d("RecetaRepository", "Verificando imagen del equipo: $equipmentName")
        val client = OkHttpClient()

        // Formatear el nombre del equipo para crear la URL
        val formattedName = equipmentName.lowercase().replace(" ", "-")
        val imageUrl = "https://img.spoonacular.com/equipment_100x100/$formattedName.jpg"

        // Crear la petición HTTP para verificar si la imagen está disponible
        val request = Request.Builder().url(imageUrl).build()

        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                // Si la imagen existe, comprobar si ya está guardada en Firestore
                val equipmentQuery = db.collection("equipment")
                    .whereEqualTo("imageUrl", imageUrl)

                val querySnapshot = equipmentQuery.get().await()

                if (querySnapshot.isEmpty) {
                    // Si no existe, guardar el equipo
                    val equipmentData = hashMapOf(
                        "name" to equipmentName,
                        "imageUrl" to imageUrl
                    )

                    db.collection("equipment")
                        .add(equipmentData)
                        .addOnSuccessListener { documentReference ->
                            Log.d("RecetaRepository", "Equipo guardado exitosamente con ID: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecetaRepository", "Error guardando el equipo: $e")
                        }
                } else {
                    // Si ya existe, no hacer nada
                    Log.d("RecetaRepository", "El equipo ya existe en Firestore")
                }
            } else {
                // Si la imagen no está disponible
                Log.d("RecetaRepository", "No se encontró imagen para $equipmentName")
            }
        } catch (e: Exception) {
            Log.e("RecetaRepository", "Error durante la petición de red: $e")
        }
    }


    /**
     * Función que actualiza los pasos de la receta con las imágenes de los equipos.
     */
    suspend fun updateEquipmentSteps(steps: List<String>): List<List<String>> {
        Log.d("RecetaRepository", "Actualizando pasos con imágenes de los equipos")
        val querySnapshot = db.collection("equipment").get().await()

        val equipos = mutableMapOf<String, String>()

        // Obtener los equipos y sus imágenes desde Firestore
        querySnapshot.documents.forEach { document ->
            val nombreEquipo = document.getString("name") ?: ""
            val urlImagen = document.getString("imageUrl") ?: ""

            if (nombreEquipo.isNotEmpty() && urlImagen.isNotEmpty()) {
                equipos[nombreEquipo] = urlImagen
            }
        }

        val stepsWithImages = mutableListOf<List<String>>()

        // Para cada paso de la receta, buscar los equipos presentes en él
        steps.forEach { step ->
            val equipmentImages = mutableListOf<String>()

            equipos.forEach { (equipo, imageUrl) ->
                if (step.contains(equipo, ignoreCase = true)) {
                    equipmentImages.add(imageUrl)
                }
            }

            stepsWithImages.add(equipmentImages)
        }

        Log.d("RecetaRepository", "Pasos actualizados con imágenes: $stepsWithImages")
        return stepsWithImages
    }



}