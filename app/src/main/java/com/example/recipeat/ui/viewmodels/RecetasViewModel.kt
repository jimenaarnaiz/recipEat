package com.example.recipeat.ui.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.ApiReceta
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.data.model.SugerenciaReceta
import com.example.recipeat.data.repository.RecetaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

class RecetasViewModel(private val recetaRepository: RecetaRepository) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()


    // para name search
    private val _recetasSugeridas = MutableStateFlow<List<SugerenciaReceta>>(emptyList())
    val recetasSugeridas: StateFlow<List<SugerenciaReceta>> = _recetasSugeridas
    // para resultados de busqueda
    private val _recetas = MutableLiveData<List<Receta>>(emptyList())
    val recetas: LiveData<List<Receta>> = _recetas

    private val _recetasHome = MutableLiveData<List<Receta>>(emptyList())
    val recetasHome: LiveData<List<Receta>> = _recetasHome

    // para my recipes (user)
    private val _recetasUser = MutableLiveData<List<Receta>>(emptyList())
    val recetasUser: LiveData<List<Receta>> = _recetasUser

    private val _recetaSeleccionada = MutableLiveData<Receta?>()
    val recetaSeleccionada: LiveData<Receta?> = _recetaSeleccionada

    // para paginacion
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

    //para resultados
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading



    fun generateRecipeId(): String {
        return recetaRepository.generateRecipeId()
    }


    // Función para obtener las recetas del usuario con paginación
    fun getRecetasUser(uid: String, limpiarLista: Boolean = true) {
        if (_isLoadingMore.value == true) return // Evita cargar más si ya se está cargando

        _isLoadingMore.value = true // Indica que está cargando

        // Usar el repositorio para obtener las recetas
        viewModelScope.launch {
            try {
                val nuevasRecetas = recetaRepository.getRecetasUser(uid, limpiarLista)

                // Si se debe limpiar la lista, se reemplaza, sino se agrega a la lista existente
                if (limpiarLista) {
                    _recetasUser.value = nuevasRecetas
                } else {
                    _recetasUser.value = _recetasUser.value.orEmpty() + nuevasRecetas
                }

                Log.d("RecetasViewModel", "Total recetas User: ${_recetasUser.value?.size}")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Recetas User: Error al obtener recetas: ${e.message}")
            } finally {
                _isLoadingMore.value = false // Indicar que terminó el proceso de carga
            }
        }
    }

    /**
     * Añade a Firebase una receta creada por el user.
     */
    fun addMyRecipe(uid: String, receta: Receta, onComplete: (Boolean, String?) -> Unit) {
        Log.d("RecetasViewModel", "recetaId generado: ${receta.id}")

        // Llamar al repositorio para guardar la receta
        recetaRepository.addMyRecipe(uid, receta, onComplete)
    }


    fun editMyRecipe(uid: String, recetaModificada: Receta, onComplete: (Boolean, String?) -> Unit) {
        // Llamamos al repositorio para editar la receta
        recetaRepository.editMyRecipe(uid, recetaModificada, onComplete)
    }


    fun mapApiRecetaToReceta(apiReceta: ApiReceta, uid: String, analyzedInstructions: List<Map<String, Any>>): Receta {
        return recetaRepository.mapApiRecetaToReceta(apiReceta, uid, analyzedInstructions)
    }


    /**
     * Obtener recetas para la pantalla principal (Home)
     * @param limpiarLista Boolean para saber si se debe limpiar la lista antes de cargar las nuevas recetas
     */
    fun obtenerRecetasHome(limpiarLista: Boolean = true) {
        if (_isLoadingMore.value == true) return // Salir si ya se está cargando

        _isLoadingMore.value = true // Indicar que está cargando

        viewModelScope.launch {
            try {
                // Llamada al repositorio
                val result = recetaRepository.obtenerRecetasHome(limpiarLista)

                // Si la consulta es exitosa, se actualiza la lista de recetas
                if (result.isSuccess) {
                    val nuevasRecetas = result.getOrNull() ?: emptyList()

                    if (limpiarLista) {
                        _recetasHome.value = nuevasRecetas
                    } else {
                        _recetasHome.value = _recetasHome.value.orEmpty() + nuevasRecetas
                    }
                    Log.d("RecetasViewModel", "Total recetas: ${_recetasHome.value?.size}")
                } else {
                    Log.e("RecetasViewModel", "Error al obtener recetas: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener recetas: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }


    /**
     * Obtener receta por ID
     * @param uid ID del usuario (si es una receta del usuario)
     * @param recetaId ID de la receta
     * @param deUser Booleano para saber si se busca en las recetas del usuario o en las recetas generales
     */
    fun obtenerRecetaPorId(uid: String, recetaId: String, deUser: Boolean) {
        Log.d("RecetasViewModel", "Obteniendo receta con ID: $recetaId para el usuario $uid")

        viewModelScope.launch {
            try {
                // Llamada al repositorio
                val result = recetaRepository.obtenerRecetaPorId(uid, recetaId, deUser)

                // Si la consulta es exitosa, se actualiza la receta seleccionada
                if (result.isSuccess) {
                    _recetaSeleccionada.value = result.getOrNull()
                    Log.d("RecetasViewModel", "Receta obtenida: ${_recetaSeleccionada.value}")
                } else {
                    Log.e("RecetasViewModel", "Error al obtener receta: ${result.exceptionOrNull()?.message}")
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
                recetaRepository.eliminarReceta(uid, recetaId)
                _recetasUser.value = _recetasUser.value?.filterNot { it.id == recetaId }
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al eliminar receta: ${e.message}")
            }
        }
    }


    /**
     * Añade una entrada al historial del usuario con los datos básicos de la receta.
     */
    fun añadirHistorial(uid: String, userReceta: String, recetaId: String, title: String, image: String) {
        viewModelScope.launch {
            try {
                recetaRepository.añadirHistorial(uid, userReceta, recetaId, title, image)
                Log.d("RecetasViewModel", "Receta $recetaId añadida al historial correctamente")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al añadir receta $recetaId al historial", e)
            }
        }
    }


    fun eliminarRecetaDelHistorial(uid: String, recetaId: String) {
        viewModelScope.launch {
            try {
                recetaRepository.eliminarRecetaDelHistorial(uid, recetaId)
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al eliminar receta del historial: ${e.message}")
            }
        }
    }

    /**
     * Devuelve las recetas (tanto las de Firebase como las creadas por el usuario) que contienen todos o algunos de los ingredientes a buscar
     * Por defecto salen las rcetas con más coincidencias de ingredientes buscados, 1º las de
     * firebase y luego las del user.
     **/
    fun buscarRecetasPorIngredientes(ingredientes: List<IngredienteSimple>, userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val recetasFinales = recetaRepository.buscarRecetasPorIngredientes(ingredientes, userId)
                _recetas.value = recetasFinales
                _recetasOriginales.value = recetasFinales
                Log.d("buscarRecetasPorIngredientes()", "$recetasFinales")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error buscando recetas: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }


    // Búsqueda estricta en tiempo real (requiere que el título contenga TODAS las palabras)
    fun obtenerSugerenciasPorNombre(name: String) {
        viewModelScope.launch {
            val sugerencias = recetaRepository.obtenerSugerenciasPorNombre(name)
            _recetasSugeridas.value = sugerencias
        }
    }


    /**
     * Se hace una doble consulta: primero a bulkRecetas, luego a my_recipes/userId/recipes.
     * Ambas listas se combinan (documents + documents) y se procesan con la misma lógica.
     * Si una receta tiene al menos una palabra coincidente, se añade a los resultados.
     *
     * Las recetas del user van al final
     **/
    fun obtenerRecetasPorNombre(prefix: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val recetas = recetaRepository.obtenerRecetasPorNombre(prefix, userId)
            _recetas.value = recetas
            _recetasOriginales.value = recetas
            _isLoading.value = false
        }
    }


    // Definir la función de filtro
    fun filtrarRecetas(
        tiempoFiltro: Int?,
        maxIngredientesFiltro: Int?,
        maxFaltantesFiltro: Int?,
        maxPasosFiltro: Int?,
        tipoPlatoFiltro: String?,
        tipoDietaFiltro: Set<String>? // Cambio: ahora es un Set<String>
    ) {
        val recetasFiltro = _recetasOriginales.value!!.filter { receta ->
            // Validación por tiempo, solo se aplica si tiempoFiltro no es null
            val tiempoValido = tiempoFiltro?.let { receta.time <= it } ?: true

            // Validación por cantidad de ingredientes, solo se aplica si maxIngredientesFiltro no es null
            val ingredientesValidos = maxIngredientesFiltro?.let { receta.ingredients.size <= it } ?: true

            // Validación por ingredientes faltantes, solo se aplica si maxFaltantesFiltro no es null
            val faltantesValidados = maxFaltantesFiltro?.let { receta.missingIngredientCount <= it } ?: true

            // Validación por número de pasos, solo se aplica si maxPasosFiltro no es null
            val pasosValidados = maxPasosFiltro?.let { receta.steps.size <= it } ?: true

            // Validación por tipo de plato, solo se aplica si tipoPlatoFiltro no es null
            val platoValido = tipoPlatoFiltro?.let { receta.dishTypes.contains(it) } ?: true

            // Validación de dietas: la receta debe cumplir con TODAS las dietas seleccionadas
            val dietaValida = if (tipoDietaFiltro?.isNotEmpty() == true) {
                tipoDietaFiltro.all { dieta ->
                    when (dieta) {
                        "Gluten-Free" -> receta.glutenFree
                        "Vegan" -> receta.vegan
                        "Vegetarian" -> receta.vegetarian
                        else -> true
                    }
                }
            } else {
                true // Si no hay dietas seleccionadas, no filtrar por dieta
            }

            // Aplicar todos los filtros
            tiempoValido && ingredientesValidos && faltantesValidados && pasosValidados && dietaValida && platoValido
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


    /**
     * Añade o elimina una receta de favoritos para el usuario.
     * Si la receta ya está en favoritos, la elimina. Si no, la añade con metadatos.
     */
    fun toggleFavorito(uid: String?, userReceta: String, recetaId: String, title: String, image: String) {
        if (uid == null) return
        viewModelScope.launch {
            val esFavoritoNuevo = recetaRepository.toggleFavorito(uid, userReceta, recetaId, title, image)
            _esFavorito.value = esFavoritoNuevo
        }
    }

    fun verificarSiEsFavorito(uid: String?, recetaId: String) {
        if (uid == null) return
        viewModelScope.launch {
            _esFavorito.value = recetaRepository.verificarSiEsFavorito(uid, recetaId)
        }
    }


    // Obtiene las recetas favoritas del usuario ordenadas por la fecha de añadido.
    fun obtenerRecetasFavoritas(uid: String) {
        viewModelScope.launch {
            try {
                val recetas = recetaRepository.obtenerRecetasFavoritas(uid)
                _recetasFavoritas.value = recetas
                Log.d("RecetasViewModel", "Recetas favs actualizadas: ${_recetasFavoritas.value}")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener los favoritos del usuario", e)
            }
        }
    }


    // Obtiene las recetas en el historial del usuario dentro de un rango de fechas.
    fun obtenerRecetasPorRangoDeFecha(uid: String, rango: Int) {
        viewModelScope.launch {
            try {
                val recetas = recetaRepository.obtenerRecetasPorRangoDeFecha(uid, rango)
                _recetasHistorial.value = recetas
                Log.d("RecetasViewModel", "Recetas obtenidas: ${_recetasHistorial.value}")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al obtener recetas", e)
            }
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


    /**
     * Llama al repositorio para verificar y guardar la imagen de un equipo.
     */
    fun checkAndSaveEquipmentImage(equipmentName: String) {
        viewModelScope.launch {
            try {
                recetaRepository.checkAndSaveEquipmentImage(equipmentName)
                Log.d("RecetasViewModel", "Imagen del equipo guardada si no existía.")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al verificar y guardar imagen del equipo", e)
            }
        }
    }

    /**
     * Llama al repositorio para actualizar los pasos con las imágenes de los equipos.
     */
    fun updateEquipmentSteps(steps: List<String>) {
        viewModelScope.launch {
            try {
                val stepsWithImages = recetaRepository.updateEquipmentSteps(steps)
                _equipmentSteps.value = stepsWithImages
                Log.d("RecetasViewModel", "Pasos con imágenes de equipos actualizados: $stepsWithImages")
            } catch (e: Exception) {
                Log.e("RecetasViewModel", "Error al actualizar los pasos con imágenes de equipos", e)
            }
        }
    }


    // Llamar la función para cada equipo
    fun saveAllEquipmentImages() {
        equipmentNames.forEach { equipmentName ->
            checkAndSaveEquipmentImage(equipmentName)
        }
    }



    //API
    val ingredients = "avocado,cayenne,cauliflower head,celery,carrots,celery stalk,cheddar,cherries," +
            "almond,cherry tomato,chickpea,chicken,chicken breast,chicken broth,chicken " +
            "sausage,chicken thigh,chili pepper,chocolate,baking powder,cilantro,cinnamon,cocoa powder," +
            "coconut,condensed milk,cooking oil,corn,corn oil,cornstarch,couscous,crab,cranberries," +
            "cream,cream cheese,bacon,cumin,soy sauce,vinegar,double cream,egg,egg white,egg yolk," +
            "eggplant,chocolate chips,evaporated milk,extra virgin olive oil,feta cheese,firm brown sugar,fish sauce," +
            "flour,parsley,ginger,garlic,garlic powder,gelatin,goat cheese,gorgonzola,greek yogurt,green bean,ground beef," +
            "ground cinnamon,ground ginger,ground pepper,ground pork,ham,honey,jalapeño,rice,kidney beans,leek,lime,macaroni," +
            "mascarpone,milk,mint,mushroom,mustard,navy beans,oats,oat,olive oil,onion,orange,lettuce," +
            "oregano,breadcrumbs,parmesan cheese,peaches,pear,peas,pepper,pie crust,pineapple,banana,pork tenderloin,potato," +
            "powdered milk,prawns,bread,quinoa,radish,raisins,raspberry jam,red wine,salad oil,salmon,salt,sausage,scallion," +
            "shrimp,soy sauce,spinach,onion,squash,sugar,sundried tomatoes,sweet potato,tomato,tomato paste,tomato sauce," +
            "tuna,vanilla,vanilla extract,vegetable broth,vegetable oil,vinegar,nuts,water,white wine,bell pepper,yogurt," +
            "lentils,corn,collard greens,zucchini,beef,apple,apples,hake,anchovies,cucumber,mayonnaise,ketchup," +
            "chard,pumpkin,lemon,cabbage,octopus,strawberries,squid,cod,trout,sea bream,sardines,white fish,smoked salmon," +
            "clams,pork,lamb,turkey,quail,ground meat,white beans,green beans,black beans"


    fun logRecetasCount() {
        viewModelScope.launch {
            try {

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










}



