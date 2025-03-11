package com.example.recipeat.data.model

// Receta con la estructura que usa la API
data class ApiReceta(
    val id: Int,
    val title: String,
    val image: String,
    val extendedIngredients: List<Ingrediente>,
    val instructions: String,
    val readyInMinutes: Int,
    val dishTypes: List<String>,
    val missedIngredientCount: Int, //solo para busqueda por ingredientes
    val usedIngredientCount: Int,
    val missedIngredients: List<Ingrediente>, //solo para busqueda por ingredientes
    val unusedIngredients: List<Ingrediente>,
    val glutenFree: Boolean,
    val vegan: Boolean,
    val vegetarian: Boolean
)

// Respuesta de la llamada random recipes de la api
data class RandomRecipesResponse(
    val recipes: List<ApiReceta>
)

// Respuesta de la llamada buscar recetas por nombre de la api
data class RecipesNameResponse(
    val results: List<ApiReceta>
)

