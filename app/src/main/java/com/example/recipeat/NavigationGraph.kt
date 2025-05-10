package com.example.recipeat

import LoginScreen
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.recipeat.data.repository.IngredienteRepository
import com.example.recipeat.data.repository.PlanRepository
import com.example.recipeat.data.repository.RecetaRepository
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.screens.AddRecipe
import com.example.recipeat.ui.screens.DetailsScreen
import com.example.recipeat.ui.screens.EditProfileScreen
import com.example.recipeat.ui.screens.EditRecipeScreen
import com.example.recipeat.ui.screens.FavoritesScreen
import com.example.recipeat.ui.screens.ForgotPasswordScreen
import com.example.recipeat.ui.screens.HistoryScreen
import com.example.recipeat.ui.screens.HomeScreen
import com.example.recipeat.ui.screens.MyRecipesScreen
import com.example.recipeat.ui.screens.ProfileScreen
import com.example.recipeat.ui.screens.RegisterScreen
import com.example.recipeat.ui.screens.ResNameScreen
import com.example.recipeat.ui.screens.ResIngredientsScreen
import com.example.recipeat.ui.screens.ShoppingListScreen
import com.example.recipeat.ui.screens.StepsScreen
import com.example.recipeat.ui.screens.WeeklyPlanScreen
import com.example.recipeat.ui.screens.search.UnifiedSearchScreen
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.PermissionsViewModel
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.factories.PlanViewModelFactory
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.ui.viewmodels.factories.UsersViewModelFactory


