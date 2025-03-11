package com.example.recipeat.ui.screens

import com.example.recipeat.ui.viewmodels.RecetasViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.IngredientesViewModel


@Composable
fun IngredientsSearchScreen(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel
) {

    // Obtener las recetas e ingredientes desde el ViewModel
    val recetas by recetasViewModel.apiRecetas.collectAsState()
    val ingredientesSugeridos by ingredientesViewModel.ingredientesSugeridos.collectAsState()
    val ingredientesReceta by ingredientesViewModel.ingredientes.collectAsState()

    var ingrediente by remember { mutableStateOf("") } // ingrediente a buscar


    // Cuando cambia el nombre del ingrediente, busca los ingredientes
    LaunchedEffect(ingrediente) {
        if (ingrediente.isNotEmpty()) { // Agregar una verificación para no hacer la búsqueda cuando la cadena esté vacía
            ingredientesViewModel.buscarIngredientes(ingrediente)
        }
    }

   Column(modifier = Modifier
       .fillMaxSize()
       .padding(16.dp)) {
        TextField(
            value = ingrediente,
            onValueChange = { ingrediente = it },
            label = { Text("Pe: Lettuce", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        if (ingredientesReceta.isNotEmpty()) {
            // Mostrar los ingredientes que coincidan con la búsqueda
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ingredientesReceta) { ingrediente ->
                    Column(
                        modifier = Modifier
                            .clickable { }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally


                    ) {
                        AsyncImage(
                            model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                            contentDescription = ingrediente.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(
                                    onClick = {
                                        ingredientesViewModel.removeIngredient(ingrediente)
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Mostrar los ingredientes que coincidan con la búsqueda
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ingredientesSugeridos) { ingrediente ->
                Column(
                    modifier = Modifier
                        .clickable { }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally


                ) {
                    AsyncImage(
                        model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                        contentDescription = ingrediente.name,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(
                                onClick = {
                                    if (!ingredientesReceta.contains(ingrediente) && ingredientesReceta.size < 6)
                                        ingredientesViewModel.addIngredient(ingrediente)
                                }
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = ingrediente.name,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                recetasViewModel.buscarRecetasPorIngredientes(ingrediente)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightYellow,
                contentColor = Color.Black
            ),
            enabled = ingredientesReceta.isNotEmpty()
        ) {
            Text(
                text = "Cook!",
                style = MaterialTheme.typography.bodyMedium
            )
        }


//        LazyColumn(modifier = Modifier
//            .fillMaxSize()
//            .padding(top = 16.dp)) {
//            items(recetas) { receta ->
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(8.dp),
//                ) {
//                    Column(modifier = Modifier.padding(8.dp)) {
//                        Image(
//                            painter = rememberAsyncImagePainter(receta.image),
//                            contentDescription = receta.title,
//                            modifier = Modifier
//                                .height(150.dp)
//                                .fillMaxWidth()
//                        )
//                        Text(receta.title, style = MaterialTheme.typography.titleLarge)
//
//                        // Mostrar cuántos ingredientes faltan
//                        Text(
//                            text = "Faltan ${receta.missedIngredientCount} ingredientes",
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//
//                        // Mostrar los nombres de los ingredientes faltantes
//                        if (receta.missedIngredients?.isNotEmpty() == true) {
//                            Text(
//                                text = "Ingredientes faltantes: ${
//                                    receta.missedIngredients.joinToString(
//                                        ", "
//                                    ) { it.name }
//                                }",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }
//                }
//            }
//        }
    }
}