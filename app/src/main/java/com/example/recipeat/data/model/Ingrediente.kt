package com.example.recipeat.data.model


data class Ingrediente(
    val name: String,
    val image: String,
    val amount: Double,
    val unit: String,
    val aisle: String
)

//para las card de favs e historial
data class IngredienteSimple(val name: String, val image: String)

data class IngredienteCompra(
    val name: String,
    val aisle: String,
    val image: String,
    var medidas: List<Pair<Double, String>>,
    val estaComprado: Boolean
)