package com.example.recipeat.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.CalendarView
import com.example.recipeat.ui.components.RecetaSimpleCardItem
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    val recetasHistorial = recetasViewModel.recetasHistorial.observeAsState(emptyList())
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Estado para almacenar el mes seleccionado
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    // Formatear el nombre del mes y año
    val monthName = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    // Estado para cambiar entre 7 días y 30 días
    var viewMode by rememberSaveable { mutableIntStateOf(7) } // 7 días por defecto
    var rango by rememberSaveable { mutableIntStateOf(7) } // Variable para el rango de días

    // Estado para manejar la habilitación de los iconos
    var isPreviousMonthEnabled by rememberSaveable { mutableStateOf(true) }
    var isNextMonthEnabled by rememberSaveable { mutableStateOf(false) }

    // Función para cambiar al mes anterior
    fun goToPreviousMonth() {
        if (isPreviousMonthEnabled) {
            selectedDate = selectedDate.minusMonths(1)
            isPreviousMonthEnabled = false // Deshabilitar el icono de mes anterior
            isNextMonthEnabled = true // Habilitar el icono de mes siguiente
        }
    }

    // Función para cambiar al mes siguiente
    fun goToNextMonth() {
        if (isNextMonthEnabled) {
            selectedDate = selectedDate.plusMonths(1)
            isNextMonthEnabled = false // Deshabilitar el icono de mes siguiente
            isPreviousMonthEnabled = true // Habilitar el icono de mes anterior
        }
    }

    LaunchedEffect(rango) {
        recetasViewModel.obtenerRecetasPorRangoDeFecha(uid.toString(),
            rango
        )
        Log.d("HistoryScreen", "El rango ha cambiado")
    }


    Scaffold(
        topBar = {
            AppBar(
                title = "My Cook History",
                navController = navController,
                onBackPressed = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)


        ) {
            Button(
                onClick = {
                    // Alternar entre 7 días y 30 días
                    rango = if (rango == 7) 30 else 7
                    viewMode = if (rango == 7) 7 else 30
                },
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cherry,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (rango == 7) "View Last 30 Days" else "View Last 7 Days"
                )
            }

            // Barra superior con los iconos para navegar entre los meses
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono para el mes anterior
                IconButton(
                    onClick = { goToPreviousMonth() },
                    enabled = isPreviousMonthEnabled
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }

                // Nombre del mes
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                // Icono para el mes siguiente
                IconButton(
                    onClick = { goToNextMonth() },
                    enabled = isNextMonthEnabled
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }


            // Vista de calendario
            CalendarView(
                viewMode = viewMode,
                selectedDate = selectedDate,
                onDaySelected = { newSelectedDate -> selectedDate = newSelectedDate },
                recetasHistorial = recetasHistorial.value
            )

            // Filtrar recetas según los días seleccionados
            val filteredRecetas = recetasViewModel.filtrarRecetasPorDiaSelecc(selectedDate)

            if (filteredRecetas.isEmpty()) {
                Text(
                    text = "You haven't cooked on this day!",
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(filteredRecetas) { receta ->
                        RecetaSimpleCardItem(
                            id = receta.id,
                            title = receta.title,
                            image = receta.image,
                            navController = navController,
                            esDeUser = receta.userReceta.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}
