package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@Composable
fun ResIngredientsScreen(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel
) {

    val ingredientes by ingredientesViewModel.ingredientes.collectAsState(emptyList())
    val recetas by recetasViewModel.recetas.observeAsState(emptyList())

    var mostrarDialog by remember { mutableStateOf(false) }



    // Función que busca recetas mientras se escribe en el input
    LaunchedEffect(ingredientes) {
        Log.d("ResultadosIngredientsSearch", "ingredientes a buscar: $ingredientes")
        // Obtener la lista de nombres de los ingredientes
        //val ingredientesNombres = ingredientes.map { it.name }
        recetasViewModel.buscarRecetasPorIngredientes(ingredientes)
    }

    Scaffold(
        topBar = {
            AppBar(
                title = "",
                navController = navController
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
            // Mostrar un indicador de carga si no se han cargado las recetas
            if (recetas.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                // Carrusel de recetas
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp) // Agregar espacio al final de la lista
                ) {

                    item{
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp), // Espaciado entre los botones
                            verticalAlignment = Alignment.CenterVertically, // Alineación vertical
                            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
                        ) {
                            // Botón de Filtros
                            Button(
                                onClick = { mostrarDialog = true },
                                modifier = Modifier.weight(1f), // Para que los botones ocupen el mismo espacio
                                shape = RoundedCornerShape(12.dp), // Bordes redondeados
                                colors = ButtonDefaults.buttonColors(containerColor = Cherry)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filtros", modifier = Modifier.padding(end = 8.dp))
                                Text("Filter by", style = MaterialTheme.typography.bodyMedium)
                            }

                            // Botón de Ordenar
                            Button(
                                onClick = { /* TODO: acción de ordenar */ },
                                modifier = Modifier.weight(1f), // Para que los botones ocupen el mismo espacio
                                shape = RoundedCornerShape(12.dp), // Bordes redondeados
                                colors = ButtonDefaults.buttonColors(containerColor = Cherry)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar", modifier = Modifier.padding(end = 8.dp))
                                Text("Order by", style = MaterialTheme.typography.bodyMedium)
                            }
                        }


                        // Mostrar el dialog de filtros si es necesario
                        if (mostrarDialog) {
                            FiltroDialog(
                                onDismiss = { mostrarDialog = false },
                                onApplyFilters = { maxIngredientes, maxFaltantes, maxPasos, tipoPlato ->
                                    // Aplica los filtros a las recetas
                                    recetasViewModel.filtrarRecetas(
                                        tiempoFiltro = 30, // ejemplo de valor para el filtro de tiempo
                                        maxIngredientesFiltro = maxIngredientes,
                                        maxFaltantesFiltro = maxFaltantes,
                                        maxPasosFiltro = maxPasos,
                                        tipoPlatoFiltro = tipoPlato
                                    )
                                    mostrarDialog = false
                                }
                            )
                        }
                    }

                    items(recetas) { receta ->
                        RecetaCardRes2(receta, navController, ingredientes)
                    }

                    // Cargar más recetas si el usuario está cerca del final
                    item {
                        if (recetas.isNotEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RecetaCardRes2(receta: Receta, navController: NavController, ingredientes: List<IngredienteSimple>) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Hace que la Card ocupe tdo el ancho disponible
            .padding(vertical = 8.dp) // Separación vertical entre las Cards
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable { navController.navigate("detalles/${receta.id}") },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // Alinear el contenido en el centro
        ) {
            // Cargar la imagen de la receta con esquinas redondeadas y sin padding
            Image(
                painter = rememberAsyncImagePainter(receta.image),
                contentDescription = receta.title,
                modifier = Modifier
                    .fillMaxWidth() // La imagen ocupa tdo el ancho de la Card
                    .height(220.dp) // Mantener la altura fija
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    ),
                contentScale = ContentScale.FillWidth // Asegurarse de que la imagen se recorte y llene el espacio
            )

            // Título de la receta
            Text(
                text = receta.title,
                modifier = Modifier
                    .padding(top = 8.dp) // Padding entre la imagen y el texto
                    .padding(start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, // Limitar el texto a una sola línea
                overflow = TextOverflow.Ellipsis // Truncar el texto si es muy largo

            )

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, // Alineación vertical de los íconos y el texto
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Espaciado entre los íconos y los textos
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Ícono y texto para el tiempo
                    Icon(
                        imageVector = Icons.Default.AccessTimeFilled,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 1.dp) // Espaciado entre el ícono y el texto
                    )

                    Text(
                        text = receta.time.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Ícono y texto para el número de ingredientes usados
                    Icon(
                        imageVector = Icons.Default.ShoppingBasket,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 1.dp) // Espaciado entre el ícono y el texto
                    )

                    Text(
                        text = receta.usedIngredientCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Mostrar cuántos ingredientes faltan
                if (receta.missingIngredientCount > 0) {
                    Text(
                        text = "Faltan ${receta.missingIngredientCount} ingredientes",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )

//                    // Mostrar los nombres de los ingredientes faltantes
//                    Text(
//                        text = "${
//                            receta.unusedIngredients.joinToString(", ") { it.name }
//                        }",
//                        style = MaterialTheme.typography.bodySmall,
//                        maxLines = 1, // Limitar el texto a una sola línea
//                        overflow = TextOverflow.Ellipsis // Truncar el texto si es muy largo
//                    )
                }

                // Mostrar los ingredientes buscados por el usuario
                if (ingredientes.isNotEmpty()) {
                    // Mostrar las imágenes de los ingredientes buscados por el usuario en un Row
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ingredientes.forEach { ingrediente ->
                            val isUsedInRecipe = !(receta.unusedIngredients.contains(ingrediente))

                            if (ingrediente.image.isNotBlank()) {
                                AsyncImage(
                                    model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                                    contentDescription = ingrediente.name,
                                    modifier = Modifier
                                        .size(32.dp) // Tamaño de la imagen
                                        .clip(CircleShape) // Forma circular
                                        .then(
                                            if (isUsedInRecipe) Modifier.border(
                                                1.dp,
                                                Color.DarkGray,
                                                CircleShape
                                            ) else Modifier
                                        ),
                                    contentScale = ContentScale.Crop
                                )

                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ingredient_placeholder),
                                    contentDescription = ingrediente.name,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

