package com.example.recipeat.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.recipeat.BuildConfig.API_GEMINI_KEY
import com.example.recipeat.data.model.DayMeal
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.IngredienteCompra
import com.example.recipeat.data.model.PlanSemanal
import com.example.recipeat.data.model.Receta
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlanRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Calculamos el identificador de la semana actual en formato "día/mes"
     * El día es el lunes de la semana actual
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerSemanaActualId(): String {
        val fechaActual = LocalDate.now()
        val primerDiaSemanaActual = fechaActual.with(DayOfWeek.MONDAY)

        return "${primerDiaSemanaActual.dayOfMonth}-${primerDiaSemanaActual.monthValue}"
    }

    /**
     * Calculamos el identificador de la semana anterior en formato "día/mes"
     * El día es el lunes de la semana anterior
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerSemanaAnteriorId(): String {

        val fechaActual = LocalDate.now()
        val primerDiaSemanaAnterior = fechaActual.minusWeeks(1).with(DayOfWeek.MONDAY)

        return "${primerDiaSemanaAnterior.dayOfMonth}-${primerDiaSemanaAnterior.monthValue}"
    }


    // Función para verificar si es lunes para actualizar el plan
    @RequiresApi(Build.VERSION_CODES.O)
    fun esLunes(): Boolean {
        val hoy = LocalDate.now()
        return hoy.dayOfWeek == DayOfWeek.MONDAY
    }


    // Función que ejecuta el Worker cada lunes que genera el plan semanal
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun iniciarGeneracionPlanSemanal(userId: String) {
        if (esLunes()) {
            Log.d("PlanSemanal", "Hoy es lunes, actualizando el plan semanal...")
            val recetas = obtenerRecetasFirebase()
            if (recetas.isNotEmpty()) {
                val planSemanal = generarPlanSemanal(recetas, userId)
                actualizarYGuardarPlanSemanal(userId, planSemanal)
                guardarListaDeLaCompraEnFirebase(userId, planSemanal)
            } else {
                Log.e("PlanSemanal", "No se encontraron recetas para generar el plan")
            }
        } else {
            Log.d("PlanSemanal", "Hoy no es lunes. No se actualiza el plan semanal.")
        }
    }


    // Si es la primera vez que el user entra (register), se genera un plan inicial
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun iniciarGeneracionPlanSemanalInicial(userId: String) {
        Log.d("PlanSemanal", "Primera vez que el usuario entra, generando el plan semanal.")
        val recetas = obtenerRecetasFirebase()

        if (recetas.isNotEmpty()) {
            val planSemanal = generarPlanSemanal(recetas, userId)
            actualizarYGuardarPlanSemanal(userId, planSemanal)
            val guardado = guardarListaDeLaCompraEnFirebase(userId, planSemanal)
            if (!guardado) Log.e("PlanSemanal", "Error guardando la lista de la compra")
        } else {
            Log.e("PlanSemanal", "No se encontraron recetas para generar el plan")
        }
    }


    // Obtener ingredientes desde el plan semanal
    fun obtenerIngredientesComoLista(planSemanal: PlanSemanal): List<Ingrediente> {
        val ingredientesList = mutableListOf<Ingrediente>()

        planSemanal.weekMeals.values.forEach { dayMeal ->
            listOf(dayMeal.breakfast, dayMeal.lunch, dayMeal.dinner).forEach { receta ->
                ingredientesList.addAll(receta.ingredients)
            }
        }

        return ingredientesList
    }

    // Guardar la lista de la compra en Firebase
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun guardarListaDeLaCompraEnFirebase(uid: String, planSemanal: PlanSemanal): Boolean {
        Log.d("PlanViewModel", "Empezando a guardar lista de la compra...")
        // Paso 1: Obtener ingredientes del plan semanal
        val ingredientes = obtenerIngredientesComoLista(planSemanal)

        // Paso 2: Procesar ingredientes con Gemini (agrupados y filtrados)
        val ingredientesAgrupados = getGroupedIngredients(ingredientes)

        // Paso 3: Verificar si hay ingredientes válidos para guardar
        if (ingredientesAgrupados.isEmpty())  return false

        val ingredientesList = ingredientesAgrupados.map { ingrediente ->
            mapOf(
                "name" to ingrediente.name,
                "aisle" to ingrediente.aisle,
                "image" to ingrediente.image,
                "medidas" to ingrediente.medidas.map { medida ->
                    mapOf(
                        "amount" to medida.first,
                        "unit" to medida.second
                    )
                },
                "estaComprado" to ingrediente.estaComprado
            )
        }

        return suspendCoroutine { continuation ->
            firestore.collection("planSemanal")
                .document(uid)
                .collection("listaCompra")
                .document("listaCompraActual")
                .set(mapOf("ingredientes" to ingredientesList))
                .addOnSuccessListener { continuation.resume(true) }
                .addOnFailureListener { continuation.resume(false) }
        }
    }


    // Obtener lista de la compra desde Firebase
    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerListaDeLaCompraDeFirebase(uid: String, onComplete: (List<IngredienteCompra>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("planSemanal")
            .document(uid)
            .collection("listaCompra")
            .document("listaCompraActual")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ingredientesList = document.get("ingredientes") as? List<Map<String, Any>> ?: emptyList()

                    val listaCompra = ingredientesList.map { ingrediente ->
                        val name = ingrediente["name"] as? String ?: ""
                        val aisle = ingrediente["aisle"] as? String ?: ""
                        val image = ingrediente["image"] as? String ?: ""
                        val medidas = ingrediente["medidas"] as? List<Map<String, Any>> ?: emptyList()
                        val estaComprado = ingrediente["estaComprado"] as? Boolean ?: false

                        val medidasList = medidas.map { medida ->
                            Pair(medida["amount"] as? Double ?: 0.0, medida["unit"] as? String ?: "")
                        }

                        IngredienteCompra(
                            name = name,
                            aisle = aisle,
                            image = image,
                            medidas = medidasList,
                            estaComprado = estaComprado
                        )
                    }

                    onComplete(listaCompra)
                } else {
                    onComplete(emptyList())
                }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    // Actualizar el estado del ingrediente en Firebase
    @RequiresApi(Build.VERSION_CODES.O)
    fun actualizarEstadoIngredienteEnFirebase(uid: String, nombreIngrediente: String, estaComprado: Boolean, onComplete: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("planSemanal")
            .document(uid)
            .collection("listaCompra")
            .document("listaCompraActual")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ingredientesList = document.get("ingredientes") as? List<Map<String, Any>> ?: emptyList()

                    val ingredientesActualizados = ingredientesList.map { ingrediente ->
                        val ingredienteMap = ingrediente.toMutableMap()
                        val nombre = ingredienteMap["name"] as? String ?: ""

                        if (nombre == nombreIngrediente) {
                            ingredienteMap["estaComprado"] = estaComprado
                        }

                        ingredienteMap
                    }

                    firestore.collection("planSemanal")
                        .document(uid)
                        .collection("listaCompra")
                        .document("listaCompraActual")
                        .set(mapOf("ingredientes" to ingredientesActualizados))
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                } else {
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }


    //RESTRICCIONES

    /**
     * Filtra las recetas aptas para el desayuno.
     * Verifica si la receta es del tipo desayuno y si su tiempo de preparación es adecuado (≤ 30 minutos).
     * Si no tiene tipo se mira que esté en el pasillo adecuado.
     **/
    fun filtrarParaDesayuno(recetas: List<Receta>): List<Receta> {
        val dishTypesValidos = listOf("breakfast", "morning meal", "brunch")
        val aislesValidos = listOf(
            "Bakery-Bread", "Bread", "Cereal",
            "Milk, Eggs, Other Dairy", "Cheese",
            "Nut butters, Jams, and Honey", "Tea and Coffee"
        )

        return recetas.filter { receta ->
            val esTipoDesayuno = if (receta.dishTypes.isNotEmpty()) {
                receta.dishTypes.any { it.lowercase() in dishTypesValidos }
                // Si no tiene tipos de plato, verifica si el ingrediente pertenece a un pasillo válido para el desayuno.
            } else {
                receta.ingredients.any { it.aisle in aislesValidos }
            }

            val tiempoValido = receta.time <= 30

            esTipoDesayuno && tiempoValido
        }
    }

    fun filtrarParaAlmuerzo(recetas: List<Receta>): List<Receta> {
        val dishTypesValidos = listOf("lunch", "main course", "main dish")
        val aislesValidos = listOf(
            "Meat", "Pasta and Rice", "Ethnic Foods", "Produce",
            "Milk, Eggs, Other Dairy"
        )
        return recetas.filter { receta ->
            if (receta.dishTypes.isNotEmpty()) {
                receta.dishTypes.any { it.lowercase() in dishTypesValidos }
            } else {
                // Si no tiene dishTypes, se filtra según el pasillo.
                receta.ingredients.any { it.aisle in aislesValidos }
            }
        }
    }

    fun filtrarParaCena(recetas: List<Receta>): List<Receta> {
        val dishTypesValidos = listOf("dinner")
        val aislesValidos = listOf(
            "Meat", "Ethnic Foods", "Seafood", "Pasta and Rice", "Produce", "Frozen"
        )
        return recetas.filter { receta ->
            if (receta.dishTypes.isNotEmpty()) {
                receta.dishTypes.any { it.lowercase() in dishTypesValidos }
            } else {
                // Cuando dishTypes esté vacío, usamos el aisle.
                receta.ingredients.any { it.aisle in aislesValidos }
            }
        }
    }

    // Mapa para contar cuántas veces se ha usado cada aisle (se guarda en minúscula para uniformidad)
    val usoAisle = mutableMapOf<String, Int>()

    // Definir límites para algunos aisles, "meat" y "pasta and rice" para evitar su uso excesivo en el mismo plan semanal.
    val limitesAisle = mapOf(
        "meat" to 3,
        "pasta and rice" to 3
        // agregar otros límites
    )

    /**
     * Verifica si se puede usar un pasillo para una receta en función del número de veces que ya se ha utilizado ese pasillo
     * durante la generación del plan semanal. Cada pasillo tiene un límite de uso específico, para evitar que se utilicen
     * ingredientes del mismo pasillo excesivamente durante la semana.
     *
     * El métdo obtiene el número actual de usos del pasillo en cuestión y compara si es menor que el límite definido
     * para ese pasillo. Si es menor, el pasillo puede ser utilizado; si no, no se podrá usar más ingredientes de ese pasillo.

     * @return True si el pasillo se puede usar (no se ha superado el límite), False en caso contrario.
     */
    fun puedeUsarAisle(aisle: String): Boolean {
        val key = aisle.lowercase() // Convertimos el pasillo a minúsculas para garantizar consistencia.
        val actual = usoAisle.getOrDefault(key, 0) // Obtenemos el número actual de usos del pasillo.
        val limite = limitesAisle.getOrDefault(key, Int.MAX_VALUE) // Obtenemos el límite del pasillo (por defecto es infinito).
        return actual < limite // Comprobamos si el número de usos es menor que el límite.
    }

    /**
     * Registra el uso de un pasillo al generar el plan semanal. Cada vez que se utiliza un ingrediente de un pasillo,
     * se incrementa el contador correspondiente en el mapa `usoAisle` para llevar un control de la cantidad de veces
     * que se ha utilizado cada pasillo durante la generación del plan.
     */
    fun registrarUsoAisle(aisle: String) {
        val key = aisle.lowercase() // Convertimos el pasillo a minúsculas para garantizar consistencia.
        usoAisle[key] = usoAisle.getOrDefault(key, 0) + 1 // Incrementamos el contador de uso del pasillo.
    }


    /**
     * Genera un plan semanal de comidas (desayuno, almuerzo y cena) para el usuario, utilizando las recetas proporcionadas.
     * El plan intenta asegurar la diversidad de los ingredientes y evitar la repetición de las recetas de la semana anterior.
     * Además, se asegura de que cada comida del día esté correctamente asignada, considerando restricciones como los tipos de platos (desayuno, almuerzo, cena) y los pasillos de ingredientes disponibles.
     *
     * El algoritmo realiza los siguientes pasos:
     * 1. Obtiene las recetas de la semana anterior para evitar repeticiones.
     * 2. Baraja las recetas disponibles para crear una selección aleatoria.
     * 3. Filtra las recetas para el desayuno, almuerzo y cena según los tipos de platos y pasillos válidos.
     * 4. Asigna una receta para cada comida del día, respetando restricciones de ingredientes y evitando el uso excesivo de ciertos pasillos (como carne o pasta).
     * 5. En caso de no encontrar una receta adecuada que cumpla todas las restricciones, relaja gradualmente las restricciones (p. ej., eliminando la condición de pasillo o de repetición semanal).
     * 6. Registra el uso de pasillos y recetas para garantizar que las recetas no se repitan innecesariamente.
     *
     * @param recetas Lista de recetas de firebase para generar el plan semanal.
     * @param userId ID del usuario a generar el plan.
     * @return Un objeto PlanSemanal que contiene las comidas asignadas para cada día de la semana.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generarPlanSemanal(recetas: List<Receta>, userId: String): PlanSemanal {
        val idsRecetasSemanaAnterior = obtenerRecetasSemanaAnterior(userId)
        val recetasBarajeadas = recetas.shuffled()

        val candidatosDesayuno = filtrarParaDesayuno(recetasBarajeadas)
        val candidatosAlmuerzo = filtrarParaAlmuerzo(recetasBarajeadas)
        val candidatosCena = filtrarParaCena(recetasBarajeadas)

        val diasSemana = DayOfWeek.entries
        val semanaMeals = mutableMapOf<DayOfWeek, DayMeal>()

        val recetasUsadas = mutableSetOf<String>()

        for (dia in diasSemana) {
            var desayunoSeleccionado: Receta?
            var almuerzoSeleccionado: Receta?
            var cenaSeleccionado: Receta?

            val aislesUsadosEnDia = mutableSetOf<String>()

            desayunoSeleccionado = seleccionarRecetaParaComida(
                candidatosDesayuno,
                aislesUsadosEnDia,
                recetasUsadas,
                idsRecetasSemanaAnterior
            )

            almuerzoSeleccionado = seleccionarRecetaParaComida(
                candidatosAlmuerzo,
                aislesUsadosEnDia,
                recetasUsadas,
                idsRecetasSemanaAnterior
            )

            cenaSeleccionado = seleccionarRecetaParaComida(
                candidatosCena,
                aislesUsadosEnDia,
                recetasUsadas,
                idsRecetasSemanaAnterior
            )

            if (desayunoSeleccionado == null || almuerzoSeleccionado == null || cenaSeleccionado == null) {
                Log.e("PlanSemanal", "No se pudo generar menú completo para el día $dia")
            }

            semanaMeals[dia] = DayMeal(
                breakfast = desayunoSeleccionado!!,
                lunch = almuerzoSeleccionado!!,
                dinner = cenaSeleccionado!!
            )
        }

        return PlanSemanal(semanaMeals)
    }


    // Función auxiliar para seleccionar una receta para una comida (desayuno, almuerzo o cena)
    fun seleccionarRecetaParaComida(
        candidatos: List<Receta>,
        aislesUsadosEnDia: MutableSet<String>,
        recetasUsadas: MutableSet<String>,
        idsRecetasSemanaAnterior: List<String>
    ): Receta? {
        // Primero intentamos seleccionar una receta que no haya sido usada y que respete las restricciones
        var seleccionada: Receta?

        // Intento 1: No repetir "aisle" y no repetir receta
        seleccionada = candidatos.firstOrNull { receta ->
            val aisle = receta.ingredients.firstOrNull()?.aisle ?: ""
            receta.id !in recetasUsadas &&
                    puedeUsarAisle(aisle) &&
                    aisle !in aislesUsadosEnDia &&
                    receta.id !in idsRecetasSemanaAnterior
        }

        // Intento 2: Relajamos la restricción de "aisle"
        if (seleccionada == null) {
            seleccionada = candidatos.firstOrNull { receta ->
                val aisle = receta.ingredients.firstOrNull()?.aisle ?: ""
                receta.id !in recetasUsadas &&
                        aisle !in aislesUsadosEnDia &&
                        receta.id !in idsRecetasSemanaAnterior
            }
        }

        // Intento 3: Relajamos la restricción de recetas de la semana anterior
        if (seleccionada == null) {
            seleccionada = candidatos.firstOrNull { receta ->
                receta.id !in recetasUsadas && receta.id !in idsRecetasSemanaAnterior
            }
        }

        // Intento 4: Sin restricciones
        if (seleccionada == null) {
            seleccionada = candidatos.firstOrNull { receta ->
                receta.id !in recetasUsadas
            }
        }

        // Intento 5: Si ninguna de las anteriores, seleccionamos cualquier receta
        if (seleccionada == null) {
            seleccionada = candidatos.firstOrNull()
        }

        // Si se ha seleccionado una receta, la agregamos a las listas de usadas
        seleccionada?.let {
            val aisle = it.ingredients.firstOrNull()?.aisle ?: ""
            registrarUsoAisle(aisle)
            aislesUsadosEnDia.add(aisle)
            recetasUsadas.add(it.id)
        }

        return seleccionada
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun obtenerRecetasSemanaAnterior(userId: String): List<String> {
        // Calculamos el identificador de la semana anterior
        val semanaAnteriorId = obtenerSemanaAnteriorId()
        // Lista para almacenar los IDs de las recetas
        val idsRecetasSemanaAnterior = mutableListOf<String>()

        try {
            // Hacemos la consulta a Firebase de forma asincrónica usando await() en lugar de withContext
            val documentSnapshot = db.collection("planSemanal")
                .document(userId) // Usamos el userId para identificar al usuario
                .collection("semanas") // Subcolección para las semanas del usuario
                .document(semanaAnteriorId) // El documento que contiene el plan de la semana anterior
                .get()
                .await() // Usamos await para realizar la consulta de manera asincrónica

            // Si el documento existe, extraemos los IDs de las recetas
            if (documentSnapshot.exists()) {
                val semanaData = documentSnapshot.data
                semanaData?.forEach { (_, value) ->
                    val desayunoId = (value as Map<*, *>)["breakfast"] as? String
                    val almuerzoId = value["lunch"] as? String
                    val cenaId = value["dinner"] as? String
                    desayunoId?.let { idsRecetasSemanaAnterior.add(it) }
                    almuerzoId?.let { idsRecetasSemanaAnterior.add(it) }
                    cenaId?.let { idsRecetasSemanaAnterior.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error al obtener el plan semanal anterior: ", e)
        }

        return idsRecetasSemanaAnterior
    }

    /**
     * Actualiza y guarda el plan semanal en Firestore.
     * Primero guarda el plan actual como el plan de la semana anterior y luego guarda el nuevo plan como plan de la semana actual.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun actualizarYGuardarPlanSemanal(userId: String, planSemanal: PlanSemanal) {
        // Semana actual
        val semanaActualId = obtenerSemanaActualId()
        // Semana anterior
        val semanaAnteriorId = obtenerSemanaAnteriorId()

        try {
            // Primero, obtenemos el plan actual de Firestore
            val document = db.collection("planSemanal")
                .document(userId)
                .collection("semanas")
                .document(semanaActualId)
                .get()
                .await()

            if (document.exists()) {
                // Extraemos el plan de la semana actual
                val planActualData = document.data ?: return

                // Guardamos este plan como el plan de la semana anterior
                db.collection("planSemanal")
                    .document(userId)
                    .collection("semanas")
                    .document(semanaAnteriorId)
                    .set(planActualData)
                    .await()  // Esperar la operación
                Log.d("Firestore", "Plan de la semana actual guardado como plan de la semana anterior")
            }

            // Ahora, guardamos el nuevo plan semanal bajo "semana_actual"
            val data = planSemanal.weekMeals.mapKeys { it.key.name }.mapValues { (_, dayMeal) ->
                mapOf(
                    "breakfast" to dayMeal.breakfast.id,
                    "lunch" to dayMeal.lunch.id,
                    "dinner" to dayMeal.dinner.id
                )
            }

            db.collection("planSemanal")
                .document(userId)
                .collection("semanas")
                .document(semanaActualId)
                .set(data)
                .await()  // Esperar la operación
            Log.d("Firestore", "Nuevo plan semanal guardado correctamente")

        } catch (e: Exception) {
            Log.e("Firestore", "Error al actualizar y guardar el plan semanal", e)
        }
    }

    //obtiene de firebase el plan semanal actual
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun obtenerPlanSemanal(userId: String): PlanSemanal? {
        val semanaActualId = obtenerSemanaActualId()
        Log.d("PlanRepository", "semanaActualId: $semanaActualId")

        return try {
            // Obtener el documento de la semana actual
            val document = db.collection("planSemanal")
                .document(userId)
                .collection("semanas")
                .document(semanaActualId)
                .get()
                .await()

            if (document.exists()) {
                Log.d("PlanRepository", "existe el documento: $semanaActualId")
                val data = document.data ?: return null

                val semanaMeals = mutableMapOf<DayOfWeek, DayMeal>()
                val daysProcessed = mutableSetOf<DayOfWeek>()

                // Iterar sobre los días de la semana
                DayOfWeek.entries.forEach { dia ->
                    val dayData = data[dia.name] as? Map<String, Any> ?: return@forEach

                    val breakfastId = dayData["breakfast"] as? String ?: return@forEach
                    val lunchId = dayData["lunch"] as? String ?: return@forEach
                    val dinnerId = dayData["dinner"] as? String ?: return@forEach

                    // Obtener las recetas por sus ID usando corrutinas
                    val breakfast = obtenerRecetaPorId(breakfastId)
                    val lunch = obtenerRecetaPorId(lunchId)
                    val dinner = obtenerRecetaPorId(dinnerId)

                    if (breakfast != null && lunch != null && dinner != null) {
                        val dayMeal = DayMeal(breakfast, lunch, dinner)
                        semanaMeals[dia] = dayMeal
                        daysProcessed.add(dia)

                        // Si todos los días han sido procesados, actualizamos el plan semanal
                        if (daysProcessed.size == DayOfWeek.entries.size) {
                            Log.d("PlanRepository", "Obteniendo plan actual...")
                            return PlanSemanal(semanaMeals)
                        }
                    } else {
                        Log.e("Firestore", "Error: alguna receta es nula para el día $dia")
                    }
                }

                // Si no se obtuvieron todas las recetas, se regresa null
                null
            } else {
                Log.e("Firestore", "No se encontró el plan semanal")
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error al obtener el plan semanal", e)
            null
        }
    }


    suspend fun obtenerRecetaPorId(id: String): Receta? {
        return try {
            val document = db.collection("bulkRecetas").document(id).get().await()
            if (document.exists()) {
                obtenerRecetaDesdeDocumentSnapshot(document)
            } else {
                Log.e("Firestore", "Receta no encontrada para el id: $id")
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error al obtener la receta con id: $id", e)
            null
        }
    }

    // Función suspendida para obtener las recetas desde Firebase
    suspend fun obtenerRecetasFirebase(): List<Receta> {
        return try {
            val documents = firestore.collection("bulkRecetas").get().await()
            documents.mapNotNull { document ->
                try {
                    obtenerRecetaDesdeDocumentSnapshot(document)
                } catch (e: Exception) {
                    Log.e("PlanRepository", "Error al mapear receta: ${e.message}")
                    null
                }
            }
        } catch (exception: Exception) {
            Log.e("PlanRepository", "Error al obtener recetas: ${exception.message}")
            emptyList()
        }
    }

    // Función que mapea un DocumentSnapshot a una Receta
    private fun obtenerRecetaDesdeDocumentSnapshot(document: DocumentSnapshot): Receta {
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
                    aisle = ing["aisle"] as? String ?: "",
                )
            } ?: emptyList(),
            steps = document.get("steps") as? List<String> ?: emptyList(),
            time = (document.get("time") as? Number)?.toInt() ?: 0,
            dishTypes = document.get("dishTypes") as? List<String> ?: emptyList(),
            userId = document.getString("user") ?: "",
            glutenFree = document.getBoolean("glutenFree") ?: false,
            vegan = document.getBoolean("vegan") ?: false,
            vegetarian = document.getBoolean("vegetarian") ?: false,
            date = (document.get("date") as? Long) ?: System.currentTimeMillis(),
            unusedIngredients = emptyList(),
            missingIngredientCount = 0,
            unusedIngredientCount = 0,
            esFavorita = null,  // Aquí puedes cambiar según sea necesario
        )
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_GEMINI_KEY
    )


    /**
     * Agrupa y filtra ingredientes usando Gemini para eliminar los ingredientes que realmente son instrucciones.
     * Devuelve una lista de ingredientes procesados (IngredienteCompra).
     * */
    suspend fun getGroupedIngredients(ingredientes: List<Ingrediente>): List<IngredienteCompra> {
        val ingredientesProcesados = groupByImagen(ingredientes)
        val nombresIngredientes = ingredientesProcesados.map { it.name }
        val prompt = generatePrompt(nombresIngredientes)

        return try {
            val response = generativeModel.generateContent(prompt)
            val groupedContent = response.text

            val nombresAEliminar = groupedContent
                ?.split(",")
                ?.map { it.trim().lowercase() }

            val ingredientesFiltrados = ingredientesProcesados.filter { ingrediente ->
                !nombresAEliminar!!.contains(ingrediente.name.lowercase())
            }

            Log.d("PlanViewModel", "Filtrados: ${ingredientesFiltrados.size}")
            ingredientesFiltrados
        } catch (e: Exception) {
            Log.e("PlanViewModel", "Error al usar Gemini: ${e.message}", e)
            emptyList()
        }
    }


    /**
     * Devuelve una lista con los nombres de ingredientes que no son realmente ingredientes,
     * Formato lista: name1,name2...
     */
    fun generatePrompt(ingredientes: List<String>): String {
        val ingredientesText = ingredientes.joinToString(", ") { it }
        Log.d("PlanViewModel", "ingredientes que paso al modelo: $ingredientesText")

        return """
           Tengo una lista de ingredientes y necesito simplificarla para que solamente haya ingredientes. Ponme el nombre de ingredientes que contengan verbos en el nombre, ya que hay alguna instrucción que se ha colado.
            Aquí está la lista de ingredientes que debo simplificar: $ingredientesText
        
            Asegúrate de seguir estrictamente este formato: no uses estilos y separa los nombres por comas sin espacios.
        """.trimIndent()
    }


    /**
     * Filtramos los ingredientes que tienen as required para que no salga eso en el nombre
     *
     * Agrupo los ingredientes con la misma imagen, si no tienen, agrupamos por nombre
     * y agrego sus cantidades-medidas para evitar repetidos (pe: egg y eggs)
     *
     * Ordenamos alfabéticamente por nombre
     */
    fun groupByImagen(ingredientes: List<Ingrediente>): List<IngredienteCompra> {
        val agrupados = mutableMapOf<String, IngredienteCompra>()

        ingredientes.forEach { ingrediente ->
            // Verificamos si el nombre contiene "as required"
            var nombre = ingrediente.name.lowercase()

            // Si contiene "as required", tomamos lo que está antes de esa frase
            if (nombre.contains("as required", ignoreCase = true)) {
                nombre = nombre.substringBefore("as required").trim() // Obtenemos el nombre antes de "as required"
            }

            // Obtenemos la imagen del ingrediente
            val imagen = ingrediente.image.ifEmpty { null }

            // Si tiene imagen, agrupamos por imagen, si no, agrupamos por nombre
            val claveAgrupacion = imagen ?: nombre

            // Agrupar los ingredientes por imagen o nombre
            if (agrupados.containsKey(claveAgrupacion)) {
                // Si ya existe la clave de agrupación, combinamos las medidas sin modificar la lista original
                agrupados[claveAgrupacion]?.medidas = agrupados[claveAgrupacion]?.medidas?.plus(ingrediente.amount to ingrediente.unit)
                    ?: listOf(ingrediente.amount to ingrediente.unit)
            } else {
                // Si no existe, creamos una nueva entrada en el mapa
                agrupados[claveAgrupacion] = IngredienteCompra(
                    name = nombre,
                    aisle = ingrediente.aisle.ifEmpty { "" },
                    image = imagen ?: "",  // Si no tiene imagen, dejamos vacío
                    medidas = listOf(ingrediente.amount to ingrediente.unit), // Usamos una lista inmutable
                    estaComprado = false
                )
            }
        }

        // Ordenamos alfabéticamente
        return agrupados.values.sortedBy { it.name.lowercase() }
    }






}