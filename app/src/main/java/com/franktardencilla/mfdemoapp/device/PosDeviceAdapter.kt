package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest

interface PosDeviceAdapter {
    suspend fun connect(): DeviceConnectionStatus

    suspend fun disconnect()

    suspend fun getConnectionStatus(): DeviceConnectionStatus

    suspend fun getKeyStatus(): KeyStatus

    suspend fun injectTrackAKeys(
        request: TrackAKeyInjectionRequest,
        events: TrackAKeyInjectionEventSink
    ): KeyStatus

    suspend fun clearKeys(): KeyStatus

    suspend fun startSale(
        request: SaleRequest,
        events: SaleEventSink
    ): SaleDeviceResult

    suspend fun cancelCurrentOperation()
}
