package com.example.recipeat.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.mutableStateOf

/**
 * Esta clase gestiona la conectividad de la red en la aplicación y proporciona un estado
 * sobre si la conexión a Internet está disponible o no.
 */
class NetworkConnectivityManager(context: Context) {

    // Obtiene una referencia al sistema de conectividad del sistema operativo.
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Define un callback para manejar los cambios en el estado de la red.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        // Este métdo se llama cuando la conexión a Internet está disponible.
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("NetworkCallback", "Conexión disponible")
            // Actualiza el estado de la conexión a true.
            isConnected.value = true
        }

        // Este métdo se llama cuando la conexión a Internet se pierde.
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("NetworkCallback", "Conexión perdida")
            // Actualiza el estado de la conexión a false.
            isConnected.value = false
        }
    }

    // Mantiene el estado actual de la conexión, observable en la UI.
    var isConnected = mutableStateOf(false)

    // Registra el callback para escuchar los cambios en la conectividad de la red.
    fun registerNetworkCallback() {
        // Crea una solicitud para detectar la red con capacidad de Internet.
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        // Registra el callback para la red.
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    // Desregistra el callback cuando ya no se necesita escuchar los cambios de conectividad.
    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}