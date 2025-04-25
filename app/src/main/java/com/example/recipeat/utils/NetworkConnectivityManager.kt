package com.example.recipeat.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Esta clase gestiona la conectividad de la red en la aplicación y proporciona un estado
 * sobre si la conexión a Internet está disponible o no.
 */
class NetworkConnectivityManager(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var onConnectivityChanged: ((Boolean) -> Unit)? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            onConnectivityChanged?.invoke(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            onConnectivityChanged?.invoke(false)
        }
    }

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
