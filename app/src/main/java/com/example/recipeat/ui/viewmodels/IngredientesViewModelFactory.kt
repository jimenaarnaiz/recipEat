package com.example.recipeat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore

class IngredientesViewModelFactory(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IngredientesViewModel::class.java)) {
            return IngredientesViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
