package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.device.DeviceServiceManager
import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.DeviceServiceSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceRepository(
    private val deviceServiceManager: DeviceServiceManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun connect(): DeviceConnectionStatus {
        return runDeviceCall {
            deviceServiceManager.login()
        }
    }

    suspend fun disconnect() {
        withContext(ioDispatcher) {
            deviceServiceManager.logout()
        }
    }

    suspend fun getConnectionStatus(): DeviceConnectionStatus {
        return runDeviceCall {
            deviceServiceManager.getStatus()
        }
    }

    suspend fun getSession(): DeviceServiceSession? {
        return withContext(ioDispatcher) {
            try {
                deviceServiceManager.getSession()
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun runDeviceCall(
        call: suspend () -> DeviceConnectionStatus
    ): DeviceConnectionStatus {
        return withContext(ioDispatcher) {
            try {
                call()
            } catch (exception: Exception) {
                DeviceConnectionStatus(
                    isConnected = false,
                    message = "Device service error: ${exception.message ?: "try reconnecting"}"
                )
            }
        }
    }
}
