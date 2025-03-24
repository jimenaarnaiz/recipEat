package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun FavoritesScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    // Obtener las recetas favoritas del usuario desde el ViewModel
    val favoritas = recetasViewModel.recetasFavoritas.observeAsState(emptyList())
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    // Estado para almacenar los ingredientes anteriores y verificar si hay cambios
    var lastFavs by rememberSaveable { mutableStateOf<List<RecetaSimple>>(emptyList()) }


    LaunchedEffect(navController) {
        Log.d("FavoritesScreen", "recetas favs actuales $uid: ${favoritas.value}")
        if (favoritas != lastFavs) {
            recetasViewModel.obtenerRecetasFavoritas(uid.toString())
            lastFavs = favoritas.value
        }
    }
    Log.d("FavoritesScreen", "fuera: recetas favs actuales: ${favoritas.value}")

    Scaffold(
        topBar = {
            AppBar(
                title = "My Favorites",
                navController = navController,
                onBackPressed = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        // Si no hay recetas favoritas, mostrar un mensaje
        if (favoritas.value.isEmpty()) {
            Text(
                text = "Start exploring and saving your favorites!",
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        } else {
            // Mostrar las recetas favoritas en un Grid de 2 columnas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Define el número de columnas
                contentPadding = PaddingValues(16.dp), // Añadir un margen alrededor del Grid
                modifier = Modifier.padding(paddingValues)
            ) {
                items(favoritas.value) { receta ->
                    RecetaItem(receta = receta, navController = navController)
                }
            }
        }
    }
}

@Composable
fun RecetaItem(receta: RecetaSimple, navController: NavHostController) {
    val esDeUser = receta.uid.isNotEmpty()

    // Este Composable es el que muestra cada receta en el Grid
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(4.dp)
            .clickable {
                navController.navigate("detalles/${receta.id}/$esDeUser")
            },
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cargar la imagen de la receta con esquinas redondeadas y sin padding
            var imagen by remember { mutableStateOf("") }
            imagen = receta.image.ifBlank {
                "android.resource://com.example.recipeat/${R.drawable.food_placeholder}"
            }
            Image(
                painter = rememberAsyncImagePainter(imagen),
                contentDescription = receta.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop // Asegurarse de que la imagen se recorte y llene el espacio

            )
            Text(
                text = receta.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp),
                maxLines = 1, // Limitar el texto a una sola línea
                overflow = TextOverflow.Ellipsis // Truncar el texto si es muy largo
            )
        }
    }
}

