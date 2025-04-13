package com.example.recipeat.ui.screens.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.recipeat.R
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.IngredientesViewModel


@Composable
fun IngredientsSearch(
    navController: NavController,
    ingredientesViewModel: IngredientesViewModel,
    isConnected: Boolean
) {

    // Obtener los ingredientes desde el ViewModel
    val ingredientesSugeridos by ingredientesViewModel.ingredientesSugeridos.collectAsState()
    val ingredientesReceta by ingredientesViewModel.ingredientes.collectAsState()

    var ingredienteBusqueda by remember { mutableStateOf("") } // ingrediente a buscar


    // Cuando cambia el nombre del ingrediente, busca los ingredientes
    LaunchedEffect(ingredienteBusqueda) {
        if (ingredienteBusqueda.isNotBlank()) { // Agregar una verificación para no hacer la búsqueda cuando la cadena esté vacía
            ingredientesViewModel.buscarIngredientes(ingredienteBusqueda)
        }else{
            ingredientesViewModel.clearIngredientesSugeridos()
        }
    }

    val txtSearch = if (isConnected) "E.g.: apple" else "Search unavailable, no internet"
   Column(modifier = Modifier
       .fillMaxSize()
       .padding(16.dp)) {

        TextField(
            value = ingredienteBusqueda,
            onValueChange = { ingredienteBusqueda = it.lowercase() },
            label = { Text(txtSearch, style = MaterialTheme.typography.bodyMedium) },
            enabled = isConnected,
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

        if (ingredientesReceta.isNotEmpty()) {
            // Mostrar los ingredientes que quiero buscar
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
                        if (ingrediente.image.isNotBlank()){
                            AsyncImage(
                                model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                                contentDescription = ingrediente.name,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(
                                        enabled = isConnected,
                                        onClick = {
                                            ingredientesViewModel.removeIngredient(ingrediente)
                                        }
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }else {
                            Image(
                                painter = painterResource(id = R.drawable.ingredient_placeholder),
                                contentDescription = ingrediente.name,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(
                                        enabled = isConnected,
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
                    if (ingrediente.image.isNotBlank()) {
                        AsyncImage(
                            model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                            contentDescription = ingrediente.name,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(
                                    onClick = { //solo deja 5 ingredientes por búsqueda
                                        if (!ingredientesReceta.contains(ingrediente) && ingredientesReceta.size < 5) {
                                            ingredientesViewModel.addIngredient(ingrediente)
                                            //resetear para no tener q borrar el ing tú mismo
                                            ingredienteBusqueda = ""
                                        }

                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }else {
                        Image(
                            painter = painterResource(id = R.drawable.ingredient_placeholder),
                            contentDescription = ingrediente.name,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(
                                    onClick = {
                                        if (!ingredientesReceta.contains(ingrediente) && ingredientesReceta.size < 5) {
                                            //resetear para no tener q borrar el ing tú mismo
                                            ingredientesViewModel.addIngredient(ingrediente)
                                            ingredienteBusqueda = ""
                                        }
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }

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
                navController.navigate("resultadosIngredientes")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightYellow,
                contentColor = Color.Black
            ),
            enabled = ingredientesReceta.isNotEmpty() && isConnected
        ) {
            Text(
                text = "Cook!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }



}