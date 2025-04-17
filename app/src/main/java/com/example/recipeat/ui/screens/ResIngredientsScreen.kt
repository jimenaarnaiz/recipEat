package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.FiltroBottomSheet
import com.example.recipeat.ui.components.OrderBottomSheet
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.utils.NetworkConnectivityManager

@Composable
fun ResIngredientsScreen(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel,
    filtrosViewModel: FiltrosViewModel,
    usersViewModel: UsersViewModel,
    connectivityViewModel: ConnectivityViewModel
) {

    val uid = usersViewModel.getUidValue()
    val ingredientes by ingredientesViewModel.ingredientes.collectAsState(emptyList())
    val recetas by recetasViewModel.recetas.observeAsState(emptyList())

    var showBottomSheet by remember { mutableStateOf(false) }
    var showOrderBottomSheet by remember { mutableStateOf(false) }

    // Estado para almacenar los ingredientes anteriores y verificar si hay cambios
    var lastIngredientes by rememberSaveable { mutableStateOf<List<IngredienteSimple>>(emptyList()) }

    val isLoading by recetasViewModel.isLoading.observeAsState(false)

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    LaunchedEffect(ingredientes) {
        // Solo ejecutar si los ingredientes han cambiado
        if (ingredientes != lastIngredientes) {
            Log.d("ResultadosIngredientsSearch", "ingredientes a buscar: $ingredientes")
            recetasViewModel.buscarRecetasPorIngredientes(ingredientes, uid.toString())
            lastIngredientes = ingredientes // Actualiza el estado con los nuevos ingredientes
        }
    }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            showBottomSheet = false
            showOrderBottomSheet = false
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = "",
                navController = navController,
                onBackPressed = {
                    filtrosViewModel.restablecerFiltros()
                    filtrosViewModel.restablecerOrden()
                    navController.popBackStack()
                }
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

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                recetas.isEmpty() -> {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                // Carrusel de recetas
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp) // Agregar espacio al final de la lista
                ) {

                    item{
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp), // Espaciado entre los botones
                            verticalAlignment = Alignment.CenterVertically, // Alineación vertical
                            modifier = Modifier.fillMaxWidth() // Ocupa tdo el ancho disponible
                        ) {
                            // Botón de Filtros
                            Button(
                                enabled = isConnected,
                                onClick = { showBottomSheet = true },
                                modifier = Modifier.weight(1f), // Para que los botones ocupen el mismo espacio
                                shape = RoundedCornerShape(12.dp), // Bordes redondeados
                                colors = ButtonDefaults.buttonColors(containerColor = Cherry)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filtros", modifier = Modifier.padding(end = 8.dp))
                                Text("Filter by", style = MaterialTheme.typography.bodyMedium)
                            }

                            // Botón de Ordenar
                            Button(
                                enabled = isConnected,
                                onClick = { showOrderBottomSheet = true },
                                modifier = Modifier.weight(1f), // Para que los botones ocupen el mismo espacio
                                shape = RoundedCornerShape(12.dp), // Bordes redondeados
                                colors = ButtonDefaults.buttonColors(containerColor = Cherry)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar", modifier = Modifier.padding(end = 8.dp))
                                Text("Order by", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Mostrar el dialog de ordenar si es necesario
                        if (showOrderBottomSheet) {
                            OrderBottomSheet(
                                recetasViewModel = recetasViewModel,
                                busquedaMisRecetas = false,
                                onDismiss = { showOrderBottomSheet = false },
                                filtrosViewModel = filtrosViewModel
                            )
                        }

                        // Mostrar el dialog de filtros si es necesario
                        if (showBottomSheet) {
                            FiltroBottomSheet(
                                onDismiss = { showBottomSheet = false },
                                onApplyFilters = { maxTiempo, maxIngredientes, maxFaltantes, maxPasos, tipoPlato, tipoDieta ->
                                    // Aplicar los filtros seleccionados
                                    filtrosViewModel.aplicarFiltros(
                                        tiempo = maxTiempo,
                                        ingredientes = maxIngredientes,
                                        faltantes = maxFaltantes,
                                        pasos = maxPasos,
                                        plato = tipoPlato,
                                        dietas = tipoDieta
                                    )
                                    // Aplica los filtros a las recetas
                                    recetasViewModel.filtrarRecetas(
                                        tiempoFiltro = maxTiempo,
                                        maxIngredientesFiltro = maxIngredientes,
                                        maxFaltantesFiltro = maxFaltantes,
                                        maxPasosFiltro = maxPasos,
                                        tipoPlatoFiltro = tipoPlato,
                                        tipoDietaFiltro = tipoDieta
                                    )

                                    showBottomSheet = false

                                },
                                filtrosViewModel = filtrosViewModel,
                                recetasViewModel = recetasViewModel,
                                busquedaPorNombre = false,
                            )
                        }
                    }
                    if (isConnected) {
                        items(recetas) { receta ->
                            RecetaCardRes2(receta, navController, ingredientes)
                        }
                    }else{
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize(), // Usa tdo el espacio disponible dentro del LazyColumn
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No internet. Please check your connection.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}


@Composable
fun RecetaCardRes2(receta: Receta, navController: NavController, ingredientes: List<IngredienteSimple>) {

    val esDeUser = receta.userId.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth() // Hace que la Card ocupe tdo el ancho disponible
            .padding(vertical = 8.dp) // Separación vertical entre las Cards
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable { navController.navigate("detalles/${receta.id}/$esDeUser") },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // Alinear el contenido en el centro
        ) {
            // Cargar la imagen de la receta con esquinas redondeadas y sin padding
            var imagen by remember { mutableStateOf("") }
            imagen = if (receta.image?.isNotBlank() == true) {
                receta.image
            } else {
                "${R.drawable.food_placeholder}"
            }

            Image(
                painter = rememberAsyncImagePainter(
                    model = imagen,
                    error = painterResource(id = R.drawable.food_placeholder) // Imagen por defecto en caso de error
                ),
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

