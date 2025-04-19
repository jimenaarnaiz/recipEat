package com.example.recipeat.ui.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recipeat.data.repository.RecetaRoomRepository
import com.example.recipeat.ui.viewmodels.RoomViewModel

/**
 * Fábrica personalizada para crear instancias de RoomViewModel
 * y proporcionarle la dependencia RecetaRepository.
 */
class RoomViewModelFactory(private val recetaRoomRepository: RecetaRoomRepository) : ViewModelProvider.Factory {

    /**
     * Métdo que crea el ViewModel, en este caso RoomViewModel.
     *
     * @param modelClass La clase del ViewModel que se quiere crear.
     * @return Una instancia del ViewModel solicitada.
     * @throws IllegalArgumentException Si la clase del ViewModel no es compatible.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Verifica si la clase del ViewModel solicitada es RoomViewModel.
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            // Si es del tipo esperado, se crea el ViewModel y se le pasa el recetaRepository como dependencia.
            @Suppress("UNCHECKED_CAST")  // Suprime la advertencia de cast no comprobado.
            return RoomViewModel(recetaRoomRepository) as T
        }

        // Si el tipo del ViewModel no es compatible, lanza una excepción.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
