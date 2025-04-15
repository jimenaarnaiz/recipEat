package com.example.recipeat.ui.screens.search

import com.example.recipeat.ui.viewmodels.RecetasViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipeat.ui.components.SearchButton
import com.example.recipeat.ui.components.SearchTextField

@Composable
fun NameSearch(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    isConnected: Boolean
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

    val txtSearch = if (isConnected) "E.g.: Burger..." else "Search unavailable, no internet"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Hacer scroll posible para tdo el contenido
            .padding(16.dp)
    ) {
        // TextField para la búsqueda de recetas
        SearchTextField(
            value = recetaInput.text,
            onValueChange = { recetaInput = TextFieldValue(it) },
            label = txtSearch,
            isEnabled = isConnected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de búsqueda
        SearchButton(
            onClick = { navController.navigate("resultados/${recetaInput.text}") },
            isEnabled = isConnected && recetaInput.text.isNotBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (recetaInput.text.isNotBlank()) {
            // Lista de recetas sugeridas
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recetasSugeridas.forEach { recetaSug ->
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                val esDeUser = false //porque es de nameSearch
                                if (isConnected) navController.navigate("detalles/${recetaSug.id}/$esDeUser")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recetaSug.titulo,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            color = if (!isConnected) Color.LightGray else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
