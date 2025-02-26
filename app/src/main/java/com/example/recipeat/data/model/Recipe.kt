package com.example.recipeat.data.model

data class Recipe(
    val id: String, // ID de Firebase para recetas de usuario o de Spoonacular para recetas de la API
    val title: String,
    val image: String,
    val servings: Int,
    val ingredients: List<Ingredient>,
    val steps: List<String> ,
    val readyInMinutes: Int?,
    val typeDish: List<String>?, // Para mostrar en qué ocasiones se recomienda
    val missedIngredientCount: Int,
    val usedIngredientCount: Int,
    val user: String?, // ID del usuario que ha creado la receta
    val createdByUser: Boolean // Indica si la receta es de Spoonacular o creada por el usuario,
    //val missedIngredients: List<Ingrediente>
)

enum class TypeDish {
    Breakfast, Lunch, Snack, Dinner
}