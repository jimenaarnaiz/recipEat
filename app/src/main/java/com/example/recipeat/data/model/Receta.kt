package com.example.recipeat.data.model


// Receta almacenada en Firebase
data class Receta(
    val id: Int,
    val title: String,
    val image: String?,
    val ingredients: List<Ingrediente>,
    val steps: String,
    val time: Int,
    val dishTypes: List<String>?, //Breakfast, Lunch, Snack, Dinner
    val user: String, // ID del usuario que ha creado la receta
    val usedIngredientCount: Int = ingredients.size
)

// Para las recetas que crea localmente el user
enum class DishTypes {
    Breakfast, Lunch, Snack, Dinner
}
