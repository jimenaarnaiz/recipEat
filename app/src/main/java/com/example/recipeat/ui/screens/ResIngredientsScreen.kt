package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.components.RecetasScreenWrapper
import com.example.recipeat.ui.components.SinConexionTexto
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R

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

    val isLoading by recetasViewModel.isLoading.observeAsState(false)
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    var showBottomSheet by remember { mutableStateOf(false) }
    var showOrderBottomSheet by remember { mutableStateOf(false) }

    var lastIngredientes by rememberSaveable { mutableStateOf<List<IngredienteSimple>>(emptyList()) }

    LaunchedEffect(ingredientes) {
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

    RecetasScreenWrapper(
        navController = navController,
        isLoading = isLoading,
        recetas = recetas,
        isConnected = isConnected,
        showBottomSheet = showBottomSheet,
        showOrderBottomSheet = showOrderBottomSheet,
        onShowBottomSheetChange = { showBottomSheet = it },
        onShowOrderBottomSheetChange = { showOrderBottomSheet = it },
        filtrosViewModel = filtrosViewModel,
        recetasViewModel = recetasViewModel,
        busquedaPorNombre = false,
        content = {
            if (isConnected) {
                items(recetas) { receta ->
                    RecetaCardRes2(
                        receta = receta,
                        navController = navController,
                        ingredientes = ingredientes
                    )
                }
            } else {
                item {
                    SinConexionTexto()
                }
            }
        }
    )
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
                        text = "${receta.missingIngredientCount} missing ingredients",
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
