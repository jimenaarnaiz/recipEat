package com.example.recipeat.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import com.example.recipeat.data.model.DishTypes
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.theme.LightGray
import com.example.recipeat.ui.theme.LightYellow
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.data.model.IngredienteSimple
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.PermissionsViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel


@Composable
fun AddRecipe(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel,
    roomViewModel: RoomViewModel,
    usersViewModel: UsersViewModel,
    connectivityViewModel: ConnectivityViewModel,
    permissionsViewModel: PermissionsViewModel
) {
    val uid = usersViewModel.getUidValue()
    val hasStoragePermission = permissionsViewModel.storagePermissionGranted.value

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

    //para guardar imagen
    var imageUri2 by rememberSaveable { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val recetaId = recetasViewModel.generateRecipeId()

    // Comprobar si el ingrediente ingresado es válido
    fun validateIngredient(input: String) {
        val nombresIngredientes = ingredientesValidos.value.map { it.name }
        isIngredientValid = nombresIngredientes.contains(input)
    }

    // Photo picker (1 pic)
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            imageUri2 = uri
            usersViewModel.saveImageLocally(context, uri, recetaId = recetaId )
            bitmap = usersViewModel.loadImageFromFile(context, recetaId)
            //newImage = uri.toString()
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    LaunchedEffect(navController) {
        Log.d("AddRecipe", "pueba; launched de cargar ingredientes")
        ingredientesViewModel.loadIngredientsFromFirebase()
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
                "Create your recipe", onBackPressed = { navController.popBackStack() },
            )
        }
    ) { paddingValues ->

        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Network error. Please, check your internet connection.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ImageSection(imageUri2, pickMedia, hasStoragePermission)
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
                        onAddIngredient = { localIngredientImage ->
                            if (isIngredientValid && amount.isNotEmpty() && unit.isNotEmpty()) {
                                val newIngredient = Ingrediente(
                                    name = ingredientName,
                                    image = localIngredientImage,  // Usamos la imagen local pasada
                                    amount = amount.toDouble(),
                                    unit = unit,
                                    aisle = ""
                                )
                                ingredients = ingredients + newIngredient
                                ingredientName = ""
                                ingredientImage = ""  // Resetear la imagen local
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
                item {
                    SectionHeader("Instructions")
                }
                item {
                    InstructionsField(instructions, onInstructionChange = { instructions = it })
                }
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
                        "Create Recipe",
                        isValid,
                        onClick = {
                            // Verificar que todos los campos necesarios tengan valores
                            val newReceta = Receta(
                                id = recetaId,
                                title = title,
                                image = if (imageUri2 != null) imageUri2.toString() else imageUri,
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
                                unusedIngredientCount = 0
                            )
                            if (uid != null) {
                                recetasViewModel.addMyRecipe(
                                    uid, newReceta,
                                    onComplete = { success, error ->
                                        if (success) {
                                            Log.d("AddRecipe", "Receta añadida correctamente")
                                            navController.navigate(BottomNavItem.MyRecipes.route)
                                        } else {
                                            Log.e("AddRecipe", "Error al añadir receta: $error")
                                        }
                                    }
                                )
                            }
                            //añadir a Room
                            roomViewModel.insertReceta(receta = newReceta)

                            // Guardar la imagen localmente si hay una seleccionada
                            imageUri2?.let { uri ->
                                usersViewModel.saveImageLocally(context, uri, recetaId = recetaId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

@Composable
fun ImageSection( imageUri2: Uri?, pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>, hasStoragePermission: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter =
                if (imageUri2 == null || imageUri2.toString().isBlank()) {
                    painterResource(id = R.drawable.food_placeholder)
                }else{
                    rememberAsyncImagePainter(imageUri2)
                },
            contentDescription = "Recipe picture",
            modifier = Modifier
                .fillMaxWidth(0.5f) // 50% del ancho
                .height(100.dp) // Más compacta aún
                .clip(RoundedCornerShape(8.dp))
                .padding(16.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        Button(
            enabled = hasStoragePermission,
            onClick = {
                pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            ) },
            modifier = Modifier.align(Alignment.CenterVertically),
            colors = buttonColors(containerColor = LightYellow, contentColor = Color.Black)
        ) {
            Text("Select image")
        }
    }

    // Mostrar mensaje si el permiso de almacenamiento no está concedido o es limitado
    if (!hasStoragePermission) {
        Text(
            text = "You need to grant storage permission to change the recipe image.",
            color = Color.Red,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun InputField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.DarkGray,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = Color.DarkGray,
        )
    )
}

@Composable
fun NumericInputRow(
    value1: String, label1: String,
    value2: String, label2: String,
    onValueChange1: (String) -> Unit,
    onValueChange2: (String) -> Unit,
    isUnit: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value1,
            onValueChange = onValueChange1,
            label = { Text(label1) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.DarkGray,
            )
        )

        OutlinedTextField(
            value = value2,
            onValueChange = onValueChange2,
            label = { Text(label2) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType =  if (!isUnit) KeyboardType.Number else KeyboardType.Unspecified
            ),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.DarkGray,
            )
        )
    }
}


@Composable
fun IngredientField(
    ingredientName: String,
    ingredientImage: String,
    amount: String,
    unit: String,
    isIngredientValid: Boolean,
    ingredientesValidos: State<List<IngredienteSimple>>,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onAddIngredient: (String) -> Unit
) {
    Column {
        // Estado local para la imagen del ingrediente
        var localIngredientImage by remember { mutableStateOf(ingredientImage) }

        InputField(ingredientName, "Ingredient Name") { onNameChange(it) }

        if (!isIngredientValid) {
            Text(
                text = "Ingredient is not valid. Please choose a valid one.",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isIngredientValid) {
            val validIngredient = ingredientesValidos.value.find { it.name == ingredientName }
            if (validIngredient != null) {
                localIngredientImage = validIngredient.image
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumericInputRow(
                value1 = amount,
                label1 = "Amount",
                value2 = unit,
                label2 = "Unit",
                isUnit = true,
                onValueChange1 = { newAmount -> onAmountChange(newAmount) },
                onValueChange2 = { newUnit -> onUnitChange(newUnit) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Log.d("AddRecipe", "localIngredientImage: $localIngredientImage")
        Button(
            onClick = { onAddIngredient(localIngredientImage)  },
            modifier = Modifier.fillMaxWidth(),
            colors = buttonColors(containerColor = LightYellow, contentColor = Color.Black)
        ) {
            Text("Add Ingredient")
        }
    }
}

@Composable
fun IngredientList(
    ingredients: List<Ingrediente>,
    onRemoveIngredient: (Ingrediente) -> Unit
) {
    Column {
        ingredients.forEach { ingredientItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${ingredientItem.name}: ${ingredientItem.amount} ${ingredientItem.unit}",
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onRemoveIngredient(ingredientItem) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ingredient",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun InstructionsField(instructions: List<String>, onInstructionChange: (List<String>) -> Unit) {
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
                    onInstructionChange(instructions.toMutableList().apply {
                        this[index] = newStep
                    })
                },
                label = { Text("Step ${index + 1}") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.DarkGray,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.DarkGray,
                ),
            )
            if (index > 0) {
                IconButton(onClick = {
                    onInstructionChange(instructions.toMutableList().apply {
                        removeAt(index)
                    })
                }) {
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
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onInstructionChange(instructions + "") },
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = buttonColors(containerColor = LightYellow, contentColor = Color.Black)
        ) {
            Text("Add Step")
        }
    }
}

@Composable
fun OccasionButtons(
    occasions: List<String>,
    onOccasionClicked: (List<String>)  -> Unit
) {
    // Occasions buttons
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DishTypes.entries.forEach { occasion ->
            val isPressed = occasions.contains(occasion.name) // Controla si está presionado

            Button(
                onClick = {
                    // Si ya está seleccionado, lo deselecciona; si no, lo selecciona
                    val newSelection = if (isPressed) emptyList() else listOf(occasion.name)
                    onOccasionClicked(newSelection)

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


@Composable
fun DietaryPreferenceButtons(
    isVegan: Boolean,
    isVegetarian: Boolean,
    isGlutenFree: Boolean,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DietaryPreferenceButton("Vegan", isVegan, onClick)
        DietaryPreferenceButton("Vegetarian", isVegetarian, onClick)
        DietaryPreferenceButton("Gluten-Free", isGlutenFree, onClick)
    }
}

@Composable
fun DietaryPreferenceButton(
    label: String,
    isPressed: Boolean,
    onClick: (String) -> Unit
) {
    Button(
        onClick = { onClick(label) },
        colors = buttonColors(
            containerColor = if (isPressed) LightYellow else LightGray,
            contentColor = Color.Black
        )
    ) {
        Text(label)
    }
}


@Composable
fun CreateRecipeButton(textButton: String, isValid: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, // Centra los elementos
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = buttonColors(containerColor = Cherry, contentColor = Color.White),
            enabled = isValid
        ) {
            Text(textButton)
        }

        if (!isValid) {
            Text(
                "Wait! You have to complete all the fields!",
                color = Cherry,
                modifier = Modifier.align(Alignment.CenterHorizontally) // Centra el texto de error
            )
            // Mostrar un mensaje de error si algún campo está vacío
            Log.e("AddRecipeScreen", "Todos los campos deben estar completos.")
        }
    }
}

