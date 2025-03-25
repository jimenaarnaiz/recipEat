package com.example.recipeat.data.model

import java.io.Serializable

data class Ingrediente(
    val name: String,
    val image: String,
    val amount: Double,
    val unit: String
)

data class IngredienteSimple(val name: String, val image: String)