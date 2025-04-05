package com.example.recipeat.data.model

import java.time.DayOfWeek

data class PlanSemanal(
    val weekMeals: Map<DayOfWeek, DayMeal> // Un mapa que asocia un día de la semana con las comidas
)

data class DayMeal(
    val breakfast: Receta,  // Receta para el desayuno
    val lunch: Receta,      // Receta para la comida
    val dinner: Receta     // Receta para la cena
)

