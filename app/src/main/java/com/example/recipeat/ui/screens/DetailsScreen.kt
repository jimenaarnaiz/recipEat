package com.example.recipeat.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.TopBarWithIcons
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.example.recipeat.utils.NetworkConnectivityManager

@Composable
fun DetailsScreen(
    idReceta: String,
    navController: NavHostController,
    recetasViewModel: RecetasViewModel,
    esDeUser: Boolean,
    roomViewModel: RoomViewModel,
    usersViewModel: UsersViewModel
) {
    val receta by recetasViewModel.recetaSeleccionada.observeAsState()
    val uid = usersViewModel.getUidValue()
    val esFavorito by recetasViewModel.esFavorito.observeAsState()

    // Estado para mostrar el AlertDialog de confirmación de eliminación
    var showDialog by remember { mutableStateOf(false) }

    // Instanciar el NetworkConnectivityManager
    val context = LocalContext.current
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }

   val recetaRoom by roomViewModel.recipeRoom.observeAsState()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        // Registrar el callback para el estado de la red
    LaunchedEffect(true) {
        networkConnectivityManager.registerNetworkCallback()
    }

    // Usar DisposableEffect para desregistrar el callback cuando la pantalla se destruye
    DisposableEffect(context) {
        // Desregistrar el NetworkCallback cuando la pantalla deje de ser visible
        onDispose {
            networkConnectivityManager.unregisterNetworkCallback()
        }
    }

    // Verificar si hay conexión y ajustar el ícono de favoritos
    val isConnected = networkConnectivityManager.isConnected.value

    LaunchedEffect(receta?.id) {
        Log.d("DetailsScreen","Llamando a obtenerRecetaPorId con recetaId: $idReceta deUser: $esDeUser")
        recetasViewModel.obtenerRecetaPorId(
            uid = uid.toString(),
            recetaId = idReceta,
            deUser = esDeUser
        )

        recetasViewModel.verificarSiEsFavorito(uid.toString(), idReceta)
        roomViewModel.getRecetaById(recetaId = idReceta)
        bitmap = usersViewModel.loadImageFromFile(context, idReceta)
    }

    Scaffold(
        topBar = {
            if (esDeUser && isConnected) { //si hay conexion y es de la api se puede eliminar y editar
                TopBarWithIcons(
                    onBackPressed = { navController.popBackStack() },
                    onEditPressed = { navController.navigate("editRecipe/$idReceta/$esDeUser") },
                    onDeletePressed = { showDialog = true }
                )
            } else {
                AppBar(
                    title = "", navController = navController,
                    onBackPressed = { navController.popBackStack() }
                )
            }
        }
    ) { paddingValues ->
        // Usamos receta si está conectados, si no, usamos recetaRoom
        val recetaShowed = if (isConnected && receta != null) receta else recetaRoom

        recetaShowed?.let { recetaDetalle ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {

                // Cargar la imagen de la receta con esquinas redondeadas y sin padding
                // Para el caso de carga remota
//                var imagen by remember { mutableStateOf("") }
//                imagen = if (recetaDetalle.image?.isNotBlank() == true) {
//                    recetaDetalle.image
//                } else {
//                    "android.resource://com.example.recipeat/${R.drawable.food_placeholder}"
//                }

                // Determinamos el painter según esDeUser
                val painter = if (esDeUser) {
                    if (recetaDetalle.image.toString().isBlank() || bitmap == null) {
                        painterResource(id = R.drawable.food_placeholder)
                    } else {
                        BitmapPainter(bitmap!!.asImageBitmap())
                    }
                } else {
                    rememberAsyncImagePainter(recetaDetalle.image, error = painterResource(id = R.drawable.food_placeholder))
                }

                Image(
                    painter = painter,
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
                        text = "${recetaDetalle.title} ${recetaDetalle.esFavorita}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    if (isConnected){
                        IconButton(
                            onClick = {
                                receta!!.image?.let {
                                    recetasViewModel.toggleFavorito(
                                        uid.toString(),
                                        recetaId = idReceta,
                                        title = receta!!.title,
                                        image = it,
                                        userReceta = receta!!.userId
                                    )
                                }
                                if (esFavorito == false) { // si no es fav, se añade al Room con true
                                    receta!!.esFavorita = true
                                    roomViewModel.insertReceta(receta!!)
                                }else{
                                    if (esDeUser) { //si es creada por el user, solo pone a false favs
                                        roomViewModel.setEsFavoritaToZero(idReceta)
                                    }else{ // si es de la api, se elimina
                                        roomViewModel.deleteReceta(receta!!)
                                    }
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
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically, // Alinea verticalmente los elementos en el centro
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    // Ícono de reloj para "Ready in"
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "Time Icon",
                        modifier = Modifier.size(20.dp) // Tamaño del ícono
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ready in ${recetaDetalle.time} minutes",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Ícono de plato para "Servings"
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = "Servings Icon",
                        modifier = Modifier.size(20.dp) // Tamaño del ícono
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${recetaDetalle.servings}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingredients:",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                recetaDetalle.ingredients.forEach { ingredient ->
                    Text(
                        text = "- ${ingredient.name} (${ingredient.amount} ${ingredient.unit})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

//                Text(
//                    text = "Steps:",
//                    style = MaterialTheme.typography.headlineSmall,
//                    modifier = Modifier.padding(horizontal = 16.dp)
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))

                //RecetaStepsWithButton(recetaDetalle)
//                var currentStepIndex by remember { mutableStateOf(0) } // Índice del paso actual
//                var isStepsVisible by remember { mutableStateOf(false) } // Estado que controla si los pasos son visibles
//                val totalSteps = recetaDetalle.steps.size // Número total de pasos
//
//                // Barra de progreso que indica cuántos pasos faltan
//                val progress = if (totalSteps > 0) currentStepIndex / totalSteps.toFloat() else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Botón para mostrar los pasos
                    Button(
                        onClick = { navController.navigate("steps/${idReceta}/${esDeUser}") },
                        modifier = Modifier.fillMaxWidth(),
                        //enabled = !isStepsVisible, // Habilitar solo si los pasos no están visibles
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Show steps")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

//                    // Si los pasos están visibles, mostramos el paso actual
//                    if (isStepsVisible) {
//                        // Barra de progreso
//                        LinearProgressIndicator(
//                            progress = { progress },
//                            modifier = Modifier.fillMaxWidth(),
//                            color = Cherry,
//                        )
//
//                        Spacer(modifier = Modifier.height(16.dp))
//
//                        // Mostrar el paso actual
//                        if (currentStepIndex < totalSteps) {
//                            Text(
//                                text = "${currentStepIndex + 1}. ${recetaDetalle.steps[currentStepIndex]}",
//                                style = MaterialTheme.typography.bodyMedium,
//                                modifier = Modifier.padding(vertical = 8.dp)
//                            )
//                        }
//
//                        Row(
//                            horizontalArrangement = Arrangement.spacedBy(16.dp),
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 16.dp)
//                        ) {
//                            // Botón para retroceder al paso anterior
//                            if (currentStepIndex > 0 && !cocinado) {
//                                Button(
//                                    onClick = { currentStepIndex-- },
//                                    modifier = if (currentStepIndex == totalSteps - 1) Modifier.weight(
//                                        0.8f
//                                    ) else Modifier.weight(1f),
//                                    colors = ButtonDefaults.buttonColors(
//                                        containerColor = LightYellow,
//                                        contentColor = Color.Black
//                                    )
//                                ) {
//                                    Text("Back")
//                                }
//                            }
//
//                            // Botón para adelantar al siguiente paso
//                            if (currentStepIndex < totalSteps - 1) {
//                                Button(
//                                    onClick = { currentStepIndex++ },
//                                    modifier = Modifier.weight(1f),
//                                    colors = ButtonDefaults.buttonColors(
//                                        containerColor = LightYellow,
//                                        contentColor = Color.Black
//                                    )
//                                ) {
//                                    Text("Next")
//                                }
//                            } else {
//                                Button(
//                                    onClick = {
//                                        if (!cocinado) {
//                                            receta!!.image?.let {
//                                                recetasViewModel.añadirHistorial(
//                                                    uid = uid.toString(),
//                                                    userReceta = receta!!.userId,
//                                                    recetaId = idReceta,
//                                                    title = receta!!.title,
//                                                    image = it,
//                                                )
//                                            }
//                                            cocinado = true
//                                        }
//                                    },
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        //.padding(horizontal = 16.dp)
//                                        .weight(1f),
//                                    colors = ButtonDefaults.buttonColors(
//                                        containerColor = if (!cocinado) LightYellow else Cherry
//                                    ),
//                                    //enabled = !cocinado
//                                ) {
//
//                                    val color = if (!cocinado) Color.DarkGray else Color.White
//                                    if (cocinado) {
//                                        Icon(
//                                            imageVector = Icons.Filled.Check,
//                                            contentDescription = "Cooked",
//                                            tint = color
//                                        )
//                                        Spacer(modifier = Modifier.width(8.dp))
//                                    }
//                                    Text(
//                                        text = if (cocinado) "Cooked!" else "Cook",
//                                        color = color
//                                    )
//                                }
//
//                            }
//                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
        }
    }

    // AlertDialog de confirmación de eliminación
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, // Cierra el dialogo si se toca fuera de él
            title = { Text("Confirm deletion") },
            text = { Text("Are you sure you want to delete this recipe?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (uid != null) {
                            // Eliminar la receta
                            recetasViewModel.eliminarReceta(uid.toString(), idReceta)
                            // eliminar de favoritos
                            if (esFavorito == true){
                                receta!!.image?.let {
                                    recetasViewModel.toggleFavorito(
                                        uid.toString(),
                                        userReceta = receta!!.userId,
                                        recetaId = idReceta,
                                        title = receta!!.title,
                                        image = it,
                                    )
                                }
                            }
                            // eliminar de historial
                            recetasViewModel.eliminarRecetaDelHistorial(uid.toString(), recetaId = idReceta)
                            //eliminar tb de Room
                            roomViewModel.deleteRecetaById(idReceta)
                            //eliminar imagen de local
                            usersViewModel.deleteImage(context, idReceta)

                            // Cerrar el dialogo
                            showDialog = false
                            // Volver a la pantalla anterior
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


//@Composable
//fun RecetaStepsWithButton(recetaDetalle: Receta) {
//    var currentStepIndex by remember { mutableStateOf(0) } // Índice del paso actual
//    var isStepsVisible by remember { mutableStateOf(false) } // Estado que controla si los pasos son visibles
//    val totalSteps = recetaDetalle.steps.size // Número total de pasos
//
//    // Barra de progreso que indica cuántos pasos faltan
//    val progress = if (totalSteps > 0) currentStepIndex / totalSteps.toFloat() else 0f
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//    ) {
//        // Botón para mostrar los pasos
//        Button(
//            onClick = { isStepsVisible = true },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = !isStepsVisible // Habilitar solo si los pasos no están visibles
//        ) {
//            Text("Show steps")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Si los pasos están visibles, mostramos el paso actual
//        if (isStepsVisible) {
//            // Barra de progreso
//            LinearProgressIndicator(
//                progress = { progress },
//                modifier = Modifier.fillMaxWidth(),
//                color = LightYellow,
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Mostrar el paso actual
//            if (currentStepIndex < totalSteps) {
//                Text(
//                    text = "${currentStepIndex + 1}. ${recetaDetalle.steps[currentStepIndex]}",
//                    style = MaterialTheme.typography.bodyMedium,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )
//            }
//
//            // Botón para retroceder al paso anterior
//            if (currentStepIndex > 0) {
//                Button(
//                    onClick = { currentStepIndex-- },
//                    modifier = Modifier.padding(top = 16.dp)
//                ) {
//                    Text("Back")
//                }
//            }
//
//            // Botón para adelantar al siguiente paso
//            if (currentStepIndex < totalSteps - 1) {
//                Button(
//                    onClick = { currentStepIndex++ },
//                    modifier = Modifier.padding(top = 16.dp)
//                ) {
//                    Text("Next")
//                }
//            } else {
//                // Si ya se han mostrado todos los pasos, botón de reiniciar
//                Button(
//                    onClick = {
//                        currentStepIndex = 0 // Resetear los pasos
//                        isStepsVisible = false // Ocultar los pasos
//                    },
//                    modifier = Modifier.padding(top = 16.dp)
//                ) {
//                    Text("Empezar de nuevo")
//                }
//            }
//        }
//    }
//}

