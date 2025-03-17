package com.example.recipeat.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.R

import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavHostController,
                   usersViewModel: UsersViewModel,
                   recetasViewModel: RecetasViewModel,
                   ingredientesViewModel: IngredientesViewModel
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
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
            value = username,
            onValueChange = {
                username = it
                errorMessage = ""
            },
            label = { Text("Username", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                unfocusedLabelColor = Color.Gray   // Color de la etiqueta cuando no está enfocado
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next // Mueve al siguiente campo
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = ""
            },
            label = { Text("Email", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                unfocusedLabelColor = Color.Gray   // Color de la etiqueta cuando no está enfocado
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
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.DarkGray,    // Color del borde cuando está enfocado
                unfocusedBorderColor = Color.Gray, // Color del borde cuando no está enfocado
                focusedLabelColor = Color.DarkGray,     // Color de la etiqueta cuando está enfocado
                unfocusedLabelColor = Color.Gray   // Color de la etiqueta cuando no está enfocado
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
                if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                    errorMessage = "Complete all the fields"
                } else {
                    usersViewModel.register(username, email, password,
                        onResult = { success ->
                            if (success) {
                                //guardar 100 recetas para el user
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (uid != null) {
                                    recetasViewModel.verificarRecetasGuardadas(uid) //TODO cada dia puedo 100
                                    ingredientesViewModel.extraerIngredientesYGuardar()
                                }
                                errorMessage = ""
                                Toast.makeText(context, "Register successful", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            } else {
                                //Toast.makeText(context, "Register failed", Toast.LENGTH_SHORT).show()
                                errorMessage = "Register failed"
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
            Text("Register", style = MaterialTheme.typography.bodyMedium)
        }



    }



}
