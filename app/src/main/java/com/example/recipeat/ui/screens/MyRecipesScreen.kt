package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.ui.components.RecetaCard
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.utils.NetworkConnectivityManager

@Composable
fun MyRecipesScreen(navController: NavHostController, recetasViewModel: RecetasViewModel, roomViewModel: RoomViewModel, usersViewModel: UsersViewModel) {
    val userId = usersViewModel.getUidValue()
    val recetasUser by recetasViewModel.recetasUser.observeAsState(emptyList())

    // Instanciar el NetworkConnectivityManager
    val context = LocalContext.current
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }

    val recetasRoomUser by roomViewModel.userRecipesRoom.observeAsState(emptyList())

    val listState = rememberLazyListState()
    val isLoadingMore by recetasViewModel.isLoadingMore.observeAsState(false)

    // Registrar el callback de conexión
    LaunchedEffect(true) { networkConnectivityManager.registerNetworkCallback() }
    DisposableEffect(context) { onDispose { networkConnectivityManager.unregisterNetworkCallback() } }

    val isConnected = networkConnectivityManager.isConnected.value


    // Carga inicial de las recetas
    LaunchedEffect(userId) {
        if (recetasUser.isEmpty()) {
            Log.d("MyRecipeScreen", "Carga inicial de recetas del user")
            recetasViewModel.getRecetasUser(userId.toString(), limpiarLista = true)
            roomViewModel.getRoomRecetasUser(userId.toString())
        }
    }

    // cuando se agreguen nuevas recetas de user se actualizan las recetas a mostar
    LaunchedEffect(recetasUser) {
        Log.d("MyRecipeScreen", "Se han actualizado las recetas del user")
        recetasViewModel.getRecetasUser(userId.toString(), limpiarLista = true)
        roomViewModel.getRoomRecetasUser(userId.toString())
    }

    // Detectar cuando el usuario está cerca del final de la lista solo cuando hay conexión
    if (isConnected){
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastVisibleItemIndex ->
                    val totalItemsCount = recetasUser.size
                    if (lastVisibleItemIndex != null && totalItemsCount >= 15) { // Para evitar llamadas innecesarias
                        val umbral =
                            totalItemsCount - 5 // Cargar más cuando queden 5 recetas visibles
                        if (lastVisibleItemIndex >= umbral && !isLoadingMore) {
                            Log.d("MyRecipesScreen", "Cargando más recetas...")
                            recetasViewModel.getRecetasUser(userId.toString(), limpiarLista = false)
                        }
                    }
                }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isConnected) { //solo se muestra si hay conexión
                FloatingActionButton(
                    onClick = { navController.navigate("add_recipe") },
                    containerColor = Cherry,
                    modifier = Modifier
                        .padding(bottom = 80.dp) // padding debajo para q no quede opacado por la bottom bar
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Recipe", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->

        val recetas = if (isConnected) recetasUser else recetasRoomUser
        val txtEmpty =  if (isConnected) "Add your first recipe!" else
        "Wait until you have internet to add your first recipe."

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .padding(bottom = 16.dp)
        ) {
            if (recetas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(txtEmpty, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center )
                }
            } else {
                // Carrusel de recetas
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 90.dp) // Respetar espacio FloatingButton
                ) {
                    items(recetas) { receta ->
                        Log.d("MyRecipesScreen", "deUser: ${receta.userId}")
                        RecetaCard(receta, navController, usersViewModel)
                    }
                    //estado de carga
                    if (isLoadingMore) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                }
            }
        }
    }
}




