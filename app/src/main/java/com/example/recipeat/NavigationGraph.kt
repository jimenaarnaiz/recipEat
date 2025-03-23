package com.example.recipeat

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.screens.AddRecipe
import com.example.recipeat.ui.screens.DetailsScreen
import com.example.recipeat.ui.screens.EditProfileScreen
import com.example.recipeat.ui.screens.FavoritesScreen
import com.example.recipeat.ui.screens.HistoryScreen
import com.example.recipeat.ui.screens.HomeScreen
import com.example.recipeat.ui.screens.LoginScreen
import com.example.recipeat.ui.screens.MyRecipesScreen
import com.example.recipeat.ui.screens.ProfileScreen
import com.example.recipeat.ui.screens.RegisterScreen
import com.example.recipeat.ui.screens.ResNameScreen
import com.example.recipeat.ui.screens.ResIngredientsScreen
import com.example.recipeat.ui.screens.search.UnifiedSearchScreen
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel


// Función para definir el grafo de navegación
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(navController: NavHostController, onBottomBarVisibilityChanged: (Boolean) -> Unit) {
    val recetasViewModel: RecetasViewModel = viewModel()
    val usersViewModel: UsersViewModel = viewModel()
    val ingredientesViewModel: IngredientesViewModel = viewModel()
    val filtrosViewModel: FiltrosViewModel = viewModel()

    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "login", // TODO Pantalla de inicio es el login
            //modifier = Modifier.padding(padding) AÑADE PADDING INNECESARIO ARRIBA
        ) {
            composable("login") {
                LoginScreen(navController, usersViewModel, recetasViewModel) // Login
                onBottomBarVisibilityChanged(false)
            }
            composable("register") {
                RegisterScreen(navController, usersViewModel, recetasViewModel, ingredientesViewModel) // Registro
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
                ProfileScreen(navController, usersViewModel) // Perfil
                onBottomBarVisibilityChanged(true)
            }
            composable("resultados/{query}") { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                ResNameScreen(nombreReceta = query, navController = navController, recetasViewModel, filtrosViewModel)
                onBottomBarVisibilityChanged(false)
            }
            composable("detalles/{query}") { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query")?.toIntOrNull() ?: 0
                DetailsScreen(idReceta = query, navController = navController, recetasViewModel,)
                onBottomBarVisibilityChanged(false)
            }
            composable("search") {
                UnifiedSearchScreen(navController, recetasViewModel, ingredientesViewModel) // Búsqueda por nombre
                onBottomBarVisibilityChanged(false)
            }
            composable("resultadosIngredientes") {
                ResIngredientsScreen(navController, recetasViewModel, ingredientesViewModel, filtrosViewModel)
                onBottomBarVisibilityChanged(false)
            }
            composable("add_recipe") {
                AddRecipe(navController, recetasViewModel, ingredientesViewModel)   //
                onBottomBarVisibilityChanged(false)
            }
            composable("favoritos") {
                FavoritesScreen(navController, recetasViewModel)   //
                onBottomBarVisibilityChanged(false)
            }
            composable("historial") {
                HistoryScreen(navController, recetasViewModel)   //
                onBottomBarVisibilityChanged(false)
            }
            composable("editarPerfil") { //backStackEntry ->
//                val id = backStackEntry.arguments?.getString("userId") ?: ""
                EditProfileScreen(navController, usersViewModel)
                onBottomBarVisibilityChanged(false)
            }

            //TODO screen de editar mi receta


        }
    }
}


