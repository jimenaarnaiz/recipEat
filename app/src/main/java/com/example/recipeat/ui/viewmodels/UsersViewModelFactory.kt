package com.example.recipeat.ui.viewmodels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recipeat.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fábrica personalizada para crear instancias de UsersViewModel
 * y proporcionarle la dependencia UserRepository.
 */
class UsersViewModelFactory(
    private val application: Application,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {

    /**
     * Métdo que crea el ViewModel, en este caso UsersViewModel.
     *
     * @param modelClass La clase del ViewModel que se quiere crear.
     * @return Una instancia del ViewModel solicitada.
     * @throws IllegalArgumentException Si la clase del ViewModel no es compatible.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Verifica si la clase del ViewModel solicitada es UsersViewModel.
        if (modelClass.isAssignableFrom(UsersViewModel::class.java)) {
            // Crear las dependencias necesarias para UserRepository
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val userRepository = UserRepository(sharedPreferences, auth, db)

            // Crear e instanciar el ViewModel con las dependencias correctas.
            @Suppress("UNCHECKED_CAST")  // Suprime la advertencia de cast no comprobado.
            return UsersViewModel(application, userRepository) as T
        }

        // Si el tipo del ViewModel no es compatible, lanza una excepción.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
