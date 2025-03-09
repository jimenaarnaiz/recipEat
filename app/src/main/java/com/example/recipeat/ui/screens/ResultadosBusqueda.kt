package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.data.model.ApiReceta
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@Composable
fun ResultadosScreen(
    nombreReceta: String,
    navController: NavController,
    recetasViewModel: RecetasViewModel
) {

    val recetas by recetasViewModel.apiRecetas.collectAsState(emptyList())

    // Función que busca recetas mientras se escribe en el input
    LaunchedEffect(nombreReceta) {
            recetasViewModel.buscarRecetasPorNombre(nombreReceta)
        Log.d("ResultadosBusqueda", "BIEN")
    }
    Log.d("ResultadosBusqueda", "recetas res: ${recetas}")

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                items(recetas) { receta ->
                    RecetaCard2(receta)
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

@Composable
fun RecetaCard2(receta: ApiReceta) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Hace que la Card ocupe tdo el ancho disponible
            .padding(vertical = 8.dp) // Separación vertical entre las Cards
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        //elevation = 4.dp // Sombra para un toque profesional
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
                contentScale = ContentScale.Crop // Asegurarse de que la imagen se recorte y llene el espacio
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
//                    // Ícono y texto para el tiempo
//                    Icon(
//                        imageVector = Icons.Default.AccessTimeFilled,
//                        contentDescription = null,
//                        modifier = Modifier.padding(end = 1.dp) // Espaciado entre el ícono y el texto
//                    )
//
//                    Text(
//                        text = receta.readyInMinutes.toString(),
//                        style = MaterialTheme.typography.bodyMedium,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//
//                    // Ícono y texto para el número de ingredientes usados
//                    Icon(
//                        imageVector = Icons.Default.ShoppingBasket,
//                        contentDescription = null,
//                        modifier = Modifier.padding(end = 1.dp) // Espaciado entre el ícono y el texto
//                    )
//
//                    Text(
//                        text = receta.usedIngredientCount.toString(),
//                        style = MaterialTheme.typography.bodyMedium,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
                }
            }
        }
    }
}
