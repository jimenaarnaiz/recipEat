package com.example.recipeat

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recipeat.ui.components.BottomNavBar
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.screens.HomeScreen
import com.example.recipeat.ui.screens.LoginScreen
import com.example.recipeat.ui.screens.MyRecipesScreen
import com.example.recipeat.ui.screens.RegisterScreen


// Función para definir el grafo de navegación
@Composable
fun NavigationGraph(navController: NavHostController, onBottomBarVisibilityChanged: (Boolean) -> Unit) {

    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "home", // Pantalla de inicio es el login
            modifier = Modifier.padding(padding)
        ) {
            composable("login") {
                LoginScreen(navController) // Login
                onBottomBarVisibilityChanged(false)
            }
            composable("register") {
                RegisterScreen(navController) // Registro
                onBottomBarVisibilityChanged(false)
            }
            composable(BottomNavItem.Home.route) {
                HomeScreen(navController)   // Home
                onBottomBarVisibilityChanged(true)
            }
            composable(BottomNavItem.MyRecipes.route) {
                MyRecipesScreen(navController)   //
                onBottomBarVisibilityChanged(true)
            }
//            composable("add_recipe") {
//                AddRecipe(navController)   //
//                onBottomBarVisibilityChanged(false)
//            }
//            composable("resultados") {
//                RecetasScreen(navController)   //
//                onBottomBarVisibilityChanged(false)
//            }
        }
    }
}


