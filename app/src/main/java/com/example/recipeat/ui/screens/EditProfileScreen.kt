package com.example.recipeat.ui.screens



import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.R
import com.example.recipeat.data.model.User
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.components.BottomNavItem
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.UsersViewModel


@Composable
fun EditProfileScreen(navController: NavController, usersViewModel: UsersViewModel) {

    val uid = usersViewModel.getUidValue()

    // Estado para almacenar el objeto User
    var userState by remember { mutableStateOf<User?>(null) }

    var newUsername by remember { mutableStateOf("") }
    var newEmail by rememberSaveable { mutableStateOf("") }
    val newImage by rememberSaveable { mutableStateOf("") }

    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current


    // Obtener los datos desde Firestore cuando la pantalla esté lanzada
    LaunchedEffect(Unit) {
        if (uid != null) {
            usersViewModel.obtenerUsuarioCompleto(uid) { user ->
                // Actualizar el estado con el objeto User obtenido
                userState = user
                newUsername = user?.username ?: ""
                newEmail = user?.email ?: ""
                //newImage = user?.image ?: ""
                bitmap = usersViewModel.loadImageFromFile(context, null)
            }
        }
    }

    // Photo picker (1 pic)
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            imageUri = uri
            usersViewModel.saveImageLocally(context, uri, null)
            bitmap = usersViewModel.loadImageFromFile(context, null)
            //newImage = uri.toString()
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }


    Scaffold(
        topBar = {
            AppBar(
                title = "",
                navController = navController,
                onBackPressed = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp), // Ajustado el padding para mayor espacio
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))


            bitmap?.let {
                Log.d("EditProfileScreen", "ImageLoading...Bitmap cargado: ${it}")
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Imagen de perfil",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .shadow(4.dp, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Log.d("EditProfileScreen", "Usando imagen por defecto")
                // Muestra una imagen por defecto si no hay imagen seleccionada
                Image(
                    painter = painterResource(id = R.drawable.profile_avatar_placeholder),
                    contentDescription = "Imagen de perfil",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .shadow(4.dp, CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // Botón para elegir una nueva imagen de perfil
            Button(
                onClick = {
                pickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                ) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightYellow,
                    contentColor = Color.Black
                )
            ) {
                Text("Change Profile Picture")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para cambiar el nombre de usuario
            TextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo para cambiar el email
            TextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            Spacer(modifier = Modifier.height(8.dp))


            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            //Botón para guardar los cambios
            Button(
                onClick = {
                    if (uid != null) {
                        usersViewModel.actualizarUserProfile(
                            newUsername = newUsername,
                            newProfileImage = newImage,
                            onResult = { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Profile updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate(BottomNavItem.Profile.route)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error updating profile",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cherry,
                    contentColor = Color.White
                )
            ) {
                Text("Save Changes")
            }
        }
    }
}