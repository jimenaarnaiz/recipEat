package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.auth.FirebaseAuth


@Composable
fun HomeScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    val usersViewModel = UsersViewModel()

    // Observamos las recetas desde el ViewModel
    val recetasState by recetasViewModel.recetas.observeAsState(emptyList())

    var username by remember { mutableStateOf<String?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(username) {
        uid?.let {
            usersViewModel.obtenerUsername(it) { nombre ->
                username = nombre
            }
            recetasViewModel.obtenerRecetasHome(it)
        }
    }

    Log.d("HomeScreen", "Recetas: $recetasState")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Welcome, $username!",
            modifier = Modifier
                .padding(bottom = 16.dp)
        )

        // Mostrar un indicador de carga si no se han cargado las recetas
        if (recetasState.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // Carrusel de recetas
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recetasState) { receta ->
                    RecetaCard(receta = receta)
                }
            }
        }
    }
}

@Composable
fun RecetaCard(receta: Receta) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(300.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cargar la imagen de la receta
            Image(
                painter = rememberAsyncImagePainter(receta.image),
                contentDescription = receta.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )

            // Título de la receta
            Text(text = receta.title, modifier = Modifier.padding(top = 8.dp))

            // Botón de ver detalles
            Button(onClick = { /* TODO: Acción para ver detalles de la receta */ }) {
                Text(text = "Ver Más")
            }
        }
    }
}
