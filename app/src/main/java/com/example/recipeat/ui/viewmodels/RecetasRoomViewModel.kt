package com.example.recipeat.ui.viewmodels


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.repository.RecetaRepository
import kotlinx.coroutines.launch

class RoomViewModel(private val recetaRepository: RecetaRepository) : ViewModel() {

    private val _favoriteRecipesRoom = MutableLiveData<List<Receta>>()
    val favoriteRecipesRoom: LiveData<List<Receta>> get() = _favoriteRecipesRoom

    private var _recipeRoom = MutableLiveData<Receta>()
    val recipeRoom: LiveData<Receta> get() = _recipeRoom


    private val _userRecipesRoom = MutableLiveData<List<Receta>>()
    val userRecipesRoom: LiveData<List<Receta>> get() = _userRecipesRoom


//    // Función para convertir Receta a RecetaRoom dentro del ViewModel
//    fun toRecetaRoom(receta: Receta, esFavorita: Boolean = false): RecetaRoom {
//        return RecetaRoom(
//            id = receta.id,
//            title = receta.title,
//            image = receta.image ?: "",
//            servings = receta.servings,
//            ingredients = receta.ingredients,
//            steps = receta.steps,
//            time = receta.time,
//            dishTypes = receta.dishTypes,
//            userId = receta.userId,
//            usedIngredientCount = receta.usedIngredientCount,
//            glutenFree = receta.glutenFree,
//            vegan = receta.vegan,
//            vegetarian = receta.vegetarian,
//            date = receta.date,
//            esFavorita = esFavorita
//        )
//    }


    // Obtener todas las recetas
    fun getAllRecetas() {
        viewModelScope.launch {
            val recetas = recetaRepository.getAllRecetas()
            // Actualizamos el LiveData con los datos obtenidos
            _favoriteRecipesRoom.postValue(recetas)
        }
    }

    // Obtener todas las recetas
    fun deleteAllRecetas() {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Iniciando la eliminación de todas las recetas")

                recetaRepository.deleteAllRecetas()

                Log.d("RoomViewModel", "Todas las recetas han sido eliminadas exitosamente")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al eliminar todas las recetas: ${e.message}")
            }
        }
    }

    // Obtener todas las recetas
    fun getRoomRecetasUser(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("RecetasViewModel", "Obteniendo recetas para el usario $userId...")

                val recetasUser = recetaRepository.getRecetasUser(userId)
                _userRecipesRoom.value = recetasUser

                Log.d("RoomViewModel", "Todas las recetas han sido obtenidas exitosamente")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al obtener las recetas para el user $userId: ${e.message}")
            }
        }
    }



    // Insertar receta
    fun insertReceta(receta: Receta) {
        viewModelScope.launch {
            try {
                // Intentar insertar la receta
                recetaRepository.insertReceta(receta)
                Log.d("RoomViewModel", "Receta insertada correctamente: ${receta.title}")
            } catch (e: Exception) {
                // En caso de error, imprimir el error
                Log.e("RoomViewModel", "Error al insertar receta: ${e.message}", e)
            }
        }
    }


    // Eliminar receta
    fun deleteReceta(receta: Receta) {
        viewModelScope.launch {
            recetaRepository.deleteReceta(receta)
        }
    }

    // Obtener recetas favoritas
    fun getRecetasFavoritas() {
        viewModelScope.launch {
            val recetasFavoritas = recetaRepository.getRecetasFavoritas()
            _favoriteRecipesRoom.postValue(recetasFavoritas)
        }
    }

    // Obtener receta por ID
    fun getRecetaById(recetaId: String) {
        viewModelScope.launch {
            val receta = recetaRepository.getRecetaById(recetaId)
            _recipeRoom.value = receta
        }
    }

    // Obtener receta por ID
    fun deleteRecetaById(recetaId: String) {
        viewModelScope.launch {
            try {
                recetaRepository.deleteRecetaById(recetaId)

                Log.d("RoomViewModel", "Receta $recetaId eliminada correctamente")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al eliminar la receta $recetaId: ${e.message}")
            }
        }
    }


    fun setEsFavoritaToZero(recetaId: String) {
        viewModelScope.launch {
            recetaRepository.setEsFavoritaToZero(recetaId)

        }
    }

}