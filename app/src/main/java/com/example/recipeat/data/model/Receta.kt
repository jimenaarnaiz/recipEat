package com.example.recipeat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.recipeat.data.model.converters.IngredienteListConverter
import com.example.recipeat.data.model.converters.StepsTypeDishListConverter
import com.google.firebase.Timestamp


@Entity(tableName = "recetas") //para recetas del user y favs
// Receta almacenada en Firebase
data class Receta(
    @PrimaryKey val id: String,
    val title: String,
    val image: String?,
    val servings: Int,
    @TypeConverters(IngredienteListConverter::class) val ingredients: List<Ingrediente>,
    @TypeConverters(StepsTypeDishListConverter::class) val steps: List<String>,
    val time: Int,
    @TypeConverters(StepsTypeDishListConverter::class) val dishTypes: List<String>, //Breakfast, Lunch, dessert, Dinner
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
data class RecetaSimple(val id: String, val userId: String, val title: String, val image: String, val date: Timestamp)

// Para las recetas que crea localmente el user
enum class DishTypes {
    breakfast, lunch, dinner, dessert
}



