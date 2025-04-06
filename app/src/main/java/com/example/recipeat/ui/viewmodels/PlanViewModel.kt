package com.example.recipeat.ui.viewmodels

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.DayMeal
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.PlanSemanal
import com.example.recipeat.data.model.Receta
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private val _planSemanal = MutableLiveData<PlanSemanal>()
    val planSemanal: LiveData<PlanSemanal> = _planSemanal

    private val sharedPreferences =
        application.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)


    /**
     * Calculamos el identificador de la semana actual en formato "día/mes"
     * El día es el lunes de la semana actual
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerSemanaActualId(): String {
        val fechaActual = LocalDate.now()
        val primerDiaSemanaActual = fechaActual.with(DayOfWeek.MONDAY)

        return  "${primerDiaSemanaActual.dayOfMonth}-${primerDiaSemanaActual.monthValue}"
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

    /**
     * Comprueba si es la primera vez que el usuario accede al plan
     */
    fun esPrimeraVez(): Boolean {
        val primeraVez = sharedPreferences.getBoolean("primera_vez", true)
        Log.d("PlanSemanal", "Valor de primera_vez: $primeraVez")
        return primeraVez
    }

    fun borrarPrimeraVez() {
        Log.d("PlanSemanal", "Borrando valor de primera_vez")
        sharedPreferences.edit().remove("primera_vez").apply()
    }

    // Función para marcar que ya no es la primera vez que entra en el plan
    private fun marcarNoEsPrimeraVez() {
        Log.d("PlanSemanal", "Marcando primera_vez a false")
        sharedPreferences.edit().putBoolean("primera_vez", false).apply()
    }


    // Función que ejecuta el Worker cada lunes que genera el plan semanal
    @RequiresApi(Build.VERSION_CODES.O)
    fun iniciarGeneracionPlanSemanal(userId: String) {
            //Lógica de verificación con es primera vez o es lunes
//            if (esPrimeraVez()) {
//                Log.d("PlanSemanal", "Primera vez que el usuario entra, generando el plan semanal.")
//                obtenerRecetasFirebase { recetas ->
//                    if (recetas.isNotEmpty()) {
//                        // Llamamos a la función suspendida dentro de la coroutine
//                        // Usamos launch para llamar la función suspendida correctamente
//                        viewModelScope.launch {
//                            val planSemanal = generarPlanSemanal(recetas, userId)
//                            actualizarYGuardarPlanSemanal(userId, planSemanal)
//                            marcarNoEsPrimeraVez()  // Marca que ya no es primera vez
//                        }
//                    } else {
//                        Log.e("PlanSemanal", "No se encontraron recetas para generar el plan")
//                    }
//                }
            /*} else*/ if (esLunes()) {
                Log.d("PlanSemanal", "Hoy es lunes, actualizando el plan semanal...")
                obtenerRecetasFirebase { recetas ->
                    if (recetas.isNotEmpty()) {
                        // Llamamos a la función suspendida dentro de la coroutine
                        viewModelScope.launch {
                            val planSemanal = generarPlanSemanal(recetas, userId)
                            actualizarYGuardarPlanSemanal(userId, planSemanal)
                        }
                    } else {
                       Log.e("PlanSemanal", "No se encontraron recetas para generar el plan")
                    }
                }
            } else {
                Log.d("PlanSemanal", "Hoy no es lunes. No se actualiza el plan semanal.")
            }
    }


    // si es la primera vez que el user entra (register), y no es lunes, se genera un plan incial
    @RequiresApi(Build.VERSION_CODES.O)
    fun iniciarGeneracionPlanSemanalIncial(userId: String) {
        if (!esLunes()) {
            Log.d("PlanSemanal", "Primera vez que el usuario entra, generando el plan semanal.")
            obtenerRecetasFirebase { recetas ->
                if (recetas.isNotEmpty()) {
                    // Llamamos a la función suspendida dentro de la coroutine
                    // Usamos launch para llamar la función suspendida correctamente
                    viewModelScope.launch {
                        val planSemanal = generarPlanSemanal(recetas, userId)
                        actualizarYGuardarPlanSemanal(userId, planSemanal)
                        //marcarNoEsPrimeraVez()  // Marca que ya no es primera vez
                    }
                } else {
                    Log.e("PlanSemanal", "No se encontraron recetas para generar el plan")
                }
            }
        } else {
            Log.d("PlanSemanal", "Es lunes. ")
        }
    }



    //RESTRICCIONES

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
                    // Si no tiene dishTypes, se filtra según algún ingrediente.
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

        // Definir límites para algunos aisles, "meat" y "pasta and rice"
        val limitesAisle = mapOf(
            "meat" to 3,
            "pasta and rice" to 3
            // agregar otros límites
        )

        fun puedeUsarAisle(aisle: String): Boolean {
            val key = aisle.lowercase()
            val actual = usoAisle.getOrDefault(key, 0)
            val limite = limitesAisle.getOrDefault(key, Int.MAX_VALUE)
            return actual < limite
        }

        fun registrarUsoAisle(aisle: String) {
            val key = aisle.lowercase()
            usoAisle[key] = usoAisle.getOrDefault(key, 0) + 1
        }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generarPlanSemanal(recetas: List<Receta>, userId: String): PlanSemanal {
        val idsRecetasSemanaAnterior = obtenerRecetasSemanaAnterior(userId = userId)
        val recetasBarajeadas = recetas.shuffled() // Las recetas se desordenan para evitar repetir las mismas.

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

            // Desayuno
            desayunoSeleccionado = candidatosDesayuno.firstOrNull { receta ->
                val aisle = receta.ingredients.firstOrNull()?.aisle ?: ""
                receta.id !in recetasUsadas &&
                        puedeUsarAisle(aisle) &&
                        aisle !in aislesUsadosEnDia &&
                        receta.id !in idsRecetasSemanaAnterior
            }

            // Si no se encuentra desayuno válido, se selecciona por tipo de plato
            if (desayunoSeleccionado == null) {
                desayunoSeleccionado = candidatosDesayuno.firstOrNull { receta ->
                    receta.id !in recetasUsadas && receta.id !in idsRecetasSemanaAnterior
                }
            }

            desayunoSeleccionado?.let {
                val aisle = it.ingredients.firstOrNull()?.aisle ?: ""
                registrarUsoAisle(aisle)
                aislesUsadosEnDia.add(aisle)
                recetasUsadas.add(it.id)
            }

            // Almuerzo
            almuerzoSeleccionado = candidatosAlmuerzo.firstOrNull { receta ->
                val aisle = receta.ingredients.firstOrNull()?.aisle ?: ""
                receta.id !in recetasUsadas &&
                        aisle !in aislesUsadosEnDia &&
                        receta.id !in idsRecetasSemanaAnterior
            }

            // Si no se encuentra almuerzo válido, se selecciona por tipo de plato
            if (almuerzoSeleccionado == null) {
                almuerzoSeleccionado = candidatosAlmuerzo.firstOrNull { receta ->
                    receta.id !in recetasUsadas && receta.id !in idsRecetasSemanaAnterior
                }
            }

            almuerzoSeleccionado?.let {
                val aisle = it.ingredients.firstOrNull()?.aisle ?: ""
                registrarUsoAisle(aisle)
                aislesUsadosEnDia.add(aisle)
                recetasUsadas.add(it.id)
            }

            // Cena
            cenaSeleccionado = candidatosCena.firstOrNull { receta ->
                val aisle = receta.ingredients.firstOrNull()?.aisle ?: ""
                receta.id !in recetasUsadas &&
                        aisle !in aislesUsadosEnDia &&
                        receta.id !in idsRecetasSemanaAnterior
            }

            // Si no se encuentra cena válida, se selecciona por tipo de plato y que no se use ni en la semana anteior ni actual
            if (cenaSeleccionado == null) {
                cenaSeleccionado = candidatosCena.firstOrNull { receta ->
                    receta.id !in recetasUsadas && receta.id !in idsRecetasSemanaAnterior
                }
            }

            cenaSeleccionado?.let {
                val aisle = it.ingredients.firstOrNull()?.aisle ?: ""
                registrarUsoAisle(aisle)
                aislesUsadosEnDia.add(aisle)
                recetasUsadas.add(it.id)
            }

            // Verifica que se ha seleccionado una receta para cada comida
            if (desayunoSeleccionado == null || almuerzoSeleccionado == null || cenaSeleccionado == null) {
                Log.e("PlanSemanal", "No se pudo generar menú completo para el día $dia")
                continue
            }

            semanaMeals[dia] = DayMeal(
                breakfast = desayunoSeleccionado,
                lunch = almuerzoSeleccionado,
                dinner = cenaSeleccionado
            )
        }

        val planSemanal = PlanSemanal(semanaMeals)
        //_planSemanal.value = planSemanal
        return planSemanal
    }



    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun obtenerRecetasSemanaAnterior(userId: String): List<String> {
        // Calculamos el identificador de la semana anterior
        val semanaAnteriorId = obtenerSemanaAnteriorId()
        // Lista para almacenar los IDs de las recetas
        val idsRecetasSemanaAnterior = mutableListOf<String>()

        // Hacemos la consulta a Firebase de forma asincrónica
        try {
            val documentSnapshot = withContext(Dispatchers.IO) {
                // Consulta de Firebase de manera asincrónica con la nueva estructura de Firestore
                db.collection("planSemanal")
                    .document(userId) // Usamos el userId para identificar al usuario
                    .collection("semanas") // Subcolección para las semanas del usuario
                    .document(semanaAnteriorId) // El documento que contiene el plan de la semana anterior
                    .get()
                    .await()  // Usamos `await` para esperar el resultado asincrónicamente
            }

            // Si el documento existe, extraemos los IDs de las recetas
            if (documentSnapshot.exists()) {
                val semanaData = documentSnapshot.data
                semanaData?.forEach { (_, value) ->
                    val desayunoId = (value as Map<*, *>)["breakfast"] as? String
                    val almuerzoId = (value as Map<*, *>)["lunch"] as? String
                    val cenaId = (value as Map<*, *>)["dinner"] as? String
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
     * Obtenemos el plan de la semana actual desde Firestore.
     * Guardamos ese plan como plan de la semana anterior.
     * Guardamos el nuevo plan como plan de la semana actual.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun actualizarYGuardarPlanSemanal(userId: String, planSemanal: PlanSemanal) {
        // Semana actual
        val semanaActualId = obtenerSemanaActualId()
        // Semana anterior
        val semanaAnteriorId = obtenerSemanaAnteriorId()

        // Primero, obtenemos el plan actual de Firestore
        db.collection("planSemanal")
            .document(userId) // Usamos el userId para identificar al usuario
            .collection("semanas") // Subcolección para almacenar los planes semanales
            .document(semanaActualId) // El documento que contiene el plan de la semana actual
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Extraemos el plan de la semana actual
                    val planActualData = document.data ?: return@addOnSuccessListener

                    // Guardamos este plan como el plan de la semana anterior
                    db.collection("planSemanal")
                        .document(userId) // Usamos el userId para identificar al usuario
                        .collection("semanas") // Subcolección para almacenar los planes semanales
                        .document(semanaAnteriorId) // Usamos el ID de la semana anterior
                        .set(planActualData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Plan de la semana actual guardado como plan de la semana anterior")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error al guardar el plan de la semana anterior", e)
                        }
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
                    .document(userId) // Usamos el userId para identificar al usuario
                    .collection("semanas") // Subcolección para almacenar los planes semanales
                    .document(semanaActualId) // Usamos el ID de la semana actual
                    .set(data)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Nuevo plan semanal guardado correctamente")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error al guardar el plan semanal", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener el plan semanal actual", e)
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerPlanSemanal(userId: String) {
        //semana actual
        val semanaActualId = obtenerSemanaActualId()
        Log.d("PlanViewModel", "semanaActualId: $semanaActualId")

        db.collection("planSemanal")
            .document(userId) // Usamos el userId para identificar al usuario
            .collection("semanas") // Subcolección para almacenar los planes semanales
            .document(semanaActualId) // El documento que contiene el plan de la semana actual
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener los datos del plan semanal
                    val data = document.data ?: return@addOnSuccessListener

                    // Crear una lista de los días de la semana con sus comidas correspondientes
                    val semanaMeals = mutableMapOf<DayOfWeek, DayMeal>()
                    val daysProcessed = mutableSetOf<DayOfWeek>() // Para saber cuándo hemos procesado todos los días

                    // Iterar sobre los días de la semana
                    DayOfWeek.entries.forEach { dia ->
                        val dayData = data[dia.name] as? Map<String, Any> ?: return@forEach

                        val breakfastId = dayData["breakfast"] as? String ?: return@forEach
                        val lunchId = dayData["lunch"] as? String ?: return@forEach
                        val dinnerId = dayData["dinner"] as? String ?: return@forEach

                        // Obtener las recetas por sus ID usando `obtenerRecetaPorId`
                        obtenerRecetaPorId(breakfastId) { breakfast ->
                            obtenerRecetaPorId(lunchId) { lunch ->
                                obtenerRecetaPorId(dinnerId) { dinner ->
                                    // Comprobar si alguna receta es nula
                                    if (breakfast != null && lunch != null && dinner != null) {
                                        // Crear un objeto `DayMeal` para el día con sus tres comidas
                                        val dayMeal = DayMeal(breakfast, lunch, dinner)

                                        // Asignar el plan de comida para ese día
                                        semanaMeals[dia] = dayMeal
                                        daysProcessed.add(dia)

                                        // Si todos los días han sido procesados, actualizamos el plan semanal
                                        if (daysProcessed.size == DayOfWeek.entries.size) {
                                            Log.d("PlanViewModel", "Obteniendo plan actual...")
                                            _planSemanal.value = PlanSemanal(semanaMeals)
                                        }
                                    } else {
                                        // Manejar el caso en que alguna receta es nula
                                        Log.e("Firestore", "Error: alguna receta es nula para el día $dia")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.e("Firestore", "No se encontró el plan semanal")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener el plan semanal", e)
            }
    }


    fun obtenerRecetaPorId(id: String, callback: (Receta?) -> Unit) {
        db.collection("bulkRecetas").document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                   val receta = obtenerRecetaDesdeDocumentSnapshot(document)
                    // Llamar al callback pasando la receta obtenida
                    callback(receta)
                } else {
                    Log.e("Firestore", "Receta no encontrada para el id: $id")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener la receta con id: $id", e)
                callback(null)
            }
    }


    fun obtenerRecetasFirebase(onRecetasObtenidas: (List<Receta>) -> Unit) {
        db.collection("bulkRecetas")
            .get()
            .addOnSuccessListener { documents ->
                val recetas = documents.mapNotNull { document ->
                    try {
                        obtenerRecetaDesdeDocumentSnapshot(document)
                    } catch (e: Exception) {
                        Log.e("PlanViewModel", "Error al mapear receta: ${e.message}")
                        null
                    }
                }
                onRecetasObtenidas(recetas)
            }
            .addOnFailureListener { exception ->
                Log.e("PlanViewModel", "Error al obtener recetas: ${exception.message}")
                onRecetasObtenidas(emptyList())
            }
    }


    fun obtenerRecetaDesdeDocumentSnapshot(document: DocumentSnapshot): Receta {
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
            esFavorita = null,  // Aquí puedes cambiar según sea necesario, ya que no está claro de dónde viene este valor.
        )
    }




}

