package com.example.recipeat.data.api

import com.example.recipeat.BuildConfig.API_SPOONACULAR_KEY
import com.example.recipeat.data.model.ApiReceta
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.RandomRecipesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz para interactuar con la API de Spoonacular.
 * Contiene endpoints para buscar recetas, obtener detalles de recetas...
 */
interface SpoonacularApi {

    // Obtener recetas random
    @GET("recipes/random")
    suspend fun obtenerRecetasRandom(
        @Query("number") number: Int = 90,
        @Query("apiKey") apiKey: String = API_SPOONACULAR_KEY
    ): RandomRecipesResponse

    // Obtener instrucciones en diferentes strings de una receta por ID
    @GET("recipes/{id}/analyzedInstructions")
    suspend fun obtenerInstruccionesReceta(
        @Path("id") recetaId: Int,
        @Query("stepBreakdown") stepBreakdown: Boolean = true,
        @Query("apiKey") apiKey: String = API_SPOONACULAR_KEY
    ) : List<Map<String, Any>>


    //  Buscar recetas por ingredientes
    @GET("recipes/findByIngredients")
    suspend fun buscarRecetasPorIngredientes(
        @Query("ingredients") ingredientes: String,
        @Query("number") number: Int = 22,
        @Query("apiKey") apiKey: String = API_SPOONACULAR_KEY
    ): List<ApiReceta>

    //  Buscar recetas
    @GET("recipes/informationBulk")
    suspend fun obtenerRecetasBulk(
        @Query("ids") recetas_ids: String,
        @Query("includeNutrition") includeNutrition: Boolean = false,
        @Query("apiKey") apiKey: String = API_SPOONACULAR_KEY
    ): List<ApiReceta>


    //  Buscar ingredientes
    @GET("/food/ingredients/search")
    suspend fun obtenerIngrediente(
        @Query("ingredient") ingredient: String,
        @Query("number") results: Int = 1,
        @Query("apiKey") apiKey: String = API_SPOONACULAR_KEY
    ): IngredienteSimple


}