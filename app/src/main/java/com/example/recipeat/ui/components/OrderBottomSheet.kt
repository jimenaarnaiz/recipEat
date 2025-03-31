package com.example.recipeat.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderBottomSheet(
    onDismiss: () -> Unit,
    recetasViewModel: RecetasViewModel,
    busquedaPorNombre: Boolean //Para controlar la visibilidad del filtro
) {
    val sheetState = rememberModalBottomSheetState()
    val selectedOrder = rememberSaveable { mutableStateOf("Default") }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Order By", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de selección
            OrderButton("Time", Icons.Filled.Timer,selectedOrder.value == "Time") {
                selectedOrder.value = "Time"
            }
            OrderButton("Number of Ingredients", Icons.Filled.RestaurantMenu,selectedOrder.value == "Number of Ingredients") {
                selectedOrder.value = "Number of Ingredients"
            }
            OrderButton("Alphabetical", Icons.Filled.SortByAlpha,selectedOrder.value == "Alphabetical") {
                selectedOrder.value = "Alphabetical"
            }

            if (busquedaPorNombre) {
                // Agregar nuevas opciones
                OrderButton("Recent (Asc)", Icons.Filled.Timer, selectedOrder.value == "Recent Asc") {
                    selectedOrder.value = "Recent Asc"
                }

                OrderButton("Recent (Desc)", Icons.Filled.Timer, selectedOrder.value == "Recent Desc") {
                    selectedOrder.value = "Recent Desc"
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    selectedOrder.value = "Default" // Restablecer al valor predeterminado
                }) {
                    Text("Reset order")
                }

                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                    // Aplicar el orden al ViewModel
                    recetasViewModel.ordenarResultados(selectedOrder.value)
                    onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cherry,
                        contentColor = Color.White
                    )
                ) {
                    Text("Apply order")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Composable para los botones de selección
@Composable
fun OrderButton(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) LightYellow else Color.LightGray,
            contentColor = Color.Black

        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.padding(end = 8.dp))
        Text(text)
    }
}
