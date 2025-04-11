package com.example.recipeat.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.utils.NetworkConnectivityManager
import com.google.firebase.auth.FirebaseAuth


@Composable
fun StepsScreen(
    idReceta: String,
    navController: NavHostController,
    recetasViewModel: RecetasViewModel,
    deUser: Boolean,
) {
    val receta by recetasViewModel.recetaSeleccionada.observeAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val equipmentImages by recetasViewModel.equipmentSteps.observeAsState()

    var cocinado by rememberSaveable { mutableStateOf(false) }
    var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = receta?.steps?.size ?: 0
    val progress = if (totalSteps > 0) (currentStepIndex + 1) / totalSteps.toFloat() else 0f

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Instanciar el NetworkConnectivityManager
    val context = LocalContext.current
    val networkConnectivityManager = remember { NetworkConnectivityManager(context) }

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

    // Verificar si hay conexión
    val isConnected = networkConnectivityManager.isConnected.value


    LaunchedEffect(navController) {
        if (uid != null) {
            recetasViewModel.obtenerRecetaPorId(uid, idReceta, deUser)
        }
    }

    LaunchedEffect(receta) {
        receta?.let { recetasViewModel.updateEquipmentSteps(it.steps) }
    }

    Scaffold(
        topBar = {
            if (!isLandscape) {
                AppBar(
                    title = "",
                    navController = navController,
                    onBackPressed = { navController.popBackStack() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${currentStepIndex + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Cherry,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (currentStepIndex < totalSteps) {
                    Text(
                        text = "${receta?.steps?.get(currentStepIndex)}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    equipmentImages?.getOrNull(currentStepIndex)?.take(3)?.let { images ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            images.forEach { imageUrl ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUrl),
                                        contentDescription = "Equipment Image",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentStepIndex > 0 && !cocinado) {
                    Button(
                        onClick = { currentStepIndex-- },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Back")
                    }
                }
                if (currentStepIndex < totalSteps - 1) {
                    Button(
                        onClick = { currentStepIndex++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        enabled = isConnected,
                        onClick = {
                            if (!cocinado) {
                                receta?.image?.let {
                                    recetasViewModel.añadirHistorial(
                                        uid.toString(),
                                        receta!!.userId,
                                        idReceta,
                                        receta!!.title,
                                        it,
                                    )
                                }
                                cocinado = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!cocinado) LightYellow else Cherry
                        )
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
            }
        }
    }
}
