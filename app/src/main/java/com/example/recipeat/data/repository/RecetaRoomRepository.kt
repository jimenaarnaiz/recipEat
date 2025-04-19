package com.example.recipeat.data.repository

import android.util.Log
import com.example.recipeat.data.dao.FavoritoDao
import com.example.recipeat.data.dao.RecetaRoomDao
import com.example.recipeat.data.model.Favorito
import com.example.recipeat.data.model.Receta
import java.sql.Timestamp

class RecetaRoomRepository(private val recetaRoomDao: RecetaRoomDao,  private val favoritoDao: FavoritoDao) {

    // Obtener todas las recetas
    suspend fun getAllRecetas(): List<Receta> {
        return recetaRoomDao.getAllRecetas()
    }

    // Insertar receta
    suspend fun insertReceta(receta: Receta) {
        recetaRoomDao.insertReceta(receta)
    }

    suspend fun insertRecetas(recetas: List<Receta>){
        recetaRoomDao.insertRecetas(recetas)
    }


    // Eliminar receta por ID
    suspend fun deleteRecetaById(recetaId: String) {
        recetaRoomDao.deleteRecetaById(recetaId)
    }
    // Obtener receta por ID
    suspend fun getRecetaById(recetaId: String): Receta {
        return recetaRoomDao.getRecetaById(recetaId)
    }

    suspend fun deleteAllRecetas(){
        return recetaRoomDao.deleteAllRecetas()
    }

    suspend fun getRecetasUser(userId: String): List<Receta>{
        return recetaRoomDao.getRecetasUser(userId = userId)
    }

    suspend fun getRecetasHome(): List<Receta> {
        return recetaRoomDao.getRecetasHome()
    }

    suspend fun updateReceta(receta: Receta) {
        return recetaRoomDao.updateReceta(receta)
    }


    //FAVS BIEN
    // Agregar una receta a los favoritos
    suspend fun agregarFavorito(userId: String, recetaId: String) {
        // Verificar si el favorito ya existe
        val existeFavorito = favoritoDao.esFavorita(userId, recetaId) // Asumimos que este métdo existe en tu DAO para verificar si ya existe el favorito
        if (existeFavorito != null) {
            // Si el favorito ya existe, no hacemos nada
            Log.e("RecetaRoomRepository", "El favorito ya existe")
            return
        }

        // Crear el objeto Favorito
        val favorito = Favorito(userId = userId, recetaId = recetaId, date = Timestamp(System.currentTimeMillis()))

        // Intentar agregar el favorito
        favoritoDao.agregarFavorito(favorito)
    }



    // Eliminar una receta de los favoritos
    suspend fun eliminarFavorito(userId: String, recetaId: String) {
        Log.d("RoomRepository", "Receta a eliminar de favs: uid: $userId recetaId: $recetaId")
        favoritoDao.eliminarFavorito(userId, recetaId)
    }

    // Obtener las recetas favoritas de un usuario
    suspend fun getRecetasFavoritas(userId: String): List<Receta> {
        val favoritos = favoritoDao.obtenerFavoritosPorUsuario(userId)
        return favoritos.map { favorito ->
            recetaRoomDao.getRecetaById(favorito.recetaId)
        }
    }

    // Verificar si una receta está marcada como favorita
    suspend fun esFavorita(userId: String, recetaId: String): Boolean {
        return favoritoDao.esFavorita(userId, recetaId) != null
    }


    // Métdo para eliminar todos los favoritos de un usuario
    suspend fun eliminarTodosLosFavoritos(userId: String) {
        favoritoDao.eliminarTodosLosFavoritos(userId)
    }

}

