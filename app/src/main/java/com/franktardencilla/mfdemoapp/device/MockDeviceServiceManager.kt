package com.franktardencilla.mfdemoapp.device

import android.content.Context
import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.DeviceModuleAvailability
import com.franktardencilla.mfdemoapp.domain.model.DeviceServiceSession
import kotlinx.coroutines.delay

class MockDeviceServiceManager(
    context: Context
) : DeviceServiceManager {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private var loggedIn = isPersistedConnected()
    private val moduleAvailability = DeviceModuleAvailability(
        emvAvailable = true,
        pinPadAvailable = true,
        pedAvailable = true,
        networkAvailable = true,
        printerAvailable = false,
        beeperAvailable = true
    )

    override suspend fun login(): DeviceConnectionStatus {
        delay(MOCK_BINDER_DELAY_MILLIS)
        loggedIn = true
        preferences.edit()
            .putBoolean(KEY_CONNECTED, true)
            .commit()
        return getStatus()
    }

    override suspend fun logout() {
        delay(MOCK_BINDER_DELAY_MILLIS)
        loggedIn = false
        preferences.edit()
            .putBoolean(KEY_CONNECTED, false)
            .commit()
    }

    override suspend fun getStatus(): DeviceConnectionStatus {
        loggedIn = isPersistedConnected()
        return DeviceConnectionStatus(
            isConnected = loggedIn,
            message = if (loggedIn) {
                "Device service: connected (mock session)"
            } else {
                "Device service: disconnected. Connect to continue."
            }
        )
    }

    override suspend fun getSession(): DeviceServiceSession? {
        loggedIn = isPersistedConnected()
        if (!loggedIn) {
            return null
        }

        return DeviceServiceSession(
            isLoggedIn = true,
            modules = moduleAvailability,
            message = "Mock YSDK session ready"
        )
    }

    private fun isPersistedConnected(): Boolean {
        return preferences.getBoolean(KEY_CONNECTED, false)
    }

    private companion object {
        const val PREFERENCES_NAME = "mock_device_connection"
        const val KEY_CONNECTED = "connected"
        const val MOCK_BINDER_DELAY_MILLIS = 150L
    }
}
