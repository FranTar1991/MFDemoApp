package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.DeviceServiceSession

interface DeviceServiceManager {
    suspend fun login(): DeviceConnectionStatus

    suspend fun logout()

    suspend fun getStatus(): DeviceConnectionStatus

    suspend fun getSession(): DeviceServiceSession?
}
