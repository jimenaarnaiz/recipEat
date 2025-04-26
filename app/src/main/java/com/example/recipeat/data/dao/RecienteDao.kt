package com.example.recipeat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.Reciente

@Dao
interface RecienteDao {

    // Insertar un nuevo registro en la tabla reciente
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarReciente(reciente: Reciente)

    // Contar las recetas recientes de un usuario
    @Query("SELECT COUNT(*) FROM recientes WHERE userId = :userId")
    suspend fun contarRecientes(userId: String): Int


    @Query("""
    SELECT recetaId FROM recientes 
    WHERE userId = :userId 
    ORDER BY fechaVista ASC 
    LIMIT 1
    """)
    suspend fun obtenerIdRecetaMasAntigua(userId: String): String?


    @Query("SELECT EXISTS(SELECT 1 FROM recientes WHERE recetaId = :recetaId AND userId = :userId)")
    suspend fun existeReciente(recetaId: String, userId: String): Boolean

    // Obtener las recetas recientes de un usuario
    @Query("""
        SELECT recetas.*
        FROM recetas
        INNER JOIN recientes ON recetas.id = recientes.recetaId
        WHERE recientes.userId = :userId
        ORDER BY recientes.fechaVista DESC
    """)
    suspend fun getRecientes(userId: String): List<Receta>

    // Eliminar todas las recetas recientes de un usuario
    @Query("DELETE FROM recientes WHERE userId = :userId")
    suspend fun eliminarRecientes(userId: String)

    // Eliminar una receta específica de la lista de recientes
    @Query("DELETE FROM recientes WHERE recetaId = :recetaId AND userId = :userId")
    suspend fun eliminarRecientePorId(recetaId: String, userId: String)
}