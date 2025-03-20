package com.example.recipeat.data.model

import com.google.firebase.Timestamp


// Receta almacenada en Firebase
data class Receta(
    val id: String,
    val title: String,
    val image: String?,
    val servings: Int,
    val ingredients: List<Ingrediente>,
    val steps: List<String>,
    val time: Int,
    val dishTypes: List<String>, //Breakfast, Lunch, Snack, Dinner
    val userId: String, // ID del usuario que ha creado la receta
    val usedIngredientCount: Int = ingredients.size,
    val glutenFree: Boolean,
    val vegan: Boolean,
    val vegetarian: Boolean,
    val date: Long,
    val unusedIngredients: List<IngredienteSimple>, // nombre e imagen
    val missingIngredientCount: Int, // Número de ingredientes faltantes
    val unusedIngredientCount: Int, // Número de ingredientes no usados
)

data class SugerenciaReceta(val id: String, val titulo: String, val coincidencias: Int)

// para mostrar en favs e historial
data class RecetaSimple(val id: String, val title: String, val image: String, val date: Timestamp)


// Para las recetas que crea localmente el user
enum class DishTypes {
    breakfast, lunch, dessert, snack, dinner
}
