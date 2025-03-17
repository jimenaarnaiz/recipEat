package com.example.recipeat.data.model


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





// Para las recetas que crea localmente el user
enum class DishTypes {
    Breakfast, Lunch, Snack, Dinner
}
