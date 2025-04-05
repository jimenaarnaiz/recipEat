package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.RecetaSimpleCardItem
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.utils.NetworkConnectivityManager

@Composable
fun FavoritesScreen(navController: NavHostController, recetasViewModel: RecetasViewModel, roomViewModel: RoomViewModel, usersViewModel: UsersViewModel) {
    // Obtener las recetas favoritas del usuario desde el ViewModel
    val favoritas = recetasViewModel.recetasFavoritas.observeAsState(emptyList())
    val uid = usersViewModel.getUidValue()
    // Estado para almacenar los ingredientes anteriores y verificar si hay cambios
    var lastFavs by rememberSaveable { mutableStateOf<List<RecetaSimple>>(emptyList()) }

    // Instanciar el NetworkConnectivityManager
    val context = LocalContext.current
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }

    val favoritasRoom = roomViewModel.favoriteRecipesRoom.observeAsState(emptyList())

    // Registrar el callback para el estado de la red
    LaunchedEffect(true) {
        networkConnectivityManager.registerNetworkCallback()
    }

    // Usar DisposableEffect para desregistrar el callback cuando la pantalla se destruye
    DisposableEffect(context) {
        // Desregistrar el NetworkCallback cuando la pantalla deje de ser visible
        onDispose {
            networkConnectivityManager.unregisterNetworkCallback()
        }
    }

    // Verificar si hay conexión
    val isConnected = networkConnectivityManager.isConnected.value

    LaunchedEffect (navController) {
       roomViewModel.getRecetasFavoritas()
    }

    LaunchedEffect(favoritas) {
        Log.d("FavoritesScreen", "recetas favs actuales ${uid}: ${favoritas.value}")
        if (favoritas != lastFavs) {
            uid.let {
                if (it != null) {
                    recetasViewModel.obtenerRecetasFavoritas(it)
                }
            }
            lastFavs = favoritas.value
        }
    }
    Log.d("FavoritesScreen", "fuera: recetas favs actuales: ${favoritas.value}")

    Scaffold(
        topBar = {
            AppBar(
                title = "My Favorites",
                navController = navController,
                onBackPressed = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        // Si no hay recetas favoritas, mostrar un mensaje
        if (favoritas.value.isEmpty()) {
            Text(
                text = "Start exploring and saving your favorites!",
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        } else
            // Mostrar las recetas favoritas en un Grid de 2 columnas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Define el número de columnas
                contentPadding = PaddingValues(16.dp), // Añadir un margen alrededor del Grid
                modifier = Modifier.padding(paddingValues)
            ) {

                if (isConnected) {
                    items(favoritas.value) { receta ->
                        RecetaSimpleCardItem(
                            recetaId = receta.id,
                            title = receta.title,
                            image = receta.image,
                            navController = navController,
                            esDeUser = receta.userReceta.isNotBlank(),
                            usersViewModel = usersViewModel
                        )
                    }
                }else{
                    items(favoritasRoom.value) { receta ->
                        RecetaSimpleCardItem(
                            recetaId = receta.id,
                            title = receta.title,
                            image = receta.image!!, // ojo
                            navController = navController,
                            esDeUser = receta.userId.isNotBlank(),
                            usersViewModel = usersViewModel
                        )
                    }
                }
            }
    }
}

