package com.example.recipeat

import LoginScreen
import android.os.Build
import android.util.Log
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
import com.example.recipeat.ui.screens.EditRecipeScreen
import com.example.recipeat.ui.screens.FavoritesScreen
import com.example.recipeat.ui.screens.HistoryScreen
import com.example.recipeat.ui.screens.HomeScreen
import com.example.recipeat.ui.screens.MyRecipesScreen
import com.example.recipeat.ui.screens.ProfileScreen
import com.example.recipeat.ui.screens.RegisterScreen
import com.example.recipeat.ui.screens.ResNameScreen
import com.example.recipeat.ui.screens.ResIngredientsScreen
import com.example.recipeat.ui.screens.StepsScreen
import com.example.recipeat.ui.screens.WeeklyPlanScreen
import com.example.recipeat.ui.screens.search.UnifiedSearchScreen
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel


// Función para definir el grafo de navegación
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    roomViewModel: RoomViewModel,
    onBottomBarVisibilityChanged: (Boolean) -> Unit
) {
    val recetasViewModel: RecetasViewModel = viewModel()
    val usersViewModel: UsersViewModel = viewModel()
    val ingredientesViewModel: IngredientesViewModel = viewModel()
    val filtrosViewModel: FiltrosViewModel = viewModel()
    val planViewModel: PlanViewModel = viewModel()


    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "login", // TODO Pantalla de inicio es el login
            //modifier = Modifier.padding(padding) AÑADE PADDING INNECESARIO ARRIBA
        ) {
            composable("login") {
                LoginScreen(navController, usersViewModel, recetasViewModel, roomViewModel, planViewModel) // Login
                onBottomBarVisibilityChanged(false)
            }
            composable("register") {
                RegisterScreen(navController, usersViewModel, recetasViewModel, ingredientesViewModel) // Registro
                onBottomBarVisibilityChanged(false)
            }
            composable(BottomNavItem.Home.route) {
                HomeScreen(navController, usersViewModel, recetasViewModel)   // Home
                onBottomBarVisibilityChanged(true)
            }
            composable(BottomNavItem.MyRecipes.route) {
                MyRecipesScreen(navController, recetasViewModel, roomViewModel, usersViewModel)   //
                onBottomBarVisibilityChanged(true)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(navController, usersViewModel) // Perfil
                onBottomBarVisibilityChanged(true)
            }
            composable("resultados/{query}") { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                ResNameScreen(nombreReceta = query, navController = navController, recetasViewModel, filtrosViewModel, usersViewModel)
                onBottomBarVisibilityChanged(false)
            }
            composable("detalles/{idReceta}/{deUser}") { backStackEntry ->
                val idReceta = backStackEntry.arguments?.getString("idReceta") ?: ""
                val deUser = backStackEntry.arguments?.getString("deUser")?.toBoolean() ?: false

                // Verifica que se haya obtenido el valor correctamente
                Log.d("DetailsScreen", "Valor de deUser: $deUser")

                // Ahora pasamos el parámetro 'deUser' a la pantalla de detalles
                DetailsScreen(idReceta = idReceta, navController = navController, recetasViewModel = recetasViewModel, esDeUser = deUser, roomViewModel, usersViewModel)
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
                AddRecipe(navController, recetasViewModel, ingredientesViewModel, roomViewModel, usersViewModel)   //
                onBottomBarVisibilityChanged(false)
            }
            composable("favoritos") {
                FavoritesScreen(navController, recetasViewModel, roomViewModel, usersViewModel)   //
                onBottomBarVisibilityChanged(false)
            }
            composable("historial") {
                HistoryScreen(navController, recetasViewModel, usersViewModel)   //
                onBottomBarVisibilityChanged(false)
            }
            composable("editarPerfil") {
                EditProfileScreen(navController, usersViewModel)
                onBottomBarVisibilityChanged(false)
            }
            composable("editRecipe/{idReceta}/{deUser}") { backStackEntry ->
                val idReceta = backStackEntry.arguments?.getString("idReceta") ?: ""
                val deUser = backStackEntry.arguments?.getString("deUser")?.toBoolean() ?: false
                // Ahora pasamos 'deUser' a la pantalla
                EditRecipeScreen(idReceta = idReceta, navController = navController, recetasViewModel = recetasViewModel, ingredientesViewModel, deUser = deUser, usersViewModel)
                onBottomBarVisibilityChanged(false)
            }
            composable("steps/{idReceta}/{deUser}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("idReceta") ?: ""
                val deUser = backStackEntry.arguments?.getString("deUser")?.toBoolean() ?: false
                StepsScreen(idReceta = id, navController = navController, recetasViewModel, deUser = deUser)
                onBottomBarVisibilityChanged(false)
            }
            composable(BottomNavItem.Plan.route) {
                WeeklyPlanScreen(navController, planViewModel)
                onBottomBarVisibilityChanged(true)
            }

        }
    }
}


