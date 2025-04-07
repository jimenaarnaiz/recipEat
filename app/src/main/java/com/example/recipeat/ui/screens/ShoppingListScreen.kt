package com.example.recipeat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.recipeat.data.model.Ingrediente
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel

@Composable
fun ShoppingListScreen(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
    planViewModel: PlanViewModel
) {
    // Suponemos que planSemanal es LiveData<PlanSemanal>
    val planSemanal by planViewModel.planSemanal.observeAsState()

//    // Definir un conjunto de unidades aceptadas (en minúsculas)
//    val acceptedUnits = setOf("g", "gram", "grams", "unit", "units", "serving", "servings", "ml")

//    val ingredientesAgrupados = remember(planSemanal) {
//        val mapaIngredientes = mutableMapOf<String, Triple<Double, String, String>>() // nombre -> (cantidad total, unidad, imageUrl)
//        planSemanal?.weekMeals?.values?.forEach { dayMeal ->
//            listOf(dayMeal.breakfast, dayMeal.lunch, dayMeal.dinner).forEach { receta ->
//                receta.ingredients.forEach { ingrediente ->
//                    val name = ingrediente.name
//                    val amount = ingrediente.amount
//                    val unit = ingrediente.unit
//                    val image = ingrediente.image
//
//                    // Si ya existe el ingrediente y la unidad coincide (ignorando mayúsculas), sumamos la cantidad
//                    if (mapaIngredientes.containsKey(name)) {
//                        val (existingAmount, existingUnit, existingImage) = mapaIngredientes[name]!!
//                        if (unit.lowercase(Locale.getDefault()) == existingUnit.lowercase(Locale.getDefault())) {
//                            mapaIngredientes[name] = Triple(existingAmount + amount, unit, image.ifEmpty { existingImage })
//                        } else {
//                            // Si la unidad difiere, diferenciamos la clave agregando la unidad al nombre
//                            val newKey = "$name ($unit)"
//                            mapaIngredientes[newKey] = Triple(amount, unit, image)
//                        }
//                    } else {
//                        mapaIngredientes[name] = Triple(amount, unit, image)
//                    }
//                }
//            }
//        }
//        mapaIngredientes.toList()
//    }


    val ingredientesAgrupados = remember(planSemanal) {
        val ingredientesList =
            mutableListOf<List<Any>>() // Para almacenar los ingredientes en el formato adecuado

        // Verificar que el plan semanal no es nulo y que contiene las comidas
        planSemanal?.weekMeals?.values?.forEach { dayMeal ->
            // Iterar sobre las comidas del día: desayuno, almuerzo y cena
            listOf(dayMeal.breakfast, dayMeal.lunch, dayMeal.dinner).forEach { receta ->
                receta.ingredients.forEach { ingrediente ->
                    // Extraer la información de cada ingrediente
                    val name = ingrediente.name
                    val amount = ingrediente.amount
                    val unit = ingrediente.unit
                    val aisle =
                        ingrediente.aisle ?: "Unknown"  // Si aisle es nulo, asignamos "Unknown"
                    val image = ingrediente.image

                    // Agregar al listado en el formato adecuado
                    ingredientesList.add(listOf(name, amount, unit, aisle, image))
                }
            }
        }

        ingredientesList // Devolver la lista de ingredientes

    }

// Observar los ingredientes agrupados desde el ViewModel
    val ingredientesAgrupados2 = planViewModel.groupedIngredients.observeAsState(emptyList())


//    // Para imprimir los ingredientes agrupados:
//    LaunchedEffect(ingredientesAgrupados) {
//        ingredientesAgrupados.forEach { (nombre, triple) ->
//            val (cantidad, unidad, imagen) = triple
//            Log.d("ShoppingListScreen", "$nombre: $cantidad $unidad $imagen")
//        }
//    }


    // Llamar a la función para procesar los ingredientes
    LaunchedEffect(ingredientesAgrupados) {
        // Llamar a la función del ViewModel para procesar los ingredientes
        planViewModel.getGroupedIngredients(ingredientesAgrupados.map {
            Ingrediente(
                name = it[0] as String,
                amount = it[1] as Double,
                unit = it[2] as String,
                aisle = it[3] as String,
                image = it[4] as String
            )
        })

    }

    Scaffold(
        topBar = {
            AppBar(
                "Shopping List", navController,
                onBackPressed = { navController.popBackStack() },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            // Agrupar los ingredientes por el campo "aisle"
            val ingredientesAgrupadosAisle =
                ingredientesAgrupados2.value.groupBy { it.aisle.ifEmpty { "Others" } }

            LazyColumn {
                // Recorrer cada grupo
                ingredientesAgrupadosAisle.forEach { (aisle, ingredientesDelAisle) ->
                    item {
                        // Mostrar el nombre del aisle como un encabezado
                        Text(
                            text = aisle,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    // Mostrar los ingredientes que pertenecen a ese aisle
                    items(ingredientesDelAisle) { ingrediente ->
                        val checkedState = remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // Mostrar imagen del ingrediente
                            AsyncImage(
                                model = "https://img.spoonacular.com/ingredients_100x100/${ingrediente.image}",
                                contentDescription = ingrediente.name,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ingrediente.name
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // CheckBox para marcar el ingrediente como comprado
                            Checkbox(
                                checked = checkedState.value,
                                onCheckedChange = { checked ->
                                    checkedState.value = checked
                                    // TODO lógica guardar el estado en una base de datos o actualizar el ViewModel
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}