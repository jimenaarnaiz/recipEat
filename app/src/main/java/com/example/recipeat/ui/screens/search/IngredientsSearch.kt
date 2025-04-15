package com.example.recipeat.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.recipeat.R
import com.example.recipeat.ui.components.SearchButton
import com.example.recipeat.ui.components.SearchTextField
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

       SearchTextField(
           value = ingredienteBusqueda,
           onValueChange = { ingredienteBusqueda = it.lowercase() },
           label = txtSearch,
           isEnabled = isConnected
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
                        AsyncImage(
                            model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                            error = painterResource(R.drawable.ingredient_placeholder),
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
                    //imagen ingrediente
                    AsyncImage(
                        model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                        error = painterResource(R.drawable.ingredient_placeholder),
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
                    //nombre ingrediente
                    Text(
                        text = ingrediente.name,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

       SearchButton(
           onClick = { navController.navigate("resultadosIngredientes") },
           isEnabled = ingredientesReceta.isNotEmpty() && isConnected
       )
    }


}