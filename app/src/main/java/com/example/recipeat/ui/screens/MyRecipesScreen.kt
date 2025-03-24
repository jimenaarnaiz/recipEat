package com.example.recipeat.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MyRecipesScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val recetasUser by recetasViewModel.recetasUser.observeAsState(emptyList())

    LaunchedEffect(userId) {
        if (userId != null) {
            recetasViewModel.getRecetasUser(userId)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_recipe") },
                containerColor = Cherry,
                modifier = Modifier
                    .padding(bottom = 80.dp) // padding debajo para q no quede opacado por la bottom bar
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipe", tint = Color.White)
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .padding(bottom = 16.dp)
        ) {
            if (recetasUser.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add your first recipe!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // Carrusel de recetas
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 90.dp) // Respetar espacio FloatingButton
                ) {
                    items(recetasUser) { receta ->
                        RecetaCard(receta, navController)
                    }

                }
            }
        }
    }
}




