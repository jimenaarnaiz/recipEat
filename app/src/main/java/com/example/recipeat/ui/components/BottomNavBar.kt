package com.example.recipeat.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.recipeat.ui.theme.LightYellow

// Función para mostrar la barra de navegación
@Composable
fun BottomNavBar(navController: NavController, visible: Boolean) {
    // Solo mostramos la barra de navegación si 'visible' es true
    if (visible) {
        val items = listOf(
            BottomNavItem.Home, // Añadimos el item Home
            BottomNavItem.MyRecipes, // Añadimos el item MyRecipes
            BottomNavItem.Profile // Añadimos el item Profile
        )

        NavigationBar {
            // Para cada item de la lista de BottomNavItem, agregamos un item de la barra
            items.forEach { item ->
                AddItem(
                    screen = item,
                    navController = navController,
                )
            }
        }
    }
}

// Función para agregar un item a la barra de navegación
@Composable
fun RowScope.AddItem(
    screen: BottomNavItem,
    navController: NavController
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val isSelected = currentDestination == screen.route

    NavigationBarItem(
        icon = { Icon(screen.icon, contentDescription = screen.title) }, // Muestra el icono del item
        label = { Text(screen.title) }, // Muestra el título del item
        selected = isSelected, // Esto puede ajustarse para manejar el estado seleccionado
        alwaysShowLabel = true, // Siempre muestra la etiqueta debajo del icono
        onClick = {
            // Navegamos a la ruta correspondiente cuando se hace click
            navController.navigate(screen.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = if (isSelected) LightYellow else MaterialTheme.colorScheme.surface, // Color de fondo del ítem seleccionado = if (isSelected) LightYellow, // Color del icono cuando está seleccionado
        )
    )
}