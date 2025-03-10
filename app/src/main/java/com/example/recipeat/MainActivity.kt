package com.example.recipeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.recipeat.ui.components.BottomNavBar
import com.example.recipeat.ui.theme.RecipEatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecipEatTheme {
                val navController = rememberNavController() // Un solo NavController
                // Creamos un estado mutable para controlar la visibilidad de la BottomNavBar
                val showBottomNav = remember { mutableStateOf(true) }

                Scaffold(
                    modifier = Modifier
//                        .fillMaxSize(),
                        .statusBarsPadding(),
                    bottomBar = {
                        // Solo se muestra la barra de navegación si 'showBottomNav' es true
                        if (showBottomNav.value) {
                            BottomNavBar(navController = navController, visible = true)
                        }
                    }
                ) { innerPadding ->
                    // Llamamos a NavigationGraph y pasamos la función para manejar la visibilidad de la barra
                    NavigationGraph(navController) { visible ->
                        showBottomNav.value = visible // Actualiza el estado de visibilidad
                    }
                    //Modifier.padding(top = innerPadding.calculateTopPadding())
                }
            }
        }
    }
}

