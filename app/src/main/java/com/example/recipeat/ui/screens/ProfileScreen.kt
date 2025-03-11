package com.example.recipeat.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.recipeat.ui.viewmodels.RecetasViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel

@Composable
fun ProfileScreen(navController: NavController,
                  usersViewModel: UsersViewModel,
                  recetasViewModel: RecetasViewModel
){
    Text(
        text = "Profile"
    )
}