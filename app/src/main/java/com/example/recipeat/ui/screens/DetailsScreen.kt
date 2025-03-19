package com.example.recipeat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DetailsScreen(
    idReceta: Int,
    navController: NavHostController,
    recetasViewModel: RecetasViewModel,
) {
    val receta by recetasViewModel.recetaSeleccionada.observeAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val esFavorito by recetasViewModel.esFavorito.observeAsState()

    var cocinado by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        if (uid != null) {
            recetasViewModel.obtenerRecetaPorId(
                uid = uid,
                recetaId = idReceta.toString()
            )
        }
        recetasViewModel.verificarSiEsFavorito(uid, idReceta.toString())
    }

    Scaffold(
        topBar = {
            AppBar(
                title = "", navController = navController,
                onBackPressed = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        receta?.let { recetaDetalle ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = recetaDetalle.image),
                    contentDescription = "Recipe Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .shadow(4.dp),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recetaDetalle.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = {
                        receta!!.image?.let {
                            recetasViewModel.toggleFavorito(
                                uid, idReceta.toString(),
                                title = receta!!.title,
                                image = it,
                                )
                        }
                    }) {
                        Icon(
                            modifier = Modifier.size(35.dp),
                            imageVector = if (esFavorito == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (esFavorito == true) Cherry else Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ready in ${recetaDetalle.time} minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingredients:",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                recetaDetalle.ingredients.forEach { ingredient ->
                    Text(
                        text = "- ${ingredient.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Steps:",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                recetaDetalle.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!cocinado) {
                            recetasViewModel.añadirHistorial(uid, idReceta.toString())
                            cocinado = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!cocinado) LightYellow else Cherry
                    ),
                    //enabled = !cocinado
                ) {

                    val color = if (!cocinado) Color.DarkGray else Color.White
                    if (cocinado) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Cooked",
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (cocinado) "Cooked!" else "Cook",
                        color = color
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
