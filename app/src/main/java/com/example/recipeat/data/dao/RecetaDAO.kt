package com.example.recipeat.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.recipeat.data.model.RecetaRoom

@Dao
interface RecetaDao {

    // Insertar una receta
    @Insert
    suspend fun insertReceta(receta: RecetaRoom)

    // Eliminar una receta por su ID
    @Delete
    suspend fun deleteReceta(receta: RecetaRoom)

    // Eliminar receta por su ID, de manera más explícita
    @Query("DELETE FROM recetas WHERE id = :recetaId")
    suspend fun deleteRecetaById(recetaId: String)

    // Consultar todas las recetas (si es necesario)
    @Query("SELECT * FROM recetas")
    suspend fun getAllRecetas(): List<RecetaRoom>

    // Consultar una receta por ID (si es necesario)
    @Query("SELECT * FROM recetas WHERE id = :recetaId")
    suspend fun getRecetaById(recetaId: String): RecetaRoom //antes era anullable


    @Query("SELECT * FROM recetas WHERE esFavorita = 1")
    suspend fun getRecetasFavoritas(): List<RecetaRoom>
}
