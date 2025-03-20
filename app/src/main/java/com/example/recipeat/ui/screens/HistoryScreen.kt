package com.example.recipeat.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.CalendarView
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.ZoneId
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
    Scaffold(
        topBar = {
            AppBar(
                title = "My Cook History",
                navController = navController,
                onBackPressed = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

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
                selectedDate = selectedDate,
                onDaySelected = { newSelectedDate -> selectedDate = newSelectedDate },
                recetasHistorial = recetasHistorial.value
            )

            // 📌 Filtrar recetas del día seleccionado
            val recetasDelDia = recetasHistorial.value.filter {
                val recetaDate = it.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                recetaDate == selectedDate
            }

            if (recetasDelDia.isEmpty()) {
                Text(
                    text = "No recipes for this date",
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(recetasDelDia) { receta ->
                        RecetaItem(receta = receta, navController = navController)
                    }
                }
            }
        }
    }
}
