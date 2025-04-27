package com.example.recipeat.ui.screens

import android.content.res.Configuration
import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.R

import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.UsersViewModel

@Composable
fun ForgotPasswordScreen(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val btnSize = if (isPortrait) 0.8f else 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (isPortrait) {
            // Modo Portrait
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

                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Usamos InputField
                InputField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (errorMessage.isNotBlank()) {
                            errorMessage = ""
                        }
                    },
                    label = "Email",
                    isError = errorMessage.isNotEmpty(),
                    errorMessage = errorMessage,
                    modifier = Modifier.fillMaxWidth(btnSize)
                )

                // Botón de "Send Link"
                CustomButton(
                    text = "Send Link",
                    onClick = {
                        if (email.isNotEmpty()) {
                            usersViewModel.sendPasswordResetEmail(email) { resultMsg ->
                                if (resultMsg == "success") {
                                    Toast.makeText(context, "A reset link has been sent to your email.", Toast.LENGTH_SHORT).show()
                                    errorMessage = ""
                                    navController.popBackStack()
                                } else {
                                    errorMessage = resultMsg
                                }
                            }
                        } else {
                            errorMessage = "Please enter your email."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(btnSize)
                )

                // Botón de "Go to Login"
                CustomButton(
                    text = "Go to Login",
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(btnSize),
                    backgroundColor = Cherry,
                    textColor = Color.White
                )
            }
        } else {
            // Modo Landscape
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
                    // Usamos InputField en modo Landscape
                    InputField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (errorMessage.isNotBlank()) {
                                errorMessage = ""
                            }
                        },
                        label = "Email",
                        isError = errorMessage.isNotEmpty(),
                        errorMessage = errorMessage,
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )

                    // Botón de "Send Link"
                    CustomButton(
                        text = "Send Link",
                        onClick = {
                            if (email.isNotEmpty()) {
                                usersViewModel.sendPasswordResetEmail(email) { resultMsg ->
                                    if (resultMsg == "success") {
                                        Toast.makeText(context, "A reset link has been sent to your email.", Toast.LENGTH_SHORT).show()
                                        errorMessage = ""
                                        navController.popBackStack()
                                    } else {
                                        errorMessage = resultMsg
                                    }
                                }
                            } else {
                                errorMessage = "Please enter your email."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )

                    // Botón de "Go to Login"
                    CustomButton(
                        text = "Go to Login",
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(0.5f),
                        backgroundColor = Cherry,
                        textColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.DarkGray
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // Mostrar mensaje de error si existe
        if (isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}


@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LightYellow,
    textColor: Color = Color.Black
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
