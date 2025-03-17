package com.example.recipeat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recipeat.data.model.DishTypes
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltroBottomSheet(
    onDismiss: () -> Unit,
    onApplyFilters: (Int?, Int?, Int?, Int?, String?) -> Unit,
    recetasViewModel: RecetasViewModel,
    filtrosViewModel: FiltrosViewModel // Pasamos el ViewModel
) {
    // Inicializamos los filtros con los valores actuales del ViewModel
    var maxTiempo by remember { mutableStateOf(filtrosViewModel.maxTiempo.value) }
    var maxIngredientesFiltro by remember { mutableStateOf(filtrosViewModel.maxIngredientes.value) }
    var maxFaltantesFiltro by remember { mutableStateOf(filtrosViewModel.maxFaltantes.value) }
    var maxPasosFiltro by remember { mutableStateOf(filtrosViewModel.maxPasos.value) }
    var tipoPlatoFiltro by remember { mutableStateOf(filtrosViewModel.tipoPlato.value) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val opcionesTiempo = listOf(5, 10, 20, 30, 60)  // Ejemplo de opciones de tiempo en minutos
    val opcionesIngredientes = listOf(5, 10, 15, 20)
    val opcionesFaltantes = listOf(0, 2, 5, 10)
    val opcionesPasos = listOf(3, 5, 10, 15)

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Aplicar Filtros", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(12.dp))

            // Filtro de tiempo
            Text("⏳ Tiempo máximo")
            SegmentedButtonRow(opcionesTiempo, maxTiempo) { maxTiempo = it }

            // Filtro de ingredientes
            Text("🍽️ Máximo de ingredientes")
            SegmentedButtonRow(opcionesIngredientes, maxIngredientesFiltro) { maxIngredientesFiltro = it }

            // Filtro de ingredientes faltantes
            Text("🚫 Máximo de ingredientes faltantes")
            SegmentedButtonRow(opcionesFaltantes, maxFaltantesFiltro) { maxFaltantesFiltro = it }

            // Filtro de pasos
            Text("📋 Máximo de pasos")
            SegmentedButtonRow(opcionesPasos, maxPasosFiltro) { maxPasosFiltro = it }

            // Filtro de tipo de plato
            Text("🍛 Tipo de plato")
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
                        label = { Text(opcion.toString()) }
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
                    Text("Restablecer filtros")
                }

                TextButton(onClick = { onDismiss() }) {
                    Text("Cancelar")
                }

                Button(onClick = {
                    // Aplicar filtros
                    filtrosViewModel.aplicarFiltros(
                        maxTiempo, maxIngredientesFiltro, maxFaltantesFiltro, maxPasosFiltro, tipoPlatoFiltro
                    )
                    onApplyFilters(maxTiempo, maxIngredientesFiltro, maxFaltantesFiltro, maxPasosFiltro, tipoPlatoFiltro)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Text("Aplicar filtros")
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
                label = { Text("$opcion") }
            )
        }
    }
}
