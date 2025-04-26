package com.example.recipeat.data.repository

import android.util.Log
import com.example.recipeat.data.dao.FavoritoDao
import com.example.recipeat.data.dao.RecetaRoomDao
import com.example.recipeat.data.dao.RecienteDao
import com.example.recipeat.data.model.Favorito
import com.example.recipeat.data.model.Receta
import com.example.recipeat.data.model.Reciente
import java.sql.Timestamp

class RecetaRoomRepository(
    private val recetaRoomDao: RecetaRoomDao,
    private val favoritoDao: FavoritoDao,
    private val recienteDao: RecienteDao
) {
    private val _limiteRecientes = 15

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
    suspend fun getRecetaById(recetaId: String): Receta? {
        return recetaRoomDao.getRecetaById(recetaId)
    }

    suspend fun deleteAllRecetas(){
        return recetaRoomDao.deleteAllRecetas()
    }

    suspend fun getRecetasUser(userId: String): List<Receta>{
        return recetaRoomDao.getRecetasUser(userId = userId)
    }

    // Función para obtener las recetas que deben mostrarse en Home, con prioridad para las recientes
    suspend fun getRecetasHome(userId: String, idsRecetasHome: List<String>): List<Receta> {
        // Obtener las recetas recientes desde Room
        val recientes = recienteDao.getRecientes(userId)

        // Crear una lista mutable para almacenar las recetas
        val recetasHome = mutableListOf<Receta>()

        // Agregar las recetas recientes primero
        recetasHome.addAll(recientes)

        // Agregar las recetas de SharedPreferences, pero solo si no están ya en la lista de recientes
        for (id in idsRecetasHome) {
            val receta = recetaRoomDao.getRecetaById(id)
            if (receta != null && !recetasHome.contains(receta)) {
                recetasHome.add(receta)
            }
        }

        // Devolvemos la lista final de recetas
        return recetasHome
    }

    suspend fun updateReceta(receta: Receta) {
        return recetaRoomDao.updateReceta(receta)
    }


    //favoritos

    // Agregar una receta a los favoritos
    suspend fun agregarFavorito(userId: String, recetaId: String) {
        // Verificar si el favorito ya existe
        val existeFavorito = favoritoDao.esFavorita(userId, recetaId)
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

    suspend fun getRecetasFavoritas(userId: String): List<Receta> {
        val favoritos = favoritoDao.obtenerFavoritosPorUsuario(userId)
        val recetasFavoritas = mutableListOf<Receta>()

        for (favorito in favoritos) {
            val receta = recetaRoomDao.getRecetaById(favorito.recetaId)
            if (receta != null) {
                recetasFavoritas.add(receta)
            }
        }
        return recetasFavoritas
    }


    // Verificar si una receta está marcada como favorita
    suspend fun esFavorita(userId: String, recetaId: String): Boolean {
        return favoritoDao.esFavorita(userId, recetaId) != null
    }


    // Métdo para eliminar todos los favoritos de un usuario
    suspend fun eliminarTodosLosFavoritos(userId: String) {
        favoritoDao.eliminarTodosLosFavoritos(userId)
    }


    // recientes

    // Insertar una receta reciente
    suspend fun insertarReciente(receta: Receta, userId: String) {
        // Verificamos si ya existe en recientes
        val existe = recienteDao.existeReciente(receta.id, userId)

        if (!existe) {
            // Solo si NO existe, miramos si estamos en el límite
            val count = recienteDao.contarRecientes(userId)
            if (count >= _limiteRecientes) {
                // Si llegamos al límite, eliminamos la receta más antigua
                val idRecetaAntigua = recienteDao.obtenerIdRecetaMasAntigua(userId)
                if (idRecetaAntigua != null) {
                    // Primero eliminamos de recientes
                    recienteDao.eliminarRecientePorId(idRecetaAntigua, userId)
                    // Luego eliminamos de recetas
                    recetaRoomDao.deleteRecetaById(idRecetaAntigua)
                }
            }
        }

        // Insertamos o actualizamos la receta reciente
        val reciente = Reciente(
            recetaId = receta.id,
            userId = userId,
            fechaVista = Timestamp(System.currentTimeMillis()) // Usamos la fecha actual
        )

        // Insertamos la receta en la tabla
        recienteDao.insertarReciente(reciente)
    }

    // Obtener las recetas recientes de un usuario
    suspend fun obtenerRecientes(userId: String): List<Receta> {
        return recienteDao.getRecientes(userId)
    }

    // Eliminar todas las recetas recientes
    suspend fun eliminarRecientes(userId: String) {
        recienteDao.eliminarRecientes(userId)
    }

    // Eliminar una receta específica de las recientes
    suspend fun eliminarRecientePorId(recetaId: String, userId: String) {
        recienteDao.eliminarRecientePorId(recetaId, userId)
    }

}

