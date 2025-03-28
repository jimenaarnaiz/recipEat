package com.example.recipeat.data.repository

import com.example.recipeat.data.dao.RecetaDao
import com.example.recipeat.data.model.RecetaRoom

class RecetaRepository(private val recetaDao: RecetaDao) {

    // Obtener todas las recetas
    suspend fun getAllRecetas(): List<RecetaRoom> {
        return recetaDao.getAllRecetas()
    }

    // Insertar receta
    suspend fun insertReceta(receta: RecetaRoom) {
        recetaDao.insertReceta(receta)
    }

    // Eliminar receta
    suspend fun deleteReceta(receta: RecetaRoom) {
        recetaDao.deleteReceta(receta)
    }

    // Eliminar receta por ID
    suspend fun deleteRecetaById(recetaId: String) {
        recetaDao.deleteRecetaById(recetaId)
    }

    // Obtener recetas favoritas
    suspend fun getRecetasFavoritas(): List<RecetaRoom> {
        return recetaDao.getRecetasFavoritas()
    }

    // Obtener receta por ID
    suspend fun getRecetaById(recetaId: String): RecetaRoom? {
        return recetaDao.getRecetaById(recetaId)
    }
}

