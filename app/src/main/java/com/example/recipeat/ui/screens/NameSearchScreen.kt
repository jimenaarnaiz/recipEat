package com.example.recipeat.ui.screens

import com.example.recipeat.ui.viewmodels.RecetasViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
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
    val recetasSugeridas by recetasViewModel.apiRecetasSugeridas.collectAsState()
    var recetaInput by remember { mutableStateOf(TextFieldValue("")) }

    // Función que busca recetas mientras se escribe en el input
    LaunchedEffect(recetaInput.text) {
        if (recetaInput.text.isNotEmpty()) {
            recetasViewModel.buscarRecetasPorNombreAutocompletado(recetaInput.text)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TextField para la búsqueda de recetas
        TextField(
            value = recetaInput,
            onValueChange = { updatedValue ->
                recetaInput = updatedValue
            },  // Actualiza el valor del input
            label = { Text("Pe: Burger...", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de búsqueda
        Button(
            onClick = {
                navController.navigate("resultadosScreen/${recetaInput.text}")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = LightYellow,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
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
                            recetaInput = TextFieldValue(
                                recetaSug.title,
                                selection = TextRange(recetaSug.title.length) // Coloca el cursor al final del texto
                            )  // Al hacer clic, actualiza el input con el título de la receta seleccionada
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recetaSug.title,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
