package com.example.recipeat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.recipeat.R
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MyRecipesScreen(navController: NavHostController, recetasViewModel: RecetasViewModel) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val recetasUser by recetasViewModel.recetasUser.observeAsState(emptyList())

    LaunchedEffect(userId) {
        recetasViewModel.getRecetasUser(userId)
    }

    Scaffold(
        topBar = { AppBar(
            "My Recipes", navController,
            onBackPressed = {
                navController.popBackStack()
            }
        ) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_recipe") },
                containerColor = Cherry,
                modifier = Modifier
                    .padding(bottom = 85.dp) // padding debajo para q no quede opacado por la bottom bar
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipe", tint = Color.White)
            }
        }
    ) { paddingValues ->
        if (recetasUser.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Add your first recipe!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recetasUser) { receta -> // Aquí ya no habrá error) { receta ->
                    RecipeItem(receta) {
                        navController.navigate("recipe_details/${receta.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItem(receta: Receta, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Mantiene un diseño cuadrado
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (receta.image.isNullOrBlank()){
                AsyncImage(
                    model = receta.image,
                    contentDescription = "Recipe Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }else{
                Image(
                    painter = painterResource(id = R.drawable.food_placeholder),
                    contentDescription = "Recipe picture",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(100.dp)
                        .clip(CircleShape)
                        .shadow(4.dp, CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(receta.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("${receta.time} min", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
