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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.TopBarWithIcons
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel

@Composable
fun DetailsScreen(
    idReceta: String,
    navController: NavHostController,
    recetasViewModel: RecetasViewModel,
    esDeUser: Boolean,
    roomViewModel: RoomViewModel,
    usersViewModel: UsersViewModel,
    connectivityViewModel: ConnectivityViewModel
) {
    val receta by recetasViewModel.recetaSeleccionada.observeAsState()
    val uid = usersViewModel.getUidValue()
    val esFavorito by recetasViewModel.esFavorito.observeAsState()

    // Estado para mostrar el AlertDialog de confirmación de eliminación
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

   val recetaRoom by roomViewModel.recipeRoom.observeAsState()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

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

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            showDialog= false
        }
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
        // Usamos receta si está conectado, si no, usamos recetaRoom
        val recetaShowed = if (isConnected && receta != null) receta else recetaRoom

        recetaShowed?.let { recetaDetalle ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {

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
                                if (esFavorito == false) { // si no es fav, se añade al Room
                                    //receta!!.esFavorita = true
                                    roomViewModel.insertReceta(receta!!)
                                    roomViewModel.agregarFavorito(uid.toString(), idReceta)
                                }else{
                                    if (esDeUser) { //si es creada por el user, solo pone a false favs
                                        roomViewModel.eliminarFavorito(uid.toString(), idReceta)
                                    }else{ // si es de la api, se elimina
                                        roomViewModel.eliminarFavorito(uid.toString(), idReceta)

                                        val esDelHome = roomViewModel.esRecetaDelHome(context, uid.toString(), idReceta)
                                        if (!esDelHome) {
                                            roomViewModel.deleteRecetaById(uid.toString(), idReceta)
                                        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = "https://spoonacular.com/cdn/ingredients_100x100/${ingredient.image}",
                            error = painterResource(R.drawable.ingredient_placeholder),
                            contentDescription = ingredient.name,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                        )
                        Text(
                            text = " ${ingredient.name} (${ingredient.amount} ${ingredient.unit})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            //eliminar tb de Room (recetas y favs)
                            roomViewModel.deleteRecetaById(uid.toString(), idReceta)
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


