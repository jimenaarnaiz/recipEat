package com.example.recipeat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.R
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun LoginScreen(navController: NavHostController, usersViewModel: UsersViewModel, recetasViewModel: RecetasViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 100.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp), // Espaciado entre elementos
        horizontalAlignment = Alignment.CenterHorizontally // Alineación horizontal
    ) {

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "recipEat",
            modifier = Modifier
                .size(135.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(2.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it
                errorMessage = ""
            },
            label = { Text("Email", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next // Mueve al siguiente campo
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(0.8f),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        if (errorMessage.isNotEmpty()){
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Email and Password cannot be empty."
                    //recetasViewModel.buscarRecetasPorIngredientes() //TODO
                }else{
                    usersViewModel.login(email, password,
                        onResult = { success ->
                            if (success) {
                                errorMessage = ""
                                // Navegar a la pantalla Home
                                navController.navigate("home")
                            }else{
                                errorMessage = "Incorrect email or password"
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightYellow,
                contentColor = Color.Black
            )
        ) {
            Text("Login", style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Cherry,
                contentColor = Color.Black
            )
        ) {
            Text(
                "Register",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }


    }
}