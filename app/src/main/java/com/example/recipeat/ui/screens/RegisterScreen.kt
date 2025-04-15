package com.example.recipeat.ui.screens

import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.R
import com.example.recipeat.ui.theme.Cherry

import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.auth.FirebaseAuth


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel,
    planViewModel: PlanViewModel
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val size = if (isPortrait) 0.8f else 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Si está en modo Portrait
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "recipEat",
                    modifier = Modifier
                        .size(135.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Mostrar los campos de entrada
                InputFields(
                    username = username,
                    onUsernameChange = { username = it },
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    errorMessage = errorMessage,
                    onErrorMessageChange = { errorMessage = it },
                    size = size
                )

                // Mostrar los botones
                Buttons(
                    navController = navController,
                    usersViewModel = usersViewModel,
                    recetasViewModel = recetasViewModel,
                    ingredientesViewModel = ingredientesViewModel,
                    planViewModel,
                    email = email,
                    password = password,
                    username = username,
                    errorMessage = errorMessage,
                    onErrorMessageChange = { errorMessage = it },
                    size = size
                )
            }
        } else {
            // Si está en modo Landscape
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "recipEat",
                    modifier = Modifier
                        .size(135.dp)
                        .align(Alignment.CenterVertically)
                )

                Spacer(modifier = Modifier.width(32.dp))


                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Mostrar los campos de entrada
                    InputFields(
                        username = username,
                        onUsernameChange = { username = it },
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        errorMessage = errorMessage,
                        onErrorMessageChange = { errorMessage = it },
                        size = size
                    )

                    // Mostrar los botones
                    Buttons(
                        navController = navController,
                        usersViewModel = usersViewModel,
                        recetasViewModel = recetasViewModel,
                        ingredientesViewModel = ingredientesViewModel,
                        planViewModel,
                        email = email,
                        password = password,
                        username = username,
                        errorMessage = errorMessage,
                        onErrorMessageChange = { errorMessage = it },
                        size = size
                    )

                    Spacer(modifier = Modifier.width(30.dp))
                }
            }
        }
    }
}

// Composable para mostrar los campos de entrada
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String,
    onErrorMessageChange: (String) -> Unit,
    size: Float
) {
    OutlinedTextField(
        value = username,
        onValueChange = {
            onUsernameChange(it)
            onErrorMessageChange("")
        },
        label = { Text("Username", style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(size),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.DarkGray,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray
        ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        textStyle = MaterialTheme.typography.bodyMedium
    )

    OutlinedTextField(
        value = email,
        onValueChange = {
            onEmailChange(it)
            onErrorMessageChange("")
        },
        label = { Text("Email", style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(size),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.DarkGray,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray
        ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        textStyle = MaterialTheme.typography.bodyMedium
    )

    OutlinedTextField(
        value = password,
        onValueChange = {
            onPasswordChange(it)
            onErrorMessageChange("")
        },
        label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(size),
        visualTransformation = PasswordVisualTransformation(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.DarkGray,
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )

    if (errorMessage.isNotEmpty()) {
        Text(
            text = errorMessage,
            color = Color.Red,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Composable para los botones de registro y de regreso
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Buttons(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel,
    planViewModel: PlanViewModel,
    email: String,
    password: String,
    username: String,
    errorMessage: String,
    onErrorMessageChange: (String) -> Unit,
    size: Float
) {
    val context = LocalContext.current

    Button(
        onClick = {
            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                onErrorMessageChange("Complete all the fields")
            } else {
                usersViewModel.register(username, email, password,
                    onResult = { result ->
                        if (result == "success") {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                //recetasViewModel.verificarRecetasGuardadasApi()
                                //ingredientesViewModel.extraerIngredientesYGuardar()
                                planViewModel.iniciarGeneracionPlanSemanalInicial(uid.toString())
                            }
                            onErrorMessageChange("")
                            Toast.makeText(
                                context,
                                "Registration successful",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate("login")
                        } else {
                            onErrorMessageChange(result) // aquí mostrará el mensaje de error correspondiente
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxWidth(size),
        colors = ButtonDefaults.buttonColors(
            containerColor = LightYellow,
            contentColor = Color.Black
        )
    ) {
        Text("Register", style = MaterialTheme.typography.bodyMedium)
    }

    Button(
        onClick = {
            navController.navigate("login")
        },
        modifier = Modifier.fillMaxWidth(size),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cherry,
            contentColor = Color.Black
        )
    ) {
        Text(
            "Back",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}