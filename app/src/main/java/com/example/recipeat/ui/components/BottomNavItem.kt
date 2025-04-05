package com.example.recipeat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

// Clase sellada para representar los elementos de la barra de navegación
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.FoodBank)
    object Plan : BottomNavItem("plan", "Meal Plan", Icons.Default.LocalDining)
    object MyRecipes : BottomNavItem("myrecipes", "My Recipes", Icons.AutoMirrored.Filled.MenuBook)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person)
}