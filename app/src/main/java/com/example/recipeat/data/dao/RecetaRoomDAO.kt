package com.example.recipeat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.recipeat.data.model.Receta

@Dao
interface RecetaRoomDao {

    // Insertar una receta
    @Insert
    suspend fun insertReceta(receta: Receta)

    @Insert(onConflict = OnConflictStrategy.IGNORE) // si una receta ya existe, se ignora y no genera error
    suspend fun insertRecetas(recetas: List<Receta>)

    // Eliminar receta por su ID
    @Query("DELETE FROM recetas WHERE id = :recetaId")
    suspend fun deleteRecetaById(recetaId: String)

    // Consultar todas las recetas
    @Query("SELECT * FROM recetas")
    suspend fun getAllRecetas(): List<Receta>

    // Consultar una receta por ID
    @Query("SELECT * FROM recetas WHERE id = :recetaId")
    suspend fun getRecetaById(recetaId: String): Receta? // nullable pq tiene q poder devolver null en caso de error

    // Borrar todas las recetas (si es necesario)
    @Query("DELETE FROM recetas")
    suspend fun deleteAllRecetas()

    @Query("SELECT * FROM recetas WHERE userId = :userId ORDER BY date DESC")
    suspend fun getRecetasUser(userId: String): List<Receta>

    @Update
    suspend fun updateReceta(receta: Receta)
}

