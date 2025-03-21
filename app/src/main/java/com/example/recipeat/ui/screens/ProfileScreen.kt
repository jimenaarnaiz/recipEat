package com.example.recipeat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.recipeat.R
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.auth.FirebaseAuth


@Composable
fun ProfileScreen(navController: NavController, usersViewModel: UsersViewModel) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var usernameState by remember { mutableStateOf<String?>(null) }
    var profileImageState by remember { mutableStateOf<String?>(null) }

    // Obtener los datos desde Firestore
    LaunchedEffect(Unit) {
        if (uid != null) {
            usersViewModel.obtenerUsuarioCompleto(uid) { username, profileImageUrl, email ->
                usernameState = username
                profileImageState = profileImageUrl
            }
        }
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                //.padding(paddingValues),
                .padding(8.dp), // Más espaciado general
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Imagen de perfil con borde sutil y efecto de sombra
            if (profileImageState.isNullOrEmpty()) {
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
            } else {
                AsyncImage(
                    model = profileImageState,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .shadow(4.dp, CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre de usuario estilizado
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

            // Lista de opciones (Historial y Favoritos) con tarjetas elegantes
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OptionCard(
                        title = "Edit profile",
                        icon = Icons.Default.Edit,
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("editarPerfil")
                        },
                        backgroundColor = LightYellow,
                        textColor = Color.Black
                    )
                }

                item {
                    OptionCard(
                        title = "History",
                        icon = Icons.Filled.History,
                        onClick = { navController.navigate("historial") },
                        backgroundColor = LightYellow,
                        textColor = Color.Black
                    )
                }

                item {
                    OptionCard(
                        title = "Favorites",
                        icon = Icons.Outlined.Favorite,
                        onClick = { navController.navigate("favoritos") },
                        backgroundColor = LightYellow,
                        textColor = Color.Black
                    )
                }

                item {
                    OptionCard(
                        title = "Log Out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("profile") { inclusive = true } // para eliminar de la pila las anteriores screens
                            }
                        },
                        backgroundColor = Color.Red,
                        textColor = Color.White
                    )
                }
            }
        }
    }

@Composable
fun OptionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Alineación y espaciado de las tarjetas
            .clickable { onClick() }
            .shadow(8.dp, RoundedCornerShape(12.dp)), // Sombra suave y borde redondeado
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Ajustamos el padding para que las tarjetas no sean tan altas
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(start = 16.dp) // El texto alineado a la izquierda
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