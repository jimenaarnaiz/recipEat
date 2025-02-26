package com.example.recipeat.data.model

data class User(
    val id: String, // ID único del usuario
    val username: String,
    val image: String,
    val email: String,
    val rol: String,
    val favRecipes: List<String> // Lista de IDs de recetas favoritas
)

enum class Rol {
    Anonymous, Registered
}