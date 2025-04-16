package com.example.recipeat.ui.viewmodels

import com.example.recipeat.utils.NetworkConnectivityManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * ViewModel responsable de observar los cambios en la conectividad de red.
 * Expone un LiveData<Boolean> llamado `isConnected` que se actualiza automáticamente
 * cuando cambia el estado de la red (conectado o no).
 *
 * Utiliza NetworkConnectivityManager para registrar y manejar callbacks de red.
 */
class ConnectivityViewModel(application: Application) : AndroidViewModel(application) {

    private val networkConnectivityManager: NetworkConnectivityManager =
        NetworkConnectivityManager(application)

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> get() = _isConnected

    init {
        // Registrar el callback de la conectividad de red solo una vez cuando el ViewModel se crea.
        networkConnectivityManager.registerNetworkCallback()

        // Observamos los cambios en la conectividad de red y actualizamos el LiveData.
        networkConnectivityManager.onConnectivityChanged = { isConnected ->
            _isConnected.postValue(isConnected)
        }
    }

    // Desregistrar el callback cuando el ViewModel se destruya.
    override fun onCleared() {
        super.onCleared()
        networkConnectivityManager.unregisterNetworkCallback()
    }
}
