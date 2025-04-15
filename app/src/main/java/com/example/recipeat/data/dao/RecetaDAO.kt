package com.example.recipeat.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.recipeat.data.model.Receta

@Dao
interface RecetaDao {

    // Insertar una receta
    @Insert
    suspend fun insertReceta(receta: Receta)

    @Insert(onConflict = OnConflictStrategy.IGNORE) // si una receta ya existe, se ignora y no genera error
    suspend fun insertRecetas(recetas: List<Receta>)

    // Eliminar una receta por su ID
    @Delete
    suspend fun deleteReceta(receta: Receta)

    // Eliminar receta por su ID, de manera más explícita
    @Query("DELETE FROM recetas WHERE id = :recetaId")
    suspend fun deleteRecetaById(recetaId: String)

    // Consultar todas las recetas (si es necesario)
    @Query("SELECT * FROM recetas")
    suspend fun getAllRecetas(): List<Receta>

    // Consultar una receta por ID (si es necesario)
    @Query("SELECT * FROM recetas WHERE id = :recetaId")
    suspend fun getRecetaById(recetaId: String): Receta //antes era anullable


    // no soporta múltiples usuarios en el mismo dispositivo, porque no está filtrando por userId
    @Query("SELECT * FROM recetas WHERE esFavorita = 1")
    suspend fun getRecetasFavoritas(): List<Receta>

    // Cambiar el valor de esFavorita a 0 de la receta solicitada
    @Query("UPDATE recetas SET esFavorita = 0 WHERE id = :recetaId")
    suspend fun setEsFavoritaToZero(recetaId: String)

    // Borrar todas las recetas (si es necesario)
    @Query("DELETE FROM recetas")
    suspend fun deleteAllRecetas()

    @Query("SELECT * FROM recetas WHERE userId = :userId ORDER BY date DESC")
    suspend fun getRecetasUser(userId: String): List<Receta>

    @Query("SELECT * FROM recetas WHERE userId = '' ORDER BY date DESC")
    suspend fun getRecetasHome(): List<Receta>
}

