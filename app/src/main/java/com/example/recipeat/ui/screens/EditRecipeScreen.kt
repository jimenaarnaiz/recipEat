package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.recipeat.R
import com.example.recipeat.data.model.DishTypes
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightGray
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.google.firebase.auth.FirebaseAuth


//TODO hacer las comprobaciones de los campos!!!

@Composable
fun EditRecipeScreen(
    idReceta: String, navController: NavHostController, recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel, deUser: Boolean
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    val recetaEditarState by recetasViewModel.recetaSeleccionada.observeAsState()

    var title by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf("") }
    var servings by rememberSaveable { mutableStateOf("") }
    var ingredients by rememberSaveable { mutableStateOf<List<Ingrediente>>(emptyList()) }
    var instructions by rememberSaveable { mutableStateOf(listOf("")) }
    var time by rememberSaveable { mutableStateOf("") }
    var occasions by remember { mutableStateOf(listOf("")) }

    // Nuevo estado para las opciones de Vegan, Vegetarian y Gluten-Free
    var isVegan by rememberSaveable { mutableStateOf(false) }
    var isVegetarian by rememberSaveable { mutableStateOf(false) }
    var isGlutenFree by rememberSaveable { mutableStateOf(false) }

    var isPressed by remember { mutableStateOf(false) }

    // Obtenemos la lista de ingredientes válidos
    val ingredientesValidos = ingredientesViewModel.ingredientesValidos.collectAsState()
    var ingredientName by rememberSaveable { mutableStateOf("") }
    var ingredientImage by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }  // cantidad
    var unit by rememberSaveable { mutableStateOf("") }    // unidad
    var isIngredientValid by remember { mutableStateOf(true) } // Controlar si el ingrediente es válido


    // Comprobar si el ingrediente ingresado es válido
    fun validateIngredient(input: String) {
        val nombresIngredientes = ingredientesValidos.value.map { it.name }
        isIngredientValid = nombresIngredientes.contains(input)
    }

    LaunchedEffect(uid) {
        if (uid != null) {
            recetasViewModel.obtenerRecetaPorId(
                uid = uid, recetaId = idReceta, deUser = deUser
            )
            ingredientesViewModel.loadIngredientsFromFirebase()

            // Llenar los campos con la información de la receta cargada
            recetaEditarState?.let { receta ->
                    title = receta.title
                    imageUri = receta.image.toString()
                    servings = receta.servings.toString()
                    ingredients = receta.ingredients
                    instructions = receta.steps
                    time = receta.time.toString()
                    occasions = receta.dishTypes

                    // Marcar opciones de dieta
                    isVegan = receta.vegan
                    isVegetarian = receta.vegetarian
                    isGlutenFree = receta.glutenFree
                }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                "Edit your recipe", navController,
                onBackPressed = { navController.popBackStack() },
            )
        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth(), // Asegura que ocupe el ancho disponible
                    //.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre los elementos
                    verticalAlignment = Alignment.CenterVertically // Alinea verticalmente los elementos
                ) {

//                if (receta.image.isNullOrBlank()){
//                    AsyncImage(
//                        model = receta.image,
//                        contentDescription = "Recipe Image",
//                        modifier = Modifier
//                            .size(120.dp)
//                            .clip(RoundedCornerShape(8.dp)),
//                        contentScale = ContentScale.Crop
//                    )
//                }else{
                    Image(
                        painter = painterResource(id = R.drawable.food_placeholder),
                        contentDescription = "Recipe picture",
                        modifier = Modifier
                            .padding(16.dp)
                            .size(150.dp)
                            .shadow(4.dp),
                        //.align(Alignment.CenterHorizontally)
                        contentScale = ContentScale.Crop
                    )
                    //}
                    // Name Field

                    // Botón
                    Button(
                        onClick = {
                            //TODO: Acción al hacer clic
                        },
                        modifier = Modifier.align(Alignment.CenterVertically), // Alineación vertical del botón
                        colors = buttonColors(
                            containerColor = LightYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Select image")
                    }
                }
            }

            // Sección de "Receta"
            item {
                SectionHeader("Recipe Details")
            }


            item {
                // Campo para el nombre de la receta
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Recipe Name") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                        unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                        focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                    )
                )

            }

            item {
                // Fila para Servings y Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Servings
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text("Servings") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                            unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                            focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                        ),
                    )

                    // Time
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (min)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                            unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                            focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                        ),
                    )
                }
            }

            // Sección de "Ingredientes"
            item {
                SectionHeader("Ingredients")
            }

            item {
                OutlinedTextField(
                    value = ingredientName,
                    onValueChange = {
                        ingredientName = it
                        validateIngredient(it)
                    },
                    label = { Text("Ingredient Name") },
                    isError = !isIngredientValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                        unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                        focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                    )
                )

                if (!isIngredientValid) {
                    Text(
                        text = "Ingredient is not valid. Please choose a valid one.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isIngredientValid) {
                    val validIngredient =
                        ingredientesValidos.value.find { it.name == ingredientName }
                    ingredientImage = validIngredient?.image.orEmpty()
                }
            }

            if (isIngredientValid) {
                item {
                    // Fila para Amount y Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Amount
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                            ),
                        )

                        // Unit
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                            ),
                        )
                    }
                }
            }

            item {
                // Botón para añadir ingrediente
                Button(
                    onClick = {
                        if (isIngredientValid && amount.isNotEmpty() && unit.isNotEmpty()) {
                            val newIngredient = Ingrediente(
                                name = ingredientName,
                                image = "https://img.spoonacular.com/ingredients_100x100/${ingredientImage}",
                                amount = amount.toDouble(),
                                unit = unit
                            )
                            ingredients = ingredients + newIngredient
                            ingredientName = ""
                            ingredientImage = ""
                            amount = ""
                            unit = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors(
                        containerColor = LightYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Add Ingredient")
                }
            }

            item {
                // Mostrar los ingredientes añadidos
                ingredients.forEach { ingredientItem ->
                    Text("Ingredient: ${ingredientItem.name}, Amount: ${ingredientItem.amount}, Unit: ${ingredientItem.unit}")
                }
            }

            // Sección de "Pasos"
            item {
                SectionHeader("Instructions")
            }

            item {
                // Instructions
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
                            label = { Text("Step ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                            ),
                        )

                        if (index > 0) {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { instructions = instructions + "" },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        colors = buttonColors(
                            containerColor = LightYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Add Step")
                    }
                }
            }

            // Sección de "Preferencias dietéticas"
            item {
                SectionHeader("Meal Type")
            }

            item {
                // Occasions buttons
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DishTypes.entries.forEach { occasion ->
                        Button(
                            onClick = {
                                occasions += occasion.name
                                println("AddRecipe: $occasion")
                            },
                            colors = buttonColors(
                                containerColor = if (isPressed) LightYellow else LightGray,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.width(110.dp)
                        ) {
                            Text(text = occasion.name)
                        }
                    }
                }
            }

            // Sección de "Preferencias dietéticas"
            item {
                SectionHeader("Dietary Preferences")
            }

            item {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isGlutenFree = !isGlutenFree },
                        colors = buttonColors(
                            containerColor = if (isGlutenFree) LightYellow else LightGray,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Text(text = "Gluten-Free")
                    }

                    Button(
                        onClick = { isVegan = !isVegan },
                        colors = buttonColors(
                            containerColor = if (isVegan) LightYellow else LightGray,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.width(110.dp)
                    ) {
                        Text(text = "Vegan")
                    }

                    Button(
                        onClick = { isVegetarian = !isVegetarian },
                        colors = buttonColors(
                            containerColor = if (isVegetarian) LightYellow else LightGray,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Text(text = "Vegetarian")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val newReceta = Receta(
                            id = recetasViewModel.generateRecipeId(),
                            title = title,
                            image = imageUri,
                            servings = servings.toInt(),
                            ingredients = ingredients,
                            steps = instructions,
                            time = time.toInt(),
                            dishTypes = occasions,
                            userId = uid.toString(),
                            usedIngredientCount = ingredients.size,
                            glutenFree = isGlutenFree,
                            vegan = isVegan,
                            vegetarian = isVegetarian,
                            date = System.currentTimeMillis(),
                            unusedIngredients = emptyList(),
                            missingIngredientCount = 0,
                            unusedIngredientCount = 0
                        )
                        recetasViewModel.addMyRecipe(
                            uid.toString(), newReceta,
                            onComplete = { success, error ->
                                if (success) {
                                    Log.d("EditRecipe", "Recipe was edited successfully!")
                                    navController.navigate(BottomNavItem.MyRecipes.route)
                                } else {
                                    Log.e("EditRecipe", "Error editing recipe: $error")
                                }
                            }
                        )
                    },
                    colors = buttonColors(
                        containerColor = Cherry,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(text = "Update Recipe", color = Color.White)
                }
            }
        }
    }
}

