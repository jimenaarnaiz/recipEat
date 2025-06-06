package com.example.recipeat.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.R
import com.example.recipeat.data.model.User
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.PermissionsViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel


@Composable
fun EditProfileScreen(
    navController: NavController,
    usersViewModel: UsersViewModel,
    connectivityViewModel: ConnectivityViewModel,
    permissionsViewModel: PermissionsViewModel
) {

    val uid = usersViewModel.getUidValue()
    val hasStoragePermission = permissionsViewModel.storagePermissionGranted.value

    // Estado para almacenar el objeto User
    var userState by remember { mutableStateOf<User?>(null) }

    var newUsername by remember { mutableStateOf("") }
    var newEmail by rememberSaveable { mutableStateOf("") }
    //val newImage by rememberSaveable { mutableStateOf("") }

    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    var eliminarFotoPerfil by remember { mutableStateOf(false) }

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)

    // Obtener los datos desde Firestore cuando la pantalla esté lanzada
    LaunchedEffect(Unit) {
        if (uid != null) {
            usersViewModel.obtenerUsuarioCompleto(uid) { user ->
                // Actualizar el estado con el objeto User obtenido
                userState = user
                newUsername = user?.username ?: ""
                newEmail = user?.email ?: ""
                bitmap = usersViewModel.loadImageFromFile(context, null)
            }

        }
    }

    // Photo picker (1 pic)
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            imageUri = uri
            usersViewModel.saveImageLocally(context, uri, "tmp_image")
            bitmap = usersViewModel.loadImageFromFile(context, "tmp_image")
            //newImage = uri.toString()
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }


    Scaffold(
        topBar = {
            AppBar(
                title = "",
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


            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                bitmap?.let {
                    Log.d("EditProfileScreen", "ImageLoading...Bitmap cargado: ${it}")
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Imagen de perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .shadow(4.dp, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Log.d("EditProfileScreen", "Usando imagen por defecto")
                    Image(
                        painter = painterResource(id = R.drawable.profile_avatar_placeholder),
                        contentDescription = "Imagen de perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .shadow(4.dp, CircleShape)
                    )
                }

                if (bitmap != null && isConnected && hasStoragePermission) {
                    IconButton(
                        onClick = {
                            bitmap = null
                            imageUri = null
                            eliminarFotoPerfil = true
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp) // Superposición ligera, fuera del borde de la imagen
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Profile Picture",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }


            // Botón para elegir una nueva imagen de perfil
            Button(
                enabled = isConnected && hasStoragePermission, //si tiene conexion y permisos
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    eliminarFotoPerfil = false
                          },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightYellow,
                    contentColor = Color.Black
                )
            ) {
                Text("Change Profile Picture")
            }

            // Mostrar mensaje si el permiso de almacenamiento no está concedido o es limitado
            if (!hasStoragePermission) {
                Text(
                    text = "You need to grant storage permission to change your profile picture.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para cambiar el nombre de usuario
            TextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isConnected
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

            //Botón para guardar los cambios
            Button(
                enabled = isConnected,
                onClick = {
                    if (uid != null) {
                        if (eliminarFotoPerfil) {
                            // Eliminar imagen local y resetear bitmap e imageUri
                            usersViewModel.deleteImage(context, null) // borrar img perfil
                        }

                        imageUri?.let { usersViewModel.saveImageLocally(context, it, null) }
                        usersViewModel.actualizarUserProfile(
                            newUsername = newUsername,
                            newProfileImage = bitmap.toString(),
                            onResult = { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Profile updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
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