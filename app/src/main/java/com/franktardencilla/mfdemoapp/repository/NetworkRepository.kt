package com.franktardencilla.mfdemoapp.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.franktardencilla.mfdemoapp.domain.model.NetworkStatus

class NetworkRepository(
    context: Context
) {
    private val connectivityManager = context.applicationContext.getSystemService(
        ConnectivityManager::class.java
    )

    fun getNetworkStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork
            ?: return NetworkStatus(
                isConnected = false,
                message = "Device network unavailable. Connect the POS to the same Wi-Fi/network as the host simulator."
            )
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkStatus(
                isConnected = false,
                message = "Device network unavailable. Android did not report active network capabilities."
            )

        val hasUsableTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasUsableTransport || !hasInternetCapability) {
            return NetworkStatus(
                isConnected = false,
                message = "Device network unavailable. Connect the POS to Wi-Fi, mobile data, or Ethernet before testing the host."
            )
        }

        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "network"
        }
        return NetworkStatus(
            isConnected = true,
            message = "Device network available: $transport"
        )
    }
}
