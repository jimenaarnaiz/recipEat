package com.example.recipeat.ui.screens

import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.ui.components.RecetaCard
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.utils.NetworkConnectivityManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    val usersViewModel = UsersViewModel()

    // Observamos las recetas desde el ViewModel
    val recetasState by recetasViewModel.recetas.observeAsState(emptyList())

    var username by remember { mutableStateOf<String?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    // Detectar si el usuario ha llegado cerca del final de la lista
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val isLoadingMore by recetasViewModel.isLoadingMore.observeAsState(false) // Estado de carga adicional

    // Instanciar el NetworkConnectivityManager
    val context = LocalContext.current
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }

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

    // Verificar si hay conexión y ajustar el ícono de favoritos
    val isConnected = networkConnectivityManager.isConnected.value


    LaunchedEffect(username) {
        uid?.let {
            recetasViewModel.obtenerRecetasHome(it, limpiarLista = true) // Primera carga

            usersViewModel.obtenerUsername(it) { nombre ->
                username = nombre
            }

            Log.d("HomeScreen", "Recetas inicial: ${recetasState.size}")
        }


        //121 recetas de momento
        val db = Firebase.firestore

        db.collection("recetas")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val count = querySnapshot.size() // Número de documentos
                Log.d("Firebase", "Número de recetas: $count")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error obteniendo recetas", e)
            }
    }

    // TODO Detecta si el usuario está cerca del final de la lista
    LaunchedEffect(listState) {
//        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
//            .collect { index ->
//                Log.d("HomeScreen", "Índice visible actual: $index")
//                if (index == recetasState.size - 1 && !isLoadingMore) {
//                    Log.d("HomeScreen", "Cargando más recetas")
//                    uid?.let {
//                        recetasViewModel.obtenerRecetasHome(it, limpiarLista = false)
//                        Log.d("HomeScreen", "Num de recetas act: ${recetasState.size}")
//                    }
//                }
//            }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        Text(
            text = "Welcome, $username!",
            modifier = Modifier
                .padding(16.dp)
        )

         val txtSearch = if (isConnected) "Search for recipes..." else "Search unavailable, no internet"

        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {
                isActive = false
            },
            active = isActive,
            enabled = isConnected,
            onActiveChange = { isActive = it; if (it) navController.navigate("search") },
            placeholder = { Text(txtSearch) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            //
        }

        // Mostrar un indicador de carga si no se han cargado las recetas
        if (recetasState.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // Carrusel de recetas
            LazyColumn(
                state = listState, // Vincular el estado de la lista
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // Asegúrate de que no se solapen con el BottomBar
            ) {
                items(recetasState) { receta ->
                    RecetaCard(receta = receta, navController)
                }

                // Indicador de carga al final de la lista
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

