package com.example.recipeat.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.mutableStateOf

class NetworkConnectivityManager(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            // Conexión disponible
            Log.d("NetworkCallback", "Conexión disponible")
            isConnected.value = true
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            // Conexión perdida
            Log.d("NetworkCallback", "Conexión perdida")
            isConnected.value = false
        }
    }

    var isConnected = mutableStateOf(false)

    fun registerNetworkCallback() {
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
