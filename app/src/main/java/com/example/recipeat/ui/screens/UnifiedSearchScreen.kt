package com.example.recipeat.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.FiltrosViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@Composable
fun UnifiedSearchScreen(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel,
    filtrosViewModel: FiltrosViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ingredientes, 1: Nombre

    Scaffold(
        topBar = {
            AppBar(
                title = "",
                navController = navController,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            // Tabs para cambiar entre los métodos de búsqueda
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .width(340.dp),
                contentColor = Color.Black, // text color
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Cherry // Cambia este color según tu tema
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("By Ingredients", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("By Name", modifier = Modifier.padding(16.dp))
                }
            }

            // Mostrar la vista correspondiente
            when (selectedTab) {
                0 -> IngredientsSearchScreen(navController, recetasViewModel, ingredientesViewModel)
                1 -> NameSearchScreen(navController, recetasViewModel)
            }
        }
    }
}