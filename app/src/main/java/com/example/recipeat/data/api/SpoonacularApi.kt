package com.example.recipeat.data.api

import com.example.recipeat.data.model.RandomRecipesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApi {

//    //  Buscar recetas por ingredientes
//    @GET("recipes/findByIngredients")
//    suspend fun buscarRecetasPorIngredientes(
//        @Query("ingredients") ingredientes: String,
//        @Query("number") number: Int = 10,
//        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
//    ): List<ApiReceta>
//
//    // Obtener detalles de una receta por ID
//    @GET("recipes/{id}/information")
//    suspend fun obtenerDetallesReceta(
//        @Path("id") recetaId: Int,
//        @Query("includeNutrition") includeNutrition: Boolean = false,
//        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
//    ): ApiReceta
//
//    // Buscar ingredientes por nombre autocomplete
//    @GET("food/ingredients/autocomplete")
//    suspend fun buscarIngredientesAutocompletado(
//        @Query("query") ingredient: String,
//        @Query("number") number: Int = 7,
//        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
//    ): List<Ingrediente>
//
//    // Buscar recetas por nombre
//    @GET("recipes/complexSearch")
//    suspend fun buscarRecetasPorNombre(
//        @Query("query") name: String,
//        @Query("number") number: Int = 50, // num de resultados
//        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
//    ): List<ApiReceta>
//
//    @GET("recipes/autocomplete")
//    suspend fun buscarRecetasPorNombreAutocompletado(
//        @Query("query") recipe: String,
//        @Query("number") number: Int = 7,
//        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
//    ): List<ApiReceta>
//
//    // Obtener recetas similares a una receta por ID
//    @GET("recipes/{id}/similar")
//    suspend fun obtenerRecetasSimilares(
//        @Path("id") recetaId: Int,
//        @Query("number") number: Int = 10,
//        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
//    ): List<ApiReceta>

    // Obtener recetas random
    @GET("recipes/random")
    suspend fun obtenerRecetasRandom(
        @Query("number") number: Int = 10, //TODO poner 100 cuando acabe las pruebas
        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
    ): RandomRecipesResponse

    // Obtener instrucciones en diferentes strings de una receta por ID
    @GET("recipes/{id}/analyzedInstructions")
    suspend fun obtenerInstruccionesReceta(
        @Path("id") recetaId: Int,
        @Query("stepBreakdown") stepBreakdown: Boolean = true,
        @Query("apiKey") apiKey: String = "ec231e7612fa4dd399b9e2f2c0f9bcc8"
    ) : List<Map<String, Any>>

}