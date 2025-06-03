import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recipeat.R
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreen(
    navController: NavHostController,
    usersViewModel: UsersViewModel,
    recetasViewModel: RecetasViewModel,
    planViewModel: PlanViewModel
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val fieldWidth = if (isLandscape) 0.5f else 0.8f  // Ajusta el ancho según orientación
    val context = LocalContext.current

    // Interceptar el botón de "Atrás" para salir de la aplicación
    BackHandler {
        val activity = context as? Activity
        activity?.finish() // Cerrar la actividad y salir de la app
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (isLandscape) {
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

                Spacer(modifier = Modifier.width(35.dp))

                LoginForm(
                    navController,
                    recetasViewModel,
                    usersViewModel,
                    planViewModel,
                    fieldWidth,
                    email,
                    password,
                    errorMessage
                ) {
                    // para que se actualicen los cambios de una orientación a otra
                    email = it.first
                    password = it.second
                    errorMessage = it.third
                }
            }
        } else {
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

                Spacer(modifier = Modifier.height(16.dp))

                LoginForm(
                    navController,
                    recetasViewModel,
                    usersViewModel,
                    planViewModel,
                    fieldWidth,
                    email,
                    password,
                    errorMessage
                ) {
                    email = it.first
                    password = it.second
                    errorMessage = it.third
                }
            }
        }
        // Texto con créditos fijo abajo centrado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "© 2025 Jimena Arnaiz González",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginForm(
    navController: NavHostController,
    recetasViewModel: RecetasViewModel,
    usersViewModel: UsersViewModel,
    planViewModel: PlanViewModel,
    fieldWidth: Float,
    email: String,
    password: String,
    errorMessage: String,
    onInputChange: (Triple<String, String, String>) -> Unit
) {
    var localEmail by rememberSaveable { mutableStateOf(email) }
    var localPassword by rememberSaveable { mutableStateOf(password) }
    var localError by rememberSaveable { mutableStateOf(errorMessage) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = localEmail,
            onValueChange = {
                localEmail = it
                localError = ""
                onInputChange(Triple(localEmail, localPassword, localError))
            },
            label = { Text("Email", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(fieldWidth),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.DarkGray
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = localPassword,
            onValueChange = {
                localPassword = it
                localError = ""
                onInputChange(Triple(localEmail, localPassword, localError))
            },
            label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(fieldWidth),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.DarkGray,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.DarkGray
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        if (localError.isNotEmpty()) {
            Text(text = localError,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth(fieldWidth) // Mantener el mismo ancho
                    .padding(start = 10.dp, top = 4.dp) // Añadir márgenes como el OutlinedTextField
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        Button(
            onClick = {
                if (localEmail.isEmpty() || localPassword.isEmpty()) {
                    localError = "Email and Password cannot be empty."
                    recetasViewModel.logRecetasCount()
                } else {
                    usersViewModel.login(localEmail, localPassword) { resultMsg ->
                        if (resultMsg == "success") {
                            // Comprobar si tiene un plan semanal esta semana, y si no, crearlo
                            planViewModel.obtenerPlanSemanal(userId = usersViewModel.getUidValue().toString())
                            if (planViewModel.planSemanal.value == null){
                                planViewModel.iniciarGeneracionPlanSemanalInicial(usersViewModel.getUidValue().toString())
                            }

                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }

                            localPassword = "" // Aquí vaciar la contraseña

                        }else{
                            localError = resultMsg
                        }
                        onInputChange(Triple(localEmail, localPassword, localError))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(fieldWidth),
            colors = ButtonDefaults.buttonColors(containerColor = LightYellow, contentColor = Color.Black)
        ) {
            Text("Login", style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                localEmail = "" //borra en portrait
                localPassword = ""
                localError = ""
                onInputChange(Triple(localEmail, localPassword, localError)) // Borra en landscape
                navController.navigate("register")
            },
            modifier = Modifier.fillMaxWidth(fieldWidth),
            colors = ButtonDefaults.buttonColors(containerColor = Cherry, contentColor = Color.Black)
        ) {
            Text("Register", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }

        // Aquí agregamos el texto para "Olvidaste tu contraseña?"
        Text(
            text = "Forgot password?",
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline,
                color = Cherry // Cambia aquí el color del subrayado
            ),
            modifier = Modifier
                .align(Alignment.Start) // Esto asegura que el texto esté centrado
                .padding(top = 10.dp)
                .clickable { navController.navigate("forgotPassword") }
        )

    }
}

