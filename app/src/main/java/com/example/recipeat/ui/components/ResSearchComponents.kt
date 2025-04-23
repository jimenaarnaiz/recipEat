package com.example.recipeat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@Composable
fun RecetasScreenWrapper(
    navController: NavController,
    isLoading: Boolean,
    recetas: List<Receta>,
    isConnected: Boolean,
    showBottomSheet: Boolean,
    showOrderBottomSheet: Boolean,
    onShowBottomSheetChange: (Boolean) -> Unit,
    onShowOrderBottomSheetChange: (Boolean) -> Unit,
    filtrosViewModel: FiltrosViewModel,
    recetasViewModel: RecetasViewModel,
    content: LazyListScope.() -> Unit,
    busquedaPorNombre: Boolean
) {

    //para controlar si mostrar los botones o no, ya q puede no haber resultados por haber filtrado
    var filtrosAplicados by rememberSaveable { mutableStateOf(false) }
    var ordenAplicado by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppBar(
                title = "",
                onBackPressed = {
                    filtrosViewModel.restablecerFiltros()
                    filtrosViewModel.restablecerOrden()
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .padding(bottom = 16.dp)
        ) {

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val shouldShowFilterAndOrder =
                    recetas.isNotEmpty() || filtrosAplicados || ordenAplicado

                if (shouldShowFilterAndOrder) {
                    FilterAndOrderButtons(
                        isConnected = isConnected,
                        onShowBottomSheetChange = onShowBottomSheetChange,
                        onShowOrderBottomSheetChange = onShowOrderBottomSheetChange
                    )
                }

                if (showOrderBottomSheet) {
                    ordenAplicado = true
                    OrderBottomSheet(
                        recetasViewModel = recetasViewModel,
                        busquedaMisRecetas = false,
                        onDismiss = { onShowOrderBottomSheetChange(false) },
                        filtrosViewModel = filtrosViewModel
                    )
                }

                if (showBottomSheet) {
                    filtrosAplicados = true
                    FiltroBottomSheet(
                        onDismiss = { onShowBottomSheetChange(false) },
                        onApplyFilters = { tiempo, ingredientes, faltantes, pasos, plato, dietas ->
                            filtrosAplicados = true
                            filtrosViewModel.aplicarFiltros(
                                tiempo, ingredientes, faltantes, pasos, plato, dietas
                            )
                            recetasViewModel.filtrarRecetas(
                                tiempo, ingredientes, faltantes, pasos, plato, dietas
                            )
                            onShowBottomSheetChange(false)
                        },
                        filtrosViewModel = filtrosViewModel,
                        recetasViewModel = recetasViewModel,
                        busquedaPorNombre = busquedaPorNombre
                    )
                }

                if (recetas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

    @Composable
fun FilterAndOrderButtons(
    isConnected: Boolean,
    onShowBottomSheetChange: (Boolean) -> Unit,
    onShowOrderBottomSheetChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            enabled = isConnected,
            onClick = { onShowBottomSheetChange(true) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cherry)
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtros", modifier = Modifier.padding(end = 8.dp))
            Text("Filter by", style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            enabled = isConnected,
            onClick = { onShowOrderBottomSheetChange(true) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cherry)
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar", modifier = Modifier.padding(end = 8.dp))
            Text("Order by", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SinConexionTexto() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No internet. Please check your connection.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

