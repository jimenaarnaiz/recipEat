package com.example.recipeat.data.repository

import com.example.recipeat.data.dao.RecetaDao
import com.example.recipeat.data.model.Receta

class RecetaRepository(private val recetaDao: RecetaDao) {

    // Obtener todas las recetas
    suspend fun getAllRecetas(): List<Receta> {
        return recetaDao.getAllRecetas()
    }

    // Insertar receta
    suspend fun insertReceta(receta: Receta) {
        recetaDao.insertReceta(receta)
    }

    // Eliminar receta
    suspend fun deleteReceta(receta: Receta) {
        recetaDao.deleteReceta(receta)
    }

    // Eliminar receta por ID
    suspend fun deleteRecetaById(recetaId: String) {
        recetaDao.deleteRecetaById(recetaId)
    }

    // Obtener recetas favoritas
    suspend fun getRecetasFavoritas(): List<Receta> {
        return recetaDao.getRecetasFavoritas()
    }

    // Obtener receta por ID
    suspend fun getRecetaById(recetaId: String): Receta {
        return recetaDao.getRecetaById(recetaId)
    }

    suspend fun setEsFavoritaToZero(recetaId: String){
        return recetaDao.setEsFavoritaToZero(recetaId)
    }

    suspend fun deleteAllRecetas(){
        return recetaDao.deleteAllRecetas()
    }

    suspend fun getRecetasUser(userId: String): List<Receta>{
        return recetaDao.getRecetasUser(userId = userId)
    }

}

