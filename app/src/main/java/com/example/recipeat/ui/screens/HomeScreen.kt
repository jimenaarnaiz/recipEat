package com.example.recipeat.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.recipeat.ui.components.RecetaCard
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.PermissionsViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
    recetasViewModel: RecetasViewModel,
    roomViewModel: RoomViewModel,
    connectivityViewModel: ConnectivityViewModel,
    permissionsViewModel: PermissionsViewModel
) {
    val recetasState by recetasViewModel.recetasHome.observeAsState(emptyList())
    val isLoadingMore by recetasViewModel.isLoadingMore.observeAsState(false)
    val recetasHomeRoom by roomViewModel.homeRecipesRoom.observeAsState(emptyList())

    var username by rememberSaveable { mutableStateOf<String?>(null) }
    val uid = usersViewModel.getUidValue()
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    val lifecycleOwner = LocalLifecycleOwner.current

    // Observamos cambios en el ciclo de vida
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Cuando vuelve la pantalla, actualiza el estado del permiso de fotos
                permissionsViewModel.checkStoragePermission(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    // Cargar recetas al iniciar la pantalla
    LaunchedEffect(Unit) {
        if (username.isNullOrBlank()) {
            Log.d("HomeScreen", "launched effect uid ejecutado $uid")
            usersViewModel.obtenerUsername { nombre -> username = nombre }
        }

        //if (recetasState.isEmpty()) {
            recetasViewModel.obtenerRecetasHome(limpiarLista = true, uid.toString())
        //}
    }


    LaunchedEffect(recetasState) {
        //el segundo isEmpty hace que solo cargue al abrir la app las recetas favs,
        if (recetasState.isNotEmpty() && recetasHomeRoom.isEmpty()) {
            roomViewModel.guardarPrimeras15RecetasSiNoEstan(context, recetasState, uid.toString())
            roomViewModel.getRecetasHome(context, uid.toString())
        }
    }


    // Detectar cuando el usuario está cerca del final de la lista
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                val totalItemsCount = recetasState.size
                if (lastVisibleItemIndex != null && totalItemsCount >= 15) { // Para evitar llamadas innecesarias
                    val umbral = totalItemsCount - 5 // Cargar más cuando queden 5 recetas visibles
                    if (lastVisibleItemIndex >= umbral && !isLoadingMore) {
                        Log.d("HomeScreen", "Cargando más recetas...")
                        recetasViewModel.obtenerRecetasHome(limpiarLista = false, uid.toString())
                    }
                }
            }
    }

    // Interceptar el botón de "Atrás" para salir de la aplicación
    BackHandler {
        val activity = context as? Activity
        activity?.finish() // Cerrar la actividad y salir de la app
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 65.dp) //para bottom nav bar
    ) {
        Text(text = "Welcome, $username!", modifier = Modifier.padding(16.dp))

        val txtSearch = if (isConnected) "Search for recipes..." else "Search unavailable, no internet"
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { isActive = false },
            active = isActive,
            enabled = isConnected,
            onActiveChange = { isActive = it; if (it) navController.navigate("search") },
            placeholder = { Text(txtSearch) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {}

        if (isConnected) {
            if (recetasState.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {

                    items(recetasState) { receta ->
                        RecetaCard(receta = receta, navController, usersViewModel)
                    }

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
        }else {
            if (recetasHomeRoom.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center // Centrar horizontal y verticalmente
                ) {
                    Text(
                        "No internet. Please check your connection."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(recetasHomeRoom) { receta ->
                        RecetaCard(receta = receta, navController, usersViewModel)
                    }
                }
            }
        }
    }
}


