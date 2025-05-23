package com.example.recipeat.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.recipeat.R
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShoppingListScreen(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
    planViewModel: PlanViewModel,
    connectivityViewModel: ConnectivityViewModel
) {

    val uid = usersViewModel.getUidValue()
    // Observar los ingredientes agrupados desde el ViewModel
    val ingredienteslistaCompraState = planViewModel.listaCompra.observeAsState(emptyList())
    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    Scaffold(
        topBar = {
            AppBar(
                "Shopping List", onBackPressed = { navController.popBackStack() },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            // Agrupar los ingredientes por el campo "aisle"
            val ingredientesAgrupadosAisle = ingredienteslistaCompraState.value.groupBy {
                when {
                    it.aisle.isEmpty() || it.aisle == "?" -> "Others"
                    else -> it.aisle
                }
            }

            LazyColumn {
                // Recorrer cada grupo de ingredientes agrupados por 'aisle'
                ingredientesAgrupadosAisle.forEach { (aisle, ingredientesDelAisle) ->

                    item {
                        // Mostrar el nombre del aisle como un encabezado
                        Text(
                            text = aisle,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Mostrar los ingredientes que pertenecen a ese aisle
                    items(ingredientesDelAisle) { ingrediente ->
                        // Usa el valor real del ingrediente para el estado del Checkbox
                        var checkedState = rememberSaveable { mutableStateOf(ingrediente.estaComprado) }
                        // Usamos Row para alinear la imagen y el Checkbox, y Column para los nombres y medidas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // Mostrar imagen del ingrediente
                            AsyncImage(
                                model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                                error = painterResource(id = R.drawable.ingredient_placeholder),
                                contentDescription = ingrediente.name,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Usamos Column para mostrar el nombre y las medidas debajo
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Mostrar nombre del ingrediente
                                Text(
                                    text = ingrediente.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                // Mostrar las medidas y cantidades del ingrediente justo debajo del nombre
                                Text(
                                    text = ingrediente.medidas.joinToString(" + ") {
                                    "${it.first} ${it.second}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp) // Espaciado entre nombre y medidas
                                )
                            }

                            // CheckBox para marcar el ingrediente como comprado
                            Checkbox(
                                enabled = isConnected,
                                checked = checkedState.value, // El estado del Checkbox se basa en el valor de checkedState
                                onCheckedChange = { checked ->
                                    checkedState.value = checked
                                    // Actualizar el estado en Firebase a través del ViewModel
                                    planViewModel.actualizarEstadoIngredienteEnFirebase(
                                        uid.toString(),
                                        ingrediente.name,
                                        checkedState.value
                                    )
                                }
                            )
                        }
                    }
                }
            }

        }
    }
}