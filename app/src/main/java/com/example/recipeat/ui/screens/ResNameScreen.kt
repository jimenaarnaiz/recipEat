package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.FiltroBottomSheet
import com.example.recipeat.ui.components.OrderBottomSheet
import com.example.recipeat.ui.components.RecetaCard
import com.example.recipeat.ui.components.RecetasScreenWrapper
import com.example.recipeat.ui.components.SinConexionTexto
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.utils.NetworkConnectivityManager

@Composable
fun ResNameScreen(
    nombreReceta: String,
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    filtrosViewModel: FiltrosViewModel,
    usersViewModel: UsersViewModel
) {

    val userId = usersViewModel.getUidValue()
    val recetas by recetasViewModel.recetas.observeAsState(emptyList())

    var showBottomSheet by remember { mutableStateOf(false) }
    var showOrderBottomSheet by remember { mutableStateOf(false) }

    // Estado para almacenar los ingredientes anteriores y verificar si hay cambios
    var lastName by rememberSaveable { mutableStateOf("") }

    val isLoading by recetasViewModel.isLoading.observeAsState(false)

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

    // Función que busca recetas mientras se escribe en el input
    LaunchedEffect(nombreReceta) {
        // Solo ejecutar si ha cambiado realmente
        if (nombreReceta != lastName) {
            Log.d("ResNameSearch", "name a buscar: $nombreReceta")
            recetasViewModel.obtenerRecetasPorNombre(nombreReceta, userId.toString())
            lastName = nombreReceta // Actualiza el estado
        }
    }

    RecetasScreenWrapper(
        navController = navController,
        isLoading = isLoading,
        recetas = recetas,
        isConnected = isConnected,
        showBottomSheet = showBottomSheet,
        showOrderBottomSheet = showOrderBottomSheet,
        onShowBottomSheetChange = { showBottomSheet = it },
        onShowOrderBottomSheetChange = { showOrderBottomSheet = it },
        filtrosViewModel = filtrosViewModel,
        recetasViewModel = recetasViewModel,
        busquedaPorNombre = true,
        content = {
            if (isConnected) {
                items(recetas) { receta ->
                    RecetaCard(receta, navController, usersViewModel)
                }
            } else {
                item {
                    SinConexionTexto()
                }
            }
        }
    )
}


