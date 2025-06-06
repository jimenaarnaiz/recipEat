package com.example.recipeat.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeat.R
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.auth.FirebaseAuth


@Composable
fun ProfileScreen(
    navController: NavController,
    usersViewModel: UsersViewModel,
    connectivityViewModel: ConnectivityViewModel,
    recetaRoomViewModel: RoomViewModel
) {
    val uid = usersViewModel.getUidValue()
    var usernameState by rememberSaveable { mutableStateOf<String?>(null) }
    var profileImageState by rememberSaveable { mutableStateOf<String?>(null) }

    var bitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    // Obtener los datos desde Firestore
    LaunchedEffect(Unit) {
        Log.d("ProfileScreen", "uid = $uid")

            usersViewModel.obtenerUsuarioCompletoPorCampos(uid.toString()) { username, profileImageUrl, email ->
                usernameState = username
                profileImageState = profileImageUrl
            }

            bitmap = usersViewModel.loadImageFromFile(context, null)
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 25.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp), // Más espaciado general
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            bitmap?.let {
                Log.d("ProfileScreen", "ImageLoading...Bitmap cargado: $it")
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .shadow(4.dp, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Log.d("ProfileScreen", "Usando imagen por defecto")
                // Muestra una imagen por defecto si no hay imagen seleccionada
                Image(
                    painter = painterResource(id = R.drawable.profile_avatar_placeholder),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .shadow(4.dp, CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // Nombre de usuario
            usernameState?.let {
                Text(
                    text = it,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Lista de opciones (Editar, Historial, Favoritos y Logout) con tarjetas
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                    ProfileCard(
                        title = "Edit profile",
                        icon = Icons.Default.Edit,
                        onClick = {
                            navController.navigate("editarPerfil")
                        },
                        backgroundColor = LightYellow,
                        textColor = Color.Black,
                        isConnected
                    )

                    ProfileCard(
                        title = "History",
                        icon = Icons.Filled.History,
                        onClick = { navController.navigate("historial") },
                        backgroundColor = LightYellow,
                        textColor = Color.Black,
                        isConnected
                    )

                    ProfileCard(
                        title = "Favorites",
                        icon = Icons.Outlined.Favorite,
                        onClick = {
                            recetaRoomViewModel.sincronizarFavoritosDesdeFirebase(uid.toString())
                            navController.navigate("favoritos");
                                  },
                        backgroundColor = LightYellow,
                        textColor = Color.Black,
                        true
                    )

                    ProfileCard(
                        title = "Log Out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            usersViewModel.logOut()

                            navController.navigate("login") {
                                // Borra toda la pila de navegación
                                popUpTo("login") { inclusive = true } // Elimina todas las pantallas previas a login, incluyendo el profile
                                launchSingleTop = true // Evitar que se creen múltiples instancias del login
                            }
                        },
                        backgroundColor = Color.Red,
                        textColor = Color.White,
                        isConnected
                    )

                Spacer(modifier = Modifier.height(72.dp)) // Espacio inferior para q no lo opaque el bttom bar
            }
        }
    }

@Composable
fun ProfileCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Alineación y espaciado de las tarjetas
            .clickable(enabled = isConnected) { onClick() } // Deshabilitar clic si no hay conexión
            .shadow(8.dp, RoundedCornerShape(12.dp)), // Sombra suave y borde redondeado
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isConnected) backgroundColor else Color.LightGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Ajustamos el padding para que las tarjetas no sean tan altas
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp) // El texto alineado a la izquierda
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp), // Tamaño del ícono
                    tint = textColor
                )

                Spacer(modifier = Modifier.width(16.dp)) // Espacio entre el ícono y el texto

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}