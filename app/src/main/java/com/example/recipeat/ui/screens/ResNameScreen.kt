package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.recipeat.ui.components.RecetaCard
import com.example.recipeat.ui.components.RecetasScreenWrapper
import com.example.recipeat.ui.components.SinConexionTexto
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel

@Composable
fun ResNameScreen(
    nombreReceta: String,
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    filtrosViewModel: FiltrosViewModel,
    usersViewModel: UsersViewModel,
    connectivityViewModel: ConnectivityViewModel
) {

    val userId = usersViewModel.getUidValue()
    val recetas by recetasViewModel.recetas.observeAsState(emptyList())

    var showBottomSheet by remember { mutableStateOf(false) }
    var showOrderBottomSheet by remember { mutableStateOf(false) }

    // Estado para almacenar los ingredientes anteriores y verificar si hay cambios
    var lastName by rememberSaveable { mutableStateOf("") }

    val isLoading by recetasViewModel.isLoading.observeAsState(false)

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    // Función que busca recetas mientras se escribe en el input
    LaunchedEffect(nombreReceta) {
        // Solo ejecutar si ha cambiado realmente
        if (nombreReceta != lastName) {
            Log.d("ResNameSearch", "name a buscar: $nombreReceta")
            recetasViewModel.obtenerRecetasPorNombre(nombreReceta, userId.toString())
            lastName = nombreReceta // Actualiza el estado
        }
    }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            showBottomSheet = false
            showOrderBottomSheet = false
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


