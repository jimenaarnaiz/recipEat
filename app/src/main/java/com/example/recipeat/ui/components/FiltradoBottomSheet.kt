package com.example.recipeat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recipeat.data.model.DishTypes
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltroBottomSheet(
    onDismiss: () -> Unit,
    onApplyFilters: (Int?, Int?, Int?, Int?, String?, Set<String>?) -> Unit,
    recetasViewModel: RecetasViewModel,
    filtrosViewModel: FiltrosViewModel, // Pasamos el ViewModel
    busquedaPorNombre: Boolean //Para controlar la visibilidad del filtro
) {
    // Inicializamos los filtros con los valores actuales del ViewModel
    var maxTiempo by remember { mutableStateOf(filtrosViewModel.maxTiempo.value) }
    var maxIngredientesFiltro by remember { mutableStateOf(filtrosViewModel.maxIngredientes.value) }
    var maxFaltantesFiltro by remember { mutableStateOf(filtrosViewModel.maxFaltantes.value) }
    var maxPasosFiltro by remember { mutableStateOf(filtrosViewModel.maxPasos.value) }
    var tipoPlatoFiltro by remember { mutableStateOf(filtrosViewModel.tipoPlato.value) }
    var tipoDietaFiltro by remember { mutableStateOf(filtrosViewModel.tipoDieta.value ?: emptySet()) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val opcionesTiempo = listOf(10, 20, 30, 45, 60)  // Opciones de tiempo en minutos
    val opcionesIngredientes = listOf(5, 7, 10, 15 )
    val opcionesFaltantes = listOf(0, 2, 5, 10)
    val opcionesPasos = listOf(3, 5, 10, 15)
    val opcionesDieta = listOf("Gluten-Free", "Vegan", "Vegetarian")

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Apply filters", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(12.dp))

            // Filtro de tiempo
            Text("⏳ Max. Time")
            SegmentedButtonRow(opcionesTiempo, maxTiempo) { maxTiempo = it }

            // Filtro de ingredientes
            Text("🍽️ Max. Used Ingredients")
            SegmentedButtonRow(opcionesIngredientes, maxIngredientesFiltro) { maxIngredientesFiltro = it }

            // Filtro de ingredientes faltantes
            if (!busquedaPorNombre) {
                Text("🚫 Max. Missing Ingredients")
                SegmentedButtonRow(opcionesFaltantes, maxFaltantesFiltro) { maxFaltantesFiltro = it }
            }

            // Filtro de pasos
            Text("📋 Max Steps")
            SegmentedButtonRow(opcionesPasos, maxPasosFiltro) { maxPasosFiltro = it }

            // Filtro de tipo de plato
            Text("🍛 Dish Type")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DishTypes.entries.forEach { opcion ->
                    FilterChip(
                        selected = tipoPlatoFiltro == opcion.toString(),
                        onClick = {
                            tipoPlatoFiltro = if (tipoPlatoFiltro == opcion.toString()) null else opcion.toString()
                        },
                        label = { Text(opcion.toString()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LightYellow, // fondo cuando está seleccionado
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = Color.Black //texto
                        )
                    )
                }
            }

            Text("Dietary Preferences")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opcionesDieta.forEach { opcion ->
                    FilterChip(
                        selected = tipoDietaFiltro.contains(opcion), // Verifica si está seleccionado
                        onClick = {
                            tipoDietaFiltro = if (tipoDietaFiltro.contains(opcion)) {
                                tipoDietaFiltro - opcion // Si ya estaba, lo quita
                            } else {
                                tipoDietaFiltro + opcion // Si no estaba, lo agrega
                            }
                        },
                        label = {
                            Text(opcion)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LightYellow, // fondo cuando está seleccionado
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (tipoDietaFiltro.contains(opcion)) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    // Restablecer filtros y recetas
                    filtrosViewModel.restablecerFiltros()
                    recetasViewModel.restablecerRecetas()
                    onDismiss()
                }) {
                    Text("Reset filters")
                }

                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                    // Aplicar filtros
                    filtrosViewModel.aplicarFiltros(
                        maxTiempo, maxIngredientesFiltro, maxFaltantesFiltro, maxPasosFiltro, tipoPlatoFiltro, tipoDietaFiltro
                    )
                    onApplyFilters(maxTiempo, maxIngredientesFiltro, maxFaltantesFiltro, maxPasosFiltro, tipoPlatoFiltro, tipoDietaFiltro)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cherry,
                        contentColor = Color.White
                    )
                ){
                    Text("Apply filters")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun SegmentedButtonRow(opciones: List<Int>, seleccionado: Int?, onSelectedChange: (Int?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionado,
                onClick = {
                    // Si la opción está seleccionada, desmarcarla (null)
                    if (opcion == seleccionado) {
                        onSelectedChange(null)
                    } else {
                        onSelectedChange(opcion)
                    }
                },
                label = {
                    Text(
                        "$opcion",
                        color = if (opcion == seleccionado) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LightYellow, // fondo cuando está seleccionado
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
        }
    }
}
