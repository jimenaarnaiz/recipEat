package com.example.recipeat.ui.screens

import com.example.recipeat.data.model.DishTypes
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.theme.LightGray
import com.example.recipeat.ui.theme.LightYellow
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@Composable
fun AddRecipe(navController: NavController, recetasViewModel: RecetasViewModel) {
    var title by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var ingredients by remember { mutableStateOf<List<Ingrediente>>(emptyList()) }
    var instructions by remember { mutableStateOf(listOf("")) }
    var readyInMinutes by remember { mutableStateOf<Int?>(null) }
    var occasions by remember { mutableStateOf(listOf("")) }

    // Nuevo estado para las opciones de Vegan, Vegetarian y Gluten-Free
    var isVegan by remember { mutableStateOf(false) }
    var isVegetarian by remember { mutableStateOf(false) }
    var isGlutenFree by remember { mutableStateOf(false) }

    var isPressed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppBar(
            "Create your recipe", navController,
            onBackPressed = { navController.popBackStack() },
        ) }
    ) { paddingValues ->
        // Contenedor principal con LazyColumn para el scroll
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                Text(
                    text = "Add Image (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    //textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(5.dp))


            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    //textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            item { // Ingredients
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    //textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Aquí puedes agregar campos para ingredientes si es necesario

                Spacer(modifier = Modifier.height(16.dp))
            }


            item { // Instructions
                // Instructions Header
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    //textAlign = TextAlign.Center
                )
                //(modifier = Modifier.height(5.dp))

                // Mostrar los pasos
                instructions.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { newStep ->
                                instructions = instructions.toMutableList().apply {
                                    this[index] = newStep
                                }
                            },
                            label = {
                                Text(
                                    "Step ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // Botón de papelera para eliminar el paso
                        if (index > 0) {  // El primer paso no puede ser eliminado
                            IconButton(
                                onClick = {
                                    instructions = instructions.toMutableList().apply {
                                        removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Step",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            instructions = instructions + ""  // Agregar un paso vacío
                        },
                        colors = buttonColors(containerColor = LightYellow, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                        //.padding(10.dp)
                    ) {
                        Text("Add Step")
                    }
                }
            }


            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Time
                Text(
                    text = "Time (min)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    //textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Aquí puedes agregar el campo de tiempo
                OutlinedTextField(
                    value = readyInMinutes?.toString() ?: "",
                    onValueChange = {
                        readyInMinutes = it.toIntOrNull()
                    },
                    modifier = Modifier.width(110.dp),
                    keyboardOptions = KeyboardOptions
                        .Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next //con Enter salta al sig campo
                        ),
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Occasions
                Text(
                    text = "Occasions",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    //textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Row con botones para cada valor de la enum
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()) // Habilita el desplazamiento horizontal
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Espaciado entre botones
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DishTypes.entries.forEach { occasion ->
                        Button(
                            onClick = {
                                // Aquí puedes manejar la lógica al presionar cada botón
                                println("Selected: $occasion")
                                !isPressed
                            },
                            colors = if (isPressed) buttonColors(containerColor = LightYellow, contentColor = Color.Black) else
                                buttonColors(containerColor = LightGray, contentColor = Color.Black),
                            modifier = Modifier.width(110.dp) // Los botones ocuparán el mismo ancho
                        ) {
                            Text(text = occasion.name) // El nombre del valor de la enum
                        }
                    }
                }

            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Textos de sección
                Text(
                    text = "Dietary Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Row con botones para cada opción
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()) // Habilita el desplazamiento horizontal
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Espaciado entre botones
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón para Gluten-Free
                    Button(
                        onClick = {
                            isGlutenFree = !isGlutenFree // Alternar estado de Gluten-Free
                        },
                        colors = if (isGlutenFree) buttonColors(containerColor = LightYellow, contentColor = Color.Black) else
                            buttonColors(containerColor = LightGray, contentColor = Color.Black),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Text(text = "Gluten-Free")
                    }

                    // Botón para Vegan
                    Button(
                        onClick = {
                            isVegan = !isVegan // Alternar estado de Vegan
                        },
                        colors = if (isVegan) buttonColors(containerColor = LightYellow, contentColor = Color.Black) else
                            buttonColors(containerColor = LightGray, contentColor = Color.Black),
                        modifier = Modifier.width(110.dp)
                    ) {
                        Text(text = "Vegan")
                    }

                    // Botón para Vegetarian
                    Button(
                        onClick = {
                            isVegetarian = !isVegetarian // Alternar estado de Vegetarian
                        },
                        colors = if (isVegetarian) buttonColors(containerColor = LightYellow, contentColor = Color.Black) else
                            buttonColors(containerColor = LightGray, contentColor = Color.Black),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Text(text = "Vegetarian")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Botón para añadir receta
                Button(
                    onClick = {
                        navController.navigate(BottomNavItem.MyRecipes.route)
                    },
                    colors = buttonColors(
                        containerColor = LightYellow,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Create Recipe")
                }
            }

        }
    }
}


