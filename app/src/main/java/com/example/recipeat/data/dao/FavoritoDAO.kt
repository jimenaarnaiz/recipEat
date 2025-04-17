package com.example.recipeat.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.recipeat.data.model.Favorito

@Dao
interface FavoritoDao {
    @Insert
    suspend fun agregarFavorito(favorito: Favorito)

    @Query("DELETE FROM favoritos WHERE userId = :userId AND recetaId = :recetaId")
    suspend fun eliminarFavorito(userId: String, recetaId: String)

    @Query("SELECT * FROM favoritos WHERE userId = :userId ORDER BY date DESC")
    suspend fun obtenerFavoritosPorUsuario(userId: String): List<Favorito>

    @Query("SELECT * FROM favoritos WHERE userId = :userId AND recetaId = :recetaId")
    suspend fun esFavorita(userId: String, recetaId: String): Favorito?


    @Query("DELETE FROM favoritos WHERE userId = :userId")
    suspend fun eliminarTodosLosFavoritos(userId: String) // Elimina todos los favoritos de un usuario

}
