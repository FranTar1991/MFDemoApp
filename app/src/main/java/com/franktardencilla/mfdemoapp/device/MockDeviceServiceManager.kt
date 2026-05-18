package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.DeviceModuleAvailability
import com.franktardencilla.mfdemoapp.domain.model.DeviceServiceSession
import kotlinx.coroutines.delay

class MockDeviceServiceManager : DeviceServiceManager {
    private var loggedIn = false
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
        return getStatus()
    }

    override suspend fun logout() {
        delay(MOCK_BINDER_DELAY_MILLIS)
        loggedIn = false
    }

    override suspend fun getStatus(): DeviceConnectionStatus {
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
        if (!loggedIn) {
            return null
        }

        return DeviceServiceSession(
            isLoggedIn = true,
            modules = moduleAvailability,
            message = "Mock YSDK session ready"
        )
    }

    private companion object {
        const val MOCK_BINDER_DELAY_MILLIS = 150L
    }
}
