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
    val missedIngredientCount: Int?, //solo para busqueda por ingredientes
    val usedIngredientCount: Int, //solo para busqueda por ingredientes
    val missedIngredients: List<Ingrediente>?, //solo para busqueda por ingredientes
    val glutenFree: Boolean?,
    val vegan: Boolean?,
    val vegetarian: Boolean?
)

// Respuesta de la llamada random recipes de la api
data class RandomRecipesResponse(
    val recipes: List<ApiReceta>
)

// Función para mapear ApiReceta a Receta
fun mapApiRecetaToReceta(apiReceta: ApiReceta, uid: String): Receta {
    return Receta(
        id = apiReceta.id,
        title = apiReceta.title,
        image = apiReceta.image,
        ingredients = apiReceta.extendedIngredients,
        steps = apiReceta.instructions,
        time = apiReceta.readyInMinutes,
        dishTypes = apiReceta.dishTypes,
        user = uid,  // ID del usuario

    )

}