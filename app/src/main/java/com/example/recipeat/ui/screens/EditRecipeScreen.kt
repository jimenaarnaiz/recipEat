package com.example.recipeat.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.BottomNavItem
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
    var selectedOccasion by rememberSaveable { mutableStateOf(emptyList<String>()) }

    // Nuevo estado para las opciones de Vegan, Vegetarian y Gluten-Free
    var isVegan by rememberSaveable { mutableStateOf(false) }
    var isVegetarian by rememberSaveable { mutableStateOf(false) }
    var isGlutenFree by rememberSaveable { mutableStateOf(false) }


    // Obtenemos la lista de ingredientes válidos
    val ingredientesValidos = ingredientesViewModel.ingredientesValidos.collectAsState()
    var ingredientName by rememberSaveable { mutableStateOf("") }
    var ingredientImage by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }  // cantidad
    var unit by rememberSaveable { mutableStateOf("") }    // unidad
    var isIngredientValid by rememberSaveable { mutableStateOf(true) } // Controlar si el ingrediente es válido

    // Estado mutable para la validación
    var isValid by rememberSaveable { mutableStateOf(false) }


    // Comprobar si el ingrediente ingresado es válido
    fun validateIngredient(input: String) {
        val nombresIngredientes = ingredientesValidos.value.map { it.name }
        isIngredientValid = nombresIngredientes.contains(input)
    }

    LaunchedEffect(Unit) {
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
                    selectedOccasion = receta.dishTypes

                    // Marcar opciones de dieta
                    isVegan = receta.vegan
                    isVegetarian = receta.vegetarian
                    isGlutenFree = receta.glutenFree
                }
        }
    }

    // Recalcular isValid cada vez que los valores cambien
    LaunchedEffect(title, servings, ingredients, instructions, time) {
        isValid = title.isNotEmpty() &&
                servings.isNotBlank() &&
                ingredients.isNotEmpty() &&
                instructions.all { it.isNotBlank() } && //comprobar q los steps no son ""
                time.isNotBlank()
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
                //ImageSection()  //TODO
            }
            item {
                SectionHeader("Recipe Details")
            }
            item {
                InputField(title, "Recipe Name") { title = it }
            }

            item {
                // Fila para Servings y Time
                NumericInputRow(
                    value1 = servings,
                    label1 = "Servings",
                    value2 = time,
                    label2 = "Time (min)",
                    isUnit = false,
                    onValueChange1 = { newServings -> servings = newServings },
                    onValueChange2 = { newTime -> time = newTime }
                )
            }

            // Sección de "Ingredientes"
            item {
                SectionHeader("Ingredients")
            }

            item {
                IngredientField(
                    ingredientName,
                    ingredientImage,
                    amount,
                    unit,
                    isIngredientValid,
                    ingredientesValidos,
                    onNameChange = { ingredientName = it; validateIngredient(it) },
                    onAmountChange = { amount = it },
                    onUnitChange = { unit = it },
                    onAddIngredient = {
                        if (isIngredientValid && amount.isNotEmpty() && unit.isNotEmpty()) {
                            val newIngredient = Ingrediente(
                                name = ingredientName,
                                image = "https://img.spoonacular.com/ingredients_100x100/${ingredientImage}",
                                amount = amount.toDouble(),
                                unit = unit,
                                aisle = ""
                            )
                            ingredients = ingredients + newIngredient
                            ingredientName = ""
                            ingredientImage = ""
                            amount = ""
                            unit = ""
                        }
                    }
                )
            }
            item {
                IngredientList(
                    ingredients = ingredients,
                    onRemoveIngredient = { ingredientToRemove ->
                        ingredients = ingredients.filter { it != ingredientToRemove }
                    }
                )
            }

            // Sección de "Pasos"
            item {
                SectionHeader("Instructions")
            }

            item {
                InstructionsField(instructions, onInstructionChange = { instructions = it })
            }

            // Sección de "Preferencias dietéticas"
            item {
                SectionHeader("Meal Type")
            }

            item {
                OccasionButtons(
                    occasions = selectedOccasion,
                    onOccasionClicked = { newSelection ->
                        selectedOccasion = newSelection
                    }
                )
            }
            // Sección de "Preferencias dietéticas"
            item {
                SectionHeader("Dietary Preferences")
            }

            item {
                DietaryPreferenceButtons(isVegan, isVegetarian, isGlutenFree) { preference ->
                    when (preference) {
                        "Vegan" -> isVegan = !isVegan
                        "Vegetarian" -> isVegetarian = !isVegetarian
                        "Gluten-Free" -> isGlutenFree = !isGlutenFree
                    }
                }
            }

            item {
                CreateRecipeButton(
                    "Update Recipe",
                    isValid,
                    onClick = {
                        val newReceta = Receta(
                            id = idReceta,
                            title = title,
                            image = imageUri,
                            servings = servings.toInt(),
                            ingredients = ingredients,
                            steps = instructions,
                            time = time.toInt(),
                            dishTypes = selectedOccasion,
                            userId = uid.toString(),
                            usedIngredientCount = ingredients.size,
                            glutenFree = isGlutenFree,
                            vegan = isVegan,
                            vegetarian = isVegetarian,
                            date = System.currentTimeMillis(),
                            unusedIngredients = emptyList(),
                            missingIngredientCount = 0,
                            unusedIngredientCount = 0,
                            esFavorita = null,
                        )
                        recetasViewModel.editMyRecipe(
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
                    }
                )
            }
        }
    }
}