// Función para definir el grafo de navegación
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    roomViewModel: RoomViewModel,
    permissionsViewModel: PermissionsViewModel,
    onBottomBarVisibilityChanged: (Boolean) -> Unit
) {
    // Accedemos al application context
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val recetaRepository = RecetaRepository()
    val recetasViewModel = RecetasViewModel(recetaRepository)
    val sharedPreferences = remember { navController.context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE) }

    // Crear la fábrica para UsersViewModel
    val usersViewModelFactory = UsersViewModelFactory(application = LocalContext.current.applicationContext as Application, sharedPreferences = sharedPreferences)
    // Usar la fábrica para obtener el ViewModel
    val usersViewModel: UsersViewModel = viewModel(factory = usersViewModelFactory)

    val ingredienteRepository = IngredienteRepository()
    val ingredientesViewModel = IngredientesViewModel(ingredienteRepository)

    val filtrosViewModel: FiltrosViewModel = viewModel()

    // Obtener el repository de alguna manera (ejemplo en este caso)
    val planRepository = PlanRepository()
    val planViewModel: PlanViewModel = viewModel(
        factory = PlanViewModelFactory(application, planRepository)
    )

    val connectivityViewModel: ConnectivityViewModel = viewModel()

    val startDestination = remember { mutableStateOf<String?>(null) }
    // si hay una sesión activa, no se va a login
    LaunchedEffect(Unit) {
        usersViewModel.isSessionActive { isActive ->
            startDestination.value = if (isActive) "home" else "login"
        }
    }

    Scaffold { padding ->
        startDestination.value?.let {
            NavHost(
                navController = navController,
                startDestination = it// Verifica si la sesión está activa
            ) {
                composable("login") {
                    LoginScreen(navController, usersViewModel, recetasViewModel, connectivityViewModel, planViewModel) // Login
                    onBottomBarVisibilityChanged(false)
                }
                composable("register") {
                    RegisterScreen(navController, usersViewModel, planViewModel) // Registro
                    onBottomBarVisibilityChanged(false)
                }
                composable(BottomNavItem.Home.route) {
                    HomeScreen(navController, usersViewModel, recetasViewModel, roomViewModel, connectivityViewModel, permissionsViewModel)   // Home
                    onBottomBarVisibilityChanged(true)
                }
                composable(BottomNavItem.MyRecipes.route) {
                    MyRecipesScreen(navController, recetasViewModel, roomViewModel, usersViewModel, connectivityViewModel)   //
                    onBottomBarVisibilityChanged(true)
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(navController, usersViewModel, connectivityViewModel) // Perfil
                    onBottomBarVisibilityChanged(true)
                }
                composable("resultados/{query}") { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    ResNameScreen(nombreReceta = query, navController = navController, recetasViewModel, filtrosViewModel, usersViewModel, connectivityViewModel)
                    onBottomBarVisibilityChanged(false)
                }
                composable("detalles/{idReceta}/{deUser}") { backStackEntry ->
                    val idReceta = backStackEntry.arguments?.getString("idReceta") ?: ""
                    val deUser = backStackEntry.arguments?.getString("deUser")?.toBoolean() ?: false

                    // Verifica que se haya obtenido el valor correctamente
                    Log.d("DetailsScreen", "Valor de deUser: $deUser")

                    // Ahora pasamos el parámetro 'deUser' a la pantalla de detalles
                    DetailsScreen(idReceta = idReceta, navController = navController, recetasViewModel = recetasViewModel, esDeUser = deUser, roomViewModel, usersViewModel, connectivityViewModel)
                    onBottomBarVisibilityChanged(false)
                }
                composable("search") {
                    UnifiedSearchScreen(navController, recetasViewModel, ingredientesViewModel, connectivityViewModel) // Búsqueda por nombre
                    onBottomBarVisibilityChanged(false)
                }
                composable("resultadosIngredientes") {
                    ResIngredientsScreen(navController, recetasViewModel, ingredientesViewModel, filtrosViewModel, usersViewModel, connectivityViewModel)
                    onBottomBarVisibilityChanged(false)
                }
                composable("add_recipe") {
                    AddRecipe(navController, recetasViewModel, ingredientesViewModel, roomViewModel, usersViewModel, connectivityViewModel, permissionsViewModel)   //
                    onBottomBarVisibilityChanged(false)
                }
                composable("favoritos") {
                    FavoritesScreen(navController, recetasViewModel, roomViewModel, usersViewModel, connectivityViewModel)   //
                    onBottomBarVisibilityChanged(false)
                }
                composable("historial") {
                    HistoryScreen(navController, recetasViewModel, usersViewModel, connectivityViewModel)   //
                    onBottomBarVisibilityChanged(false)
                }
                composable("editarPerfil") {
                    EditProfileScreen(navController, usersViewModel, connectivityViewModel, permissionsViewModel)
                    onBottomBarVisibilityChanged(false)
                }
                composable("editRecipe/{idReceta}/{deUser}") { backStackEntry ->
                    val idReceta = backStackEntry.arguments?.getString("idReceta") ?: ""
                    val deUser = backStackEntry.arguments?.getString("deUser")?.toBoolean() ?: false
                    // Ahora pasamos 'deUser' a la pantalla
                    EditRecipeScreen(idReceta = idReceta, navController = navController, recetasViewModel = recetasViewModel, ingredientesViewModel, deUser = deUser, usersViewModel, connectivityViewModel, permissionsViewModel, roomViewModel)
                    onBottomBarVisibilityChanged(false)
                }
                composable("steps/{idReceta}/{deUser}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("idReceta") ?: ""
                    val deUser = backStackEntry.arguments?.getString("deUser")?.toBoolean() ?: false
                    StepsScreen(idReceta = id, navController = navController, recetasViewModel, deUser = deUser, connectivityViewModel)
                    onBottomBarVisibilityChanged(false)
                }
                composable(BottomNavItem.Plan.route) {
                    WeeklyPlanScreen(navController, planViewModel, usersViewModel, connectivityViewModel)
                    onBottomBarVisibilityChanged(true)
                }
                composable("listaCompra") {
                    ShoppingListScreen(navController, usersViewModel, planViewModel, connectivityViewModel)
                    onBottomBarVisibilityChanged(false)
                }
//                composable("debug") {
//                    DebugScreen()
//                    onBottomBarVisibilityChanged(true)
//                }
                composable("forgotPassword") {
                    ForgotPasswordScreen(navController, usersViewModel)
                    onBottomBarVisibilityChanged(false)
                }

            }
        }
    }
}


