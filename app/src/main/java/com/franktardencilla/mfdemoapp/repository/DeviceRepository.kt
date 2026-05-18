package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus

class DeviceRepository {
    fun getConnectionStatus(): DeviceConnectionStatus {
        return DeviceConnectionStatus(
            isConnected = false,
            message = "Device service: not connected"
        )
    }
}
