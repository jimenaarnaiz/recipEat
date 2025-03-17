package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    val usersViewModel = UsersViewModel()

    // Observamos las recetas desde el ViewModel
    val recetasState by recetasViewModel.recetas.observeAsState(emptyList())

    var username by remember { mutableStateOf<String?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    // Detectar si el usuario ha llegado cerca del final de la lista
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val isLoadingMore by recetasViewModel.isLoadingMore.observeAsState(false) // Estado de carga adicional

    LaunchedEffect(username) {
        uid?.let {
            recetasViewModel.obtenerRecetasHome(it, limpiarLista = true) // Primera carga

            usersViewModel.obtenerUsername(it) { nombre ->
                username = nombre
            }

            Log.d("HomeScreen", "Recetas inicial: ${recetasState.size}")
        }

        //121 recetas de momento
        val db = Firebase.firestore

        db.collection("recetas")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val count = querySnapshot.size() // Número de documentos
                Log.d("Firebase", "Número de recetas: $count")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error obteniendo recetas", e)
            }
    }

    // Detecta si el usuario está cerca del final de la lista
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { index ->
                Log.d("HomeScreen", "Índice visible actual: $index")
                if (index == recetasState.size - 1 && !isLoadingMore) {
                    Log.d("HomeScreen", "Cargando más recetas")
                    uid?.let {
                        recetasViewModel.obtenerRecetasHome(it, limpiarLista = false)
                        Log.d("HomeScreen", "Num de recetas act: ${recetasState.size}")
                    }
                }
            }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Welcome, $username!",
            modifier = Modifier
                .padding(16.dp)
        )


        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {
                isActive = false
            },
            active = isActive,
            onActiveChange = { isActive = it; if (it) navController.navigate("search") },
            placeholder = { Text("Search for recipes...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            //
        }

        // Mostrar un indicador de carga si no se han cargado las recetas
        if (recetasState.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // Carrusel de recetas
            LazyColumn(
                state = listState, // Vincular el estado de la lista
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp) // Agregar espacio al final de la lista
            ) {
                items(recetasState) { receta ->
                    RecetaCard(receta = receta, navController)
                }

                // Cargar más recetas si el usuario está cerca del final


            // Indicador de carga al final de la lista
            if (isLoadingMore) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
            }
        }
    }
}

@Composable
fun RecetaCard(receta: Receta, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Hace que la Card ocupe tdo el ancho disponible
            .padding(vertical = 8.dp) // Separación vertical entre las Cards
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable { navController.navigate("detalles/${receta.id}") },
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
                    Log.d("HOME", "steps: ${receta.steps.size}")
                }
            }
        }
    }
}
