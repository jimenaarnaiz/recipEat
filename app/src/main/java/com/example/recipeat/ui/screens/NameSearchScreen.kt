package com.example.recipeat.ui.screens

import com.example.recipeat.ui.viewmodels.RecetasViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipeat.ui.theme.LightYellow

@Composable
fun NameSearchScreen(
    navController: NavController,
    recetasViewModel: RecetasViewModel
) {
    val recetasSugeridas by recetasViewModel.recetasSugeridas.collectAsState()
    var recetaInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    // Función que busca recetas mientras se escribe en el input
    LaunchedEffect(recetaInput) {
        if (recetaInput.text.isNotEmpty()) {
            recetasViewModel.obtenerSugerenciasPorNombre(recetaInput.text)
        }
    }
        Column(
            modifier = Modifier
                .fillMaxSize()
                //.padding(paddingValues)
                .padding(16.dp)
        ) {
            // TextField para la búsqueda de recetas
            TextField(
                value = recetaInput,
                onValueChange = { updatedValue ->
                    recetaInput = updatedValue
                },  // Actualiza el valor del input
                label = { Text("E.g.: Burger...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, // Aquí se usa el ícono de lupa
                        contentDescription = "Search Icon",
                        tint = Color.Gray // Puedes cambiar el color si lo deseas
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de búsqueda
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = {
                    navController.navigate("resultados/${recetaInput.text}")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightYellow,
                    contentColor = Color.Black
                ),
                //enabled = false
            ) {

                Text(
                    text = "Cook!",
                    style = MaterialTheme.typography.bodyMedium
                )

            }

            Spacer(modifier = Modifier.height(8.dp))


            // Lista de recetas sugeridas
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recetasSugeridas) { recetaSug ->
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                               navController.navigate("detalles/${recetaSug.id}")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recetaSug.titulo,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
//}
