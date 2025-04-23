package com.example.recipeat.ui.screens.search

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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeat.ui.components.AppBar
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.viewmodels.ConnectivityViewModel
import com.example.recipeat.ui.viewmodels.IngredientesViewModel
import com.example.recipeat.ui.viewmodels.RecetasViewModel

@Composable
fun UnifiedSearchScreen(
    navController: NavController,
    recetasViewModel: RecetasViewModel,
    ingredientesViewModel: IngredientesViewModel,
    connectivityViewModel: ConnectivityViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Ingredientes, 1: Nombre

    // Observamos el estado de conectividad
    val isConnected by connectivityViewModel.isConnected.observeAsState(false)


    Scaffold(
        topBar = {
            AppBar(
                title = "",
                onBackPressed = {
                    recetasViewModel.restablecerRecetasSugeridas()
                    ingredientesViewModel.clearIngredientes()
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
                0 -> IngredientsSearch(navController, ingredientesViewModel, isConnected)
                1 -> NameSearch(navController, recetasViewModel, isConnected)
            }
        }
    }
}