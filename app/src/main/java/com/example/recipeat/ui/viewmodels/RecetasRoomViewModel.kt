package com.example.recipeat.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.Reciente
import com.example.recipeat.data.repository.RecetaRoomRepository
import kotlinx.coroutines.launch
import java.sql.SQLException
import java.sql.Timestamp

class RoomViewModel(private val recetaRoomRepository: RecetaRoomRepository) : ViewModel() {

    private val _favoriteRecipesRoom = MutableLiveData<List<Receta>>()
    val favoriteRecipesRoom: LiveData<List<Receta>> get() = _favoriteRecipesRoom

    private var _recipeRoom = MutableLiveData<Receta?>()
    val recipeRoom: LiveData<Receta?> get() = _recipeRoom

    private val _userRecipesRoom = MutableLiveData<List<Receta>>()
    val userRecipesRoom: LiveData<List<Receta>> get() = _userRecipesRoom

    private val _homeRecipesRoom = MutableLiveData<List<Receta>>()
    val homeRecipesRoom: LiveData<List<Receta>> get() = _homeRecipesRoom

    private val _recientes = MutableLiveData<List<Receta>>()
    val recientes: LiveData<List<Receta>> get() = _recientes

    // Obtener todas las recetas
    fun getAllRecetas() {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Iniciando obtención de todas las recetas.")
                val recetas = recetaRoomRepository.getAllRecetas()
                _favoriteRecipesRoom.postValue(recetas)
                Log.d("RoomViewModel", "Recetas obtenidas exitosamente.")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al obtener las recetas: ${e.message}", e)
            }
        }
    }

    // Eliminar todas las recetas
    fun deleteAllRecetas() {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Iniciando la eliminación de todas las recetas")
                recetaRoomRepository.deleteAllRecetas()
                Log.d("RoomViewModel", "Todas las recetas han sido eliminadas exitosamente")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al eliminar todas las recetas: ${e.message}", e)
            }
        }
    }

    fun eliminarTodosLosFavoritos(userId: String) {
        viewModelScope.launch {
            try {
                recetaRoomRepository.eliminarTodosLosFavoritos(userId)
                Log.d("RoomViewModel", "Todos los favoritos del usuario $userId han sido eliminados.")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al eliminar todos los favoritos: ${e.message}", e)
            }
        }
    }


    // Obtener recetas de un usuario
    fun getRoomRecetasUser(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Obteniendo recetas para el usuario $userId...")
                val recetasUser = recetaRoomRepository.getRecetasUser(userId)
                _userRecipesRoom.value = recetasUser
                Log.d("RoomViewModel", "Recetas obtenidas para el usuario $userId.")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al obtener las recetas para el usuario $userId: ${e.message}", e)
            }
        }
    }

    // Insertar receta
    fun insertReceta(receta: Receta) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Iniciando inserción de receta: ${receta.title}")
                recetaRoomRepository.insertReceta(receta)
                Log.d("RoomViewModel", "Receta insertada correctamente: ${receta.title}")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al insertar receta: ${e.message}", e)
            }
        }
    }

    // Guardar las primeras 15 recetas si no están guardadas
    fun guardarPrimeras15RecetasSiNoEstan(context: Context, recetas: List<Receta>, userId: String) {
        val prefs = context.getSharedPreferences("prefs_recetas", Context.MODE_PRIVATE)
        val keyGuardado = "recetas_home_guardadas_$userId"
        val keyIds = "ids_recetas_home_$userId"
        val yaGuardadas = prefs.getBoolean(keyGuardado, false)

        Log.d("RoomViewModel", "Recetas size: ${recetas.size} | yaGuardadas: $yaGuardadas para userId: $userId")

        if (!yaGuardadas && recetas.size >= 15) {
            viewModelScope.launch {
                try {
                    val primeras15 = recetas.take(15)

                    Log.d("RoomViewModel", "Guardando las primeras 15 recetas.")
                    recetaRoomRepository.insertRecetas(primeras15)

                    // Guardar la bandera de que ya se guardaron
                    prefs.edit().putBoolean(keyGuardado, true).apply()

                    // Guardar los IDs de esas recetas como Set<String>
                    val ids = primeras15.map { it.id }.toSet()
                    prefs.edit().putStringSet(keyIds, ids).apply()

                    Log.d("RoomViewModel", "Primeras 15 recetas guardadas en Room y IDs en SharedPreferences para el usuario $userId.")
                } catch (e: Exception) {
                    Log.e("RoomViewModel", "Error al guardar recetas en Room: ${e.message}", e)
                }
            }
        } else {
            Log.d("RoomViewModel", "Ya guardadas o no hay suficientes recetas para $userId.")
        }
    }


    fun esRecetaDelHome(context: Context, userId: String, recetaId: String): Boolean {
        val prefs = context.getSharedPreferences("prefs_recetas", Context.MODE_PRIVATE)
        val keyIds = "ids_recetas_home_$userId"
        val ids = prefs.getStringSet(keyIds, emptySet()) ?: emptySet()
        return ids.contains(recetaId)
    }



    /// NEW
    // Obtener recetas favoritas
    fun getRecetasFavoritas(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Obteniendo recetas favoritas.")
                val recetasFavoritas = recetaRoomRepository.getRecetasFavoritas(userId)
                _favoriteRecipesRoom.postValue(recetasFavoritas)
                Log.d("RoomViewModel", "Recetas favoritas obtenidas exitosamente.")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al obtener recetas favoritas: ${e.message}", e)
            }
        }
    }

    // Agregar receta a favoritos
    fun agregarFavorito(userId: String, recetaId: String) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Agregando receta a favoritos.")
                recetaRoomRepository.agregarFavorito(userId, recetaId)
                Log.d("RoomViewModel", "Receta agregada a favoritos exitosamente.")
            } catch (e: SQLException) {
                Log.e("RoomViewModel", "Error de base de datos al intentar agregar receta a favoritos: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al agregar receta a favoritos: ${e.message}", e)
            }
        }
    }


    // Eliminar receta de favoritos
    fun eliminarFavorito(userId: String, recetaId: String) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Eliminando receta de favoritos.")
                recetaRoomRepository.eliminarFavorito(userId, recetaId)
                Log.d("RoomViewModel", "Receta eliminada de favoritos exitosamente.")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al eliminar receta de favoritos: ${e.message}", e)
            }
        }
    }

    ///NEW

    // Obtener recetas para el home
    fun getRecetasHome(context: Context, userId: String) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("prefs_recetas", Context.MODE_PRIVATE)
                val keyIds = "ids_recetas_home_$userId"
                val idsGuardados = prefs.getStringSet(keyIds, emptySet())?.toList() ?: emptyList()

                if (idsGuardados.isNotEmpty()) {
                    Log.d("RoomViewModel", "Buscando recetas con IDs: $idsGuardados")
                    val recetasHome = recetaRoomRepository.getRecetasHome(userId, idsGuardados)
                    _homeRecipesRoom.postValue(recetasHome)
                } else {
                    Log.d("RoomViewModel", "No hay IDs guardados para el usuario $userId.")
                    _homeRecipesRoom.postValue(emptyList())
                }
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al obtener recetas para el home: ${e.message}", e)
            }
        }
    }


    // Obtener receta por ID
    fun getRecetaById(recetaId: String) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Obteniendo receta por ID: $recetaId")

                // Obtener receta desde el repositorio
                val receta = recetaRoomRepository.getRecetaById(recetaId)

                // Verificar si la receta existe
                if (receta != null) {
                    _recipeRoom.value = receta
                    Log.d("RoomViewModel", "Receta con ID $recetaId obtenida exitosamente.")
                } else {
                    // Si la receta no existe, manejar el caso adecuadamente
                    Log.e("RoomViewModel", "Receta con ID $recetaId no encontrada.")
                    _recipeRoom.value = null // O puedes usar un valor predeterminado si es necesario
                }

            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al obtener receta por ID $recetaId: ${e.message}", e)
                _recipeRoom.value = null // O un valor de error o un estado adecuado
            }
        }
    }


    // Eliminar receta por ID
    fun deleteRecetaById(userId: String, recetaId: String) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Iniciando eliminación de receta por ID: $recetaId")
                recetaRoomRepository.eliminarFavorito(userId, recetaId)
                recetaRoomRepository.deleteRecetaById(recetaId)
                Log.d("RoomViewModel", "Receta con ID $recetaId eliminada correctamente.")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al eliminar receta con ID $recetaId: ${e.message}", e)
            }
        }
    }

    // Función para actualizar la receta
    fun updateReceta(receta: Receta) {
        viewModelScope.launch {
            try {
                Log.d("RoomViewModel", "Iniciando actualización de receta: ${receta.title}")
                // Llamada al repositorio para actualizar la receta en Room
                recetaRoomRepository.updateReceta(receta)
                Log.d("RoomViewModel", "Receta actualizada correctamente en Room: ${receta.title}")
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Error al actualizar la receta", e)
            }
        }
    }


    // recientes

    // Agregar receta al historial
    fun agregarReciente(receta: Receta, userId: String) {
        viewModelScope.launch {
            // Verificar si la receta ya está en Room
            val recetaExistente = recetaRoomRepository.getRecetaById(receta.id)

            // Si no existe en Room, insertarla
            if (recetaExistente == null) {
                recetaRoomRepository.insertReceta(receta)
            }

            recetaRoomRepository.insertarReciente(receta, userId)
        }
    }


    // Obtener recetas recientes
    fun obtenerRecientes(userId: String) {
        viewModelScope.launch {
            val recetas = recetaRoomRepository.obtenerRecientes(userId)
            _recientes.postValue(recetas)
        }
    }

    // Eliminar todas las recetas recientes
    fun eliminarRecientes(userId: String) {
        viewModelScope.launch {
            recetaRoomRepository.eliminarRecientes(userId)
        }
    }

    // Eliminar una receta específica de las recientes
    fun eliminarRecientePorId(recetaId: String, userId: String) {
        viewModelScope.launch {
            recetaRoomRepository.eliminarRecientePorId(recetaId, userId)
        }
    }
}


