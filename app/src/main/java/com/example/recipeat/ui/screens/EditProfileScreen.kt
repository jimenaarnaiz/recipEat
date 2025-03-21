package com.example.recipeat.ui.screens



import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.recipeat.R
import com.example.recipeat.data.model.User
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.auth.FirebaseAuth


@Composable
fun EditProfileScreen(navController: NavController, usersViewModel: UsersViewModel) {

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var usernameState by remember { mutableStateOf<String?>(null) }
    var profileImageState by remember { mutableStateOf<String?>(null) }

    // Obtener los datos desde Firestore
    LaunchedEffect(uid) {
        if (uid != null) {
            println( "current user edit profile screen $uid")
            usersViewModel.obtenerUsuarioCompleto(uid) { username, profileImageUrl, email ->
                usernameState = username
                profileImageState = profileImageUrl
            }
        }
    }



//    val uid = userId
//
//    val uidAuth = FirebaseAuth.getInstance().currentUser?.uid
//
//
//    // Estado para almacenar el objeto User
//    var userState by rememberSaveable { mutableStateOf<User?>(null) }
//
//    var newUsername by rememberSaveable { mutableStateOf("") }
//    var newEmail by rememberSaveable { mutableStateOf("") }
//    var newImage by rememberSaveable { mutableStateOf("") }
//    var newPassword by rememberSaveable { mutableStateOf("") }
//
//
//    // Obtener los datos desde Firestore cuando la pantalla esté lanzada
//    LaunchedEffect(userId) {
//        usersViewModel.obtenerUsuarioCompleto(uid) { user ->
//            // Actualizar el estado con el objeto User obtenido
//            userState = user
//            newUsername = user?.username ?: ""
//            newEmail = user?.email ?: ""
//            newImage = user?.image ?: ""
//        }
//    }
//
//    println( "current user edit profile screen $uidAuth")

//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp), // Ajustado el padding para mayor espacio
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Imagen de perfil con borde sutil y efecto de sombra
//        if (userState?.image.isNullOrEmpty()) {
//            Image(
//                painter = painterResource(id = R.drawable.profile_avatar_placeholder),
//                contentDescription = "Profile picture",
//                modifier = Modifier
//                    .padding(16.dp)
//                    .size(120.dp)
//                    .clip(CircleShape)
//                    .shadow(4.dp, CircleShape)
//                    .align(Alignment.CenterHorizontally)
//            )
//        } else {
//            AsyncImage(
//                model = newImage,
//                contentDescription = "Profile Image",
//                modifier = Modifier
//                    .padding(16.dp)
//                    .size(120.dp)
//                    .clip(CircleShape)
//                    .shadow(4.dp, CircleShape)
//                    .align(Alignment.CenterHorizontally)
//            )
//        }
//
//        // Botón para elegir una nueva imagen de perfil
//            Button(onClick = {
//                // Aquí agregar la lógica para seleccionar una nueva imagen
//                // Utilizar una librería como `Accompanist` para acceder a la galería
//            }) {
//                Text("Change Profile Picture")
//            }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Campo para cambiar el nombre de usuario
//        TextField(
//            value = newUsername,
//            onValueChange = { newUsername = it },
//            label = { Text("Username") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Campo para cambiar el email
//        TextField(
//            value = newEmail,
//            onValueChange = { newEmail = it },
//            label = { Text("Email") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Campo para cambiar la contraseña
//        TextField(
//            value = newPassword,
//            onValueChange = { newPassword = it },
//            label = { Text("New Password") },
//            modifier = Modifier.fillMaxWidth(),
//            visualTransformation = PasswordVisualTransformation()
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        val context = LocalContext.current
//        //Botón para guardar los cambios
//        Button(
//            onClick = {
//                usersViewModel.updateUserProfile(
//                    uid = uid,
//                    newUsername = newUsername,
//                    newEmail = newEmail,
//                    newPassword = newPassword,
//                    newProfileImage = newImage,
//                    onResult = { success ->
//                        if (success) {
//                            Toast.makeText(context, "Perfil actualizado" ,Toast.LENGTH_SHORT).show()
//                        } else {
//                            Toast.makeText(context, "Error al actualizar perfil" ,Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                )
//            }
//        ) {
//            Text("Save Changes")
//        }
//    }
}