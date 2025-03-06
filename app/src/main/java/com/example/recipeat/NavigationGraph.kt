package com.example.recipeat

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.screens.HomeScreen
import com.example.recipeat.ui.screens.LoginScreen
import com.example.recipeat.ui.screens.MyRecipesScreen
import com.example.recipeat.ui.screens.ProfileScreen
import com.example.recipeat.ui.screens.RegisterScreen
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel


// Función para definir el grafo de navegación
@Composable
fun NavigationGraph(navController: NavHostController, onBottomBarVisibilityChanged: (Boolean) -> Unit) {
    val recetasViewModel: RecetasViewModel = viewModel()
    val usersViewModel: UsersViewModel = viewModel()

    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "login", // Pantalla de inicio es el login
            modifier = Modifier.padding(padding)
        ) {
            composable("login") {
                LoginScreen(navController, usersViewModel) // Login
                onBottomBarVisibilityChanged(false)
            }
            composable("register") {
                RegisterScreen(navController, usersViewModel, recetasViewModel) // Registro
                onBottomBarVisibilityChanged(false)
            }
            composable(BottomNavItem.Home.route) {
                HomeScreen(navController, recetasViewModel)   // Home
                onBottomBarVisibilityChanged(true)
            }
            composable(BottomNavItem.MyRecipes.route) {
                MyRecipesScreen(navController, recetasViewModel)   //
                onBottomBarVisibilityChanged(true)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(navController, usersViewModel, recetasViewModel) // Perfil
                onBottomBarVisibilityChanged(true)
            }
//            composable("add_recipe") {
//                AddRecipe(navController, recetasViewModel)   //
//                onBottomBarVisibilityChanged(false)
//            }
//            composable("resultados") {
//                RecetasScreen(navController, recetasViewModel)   //
//                onBottomBarVisibilityChanged(false)
//            }
        }
    }
}


