package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest

class MockPosDeviceAdapter(
    private val deviceServiceManager: DeviceServiceManager
) : PosDeviceAdapter {
    private var keyStatus = KeyStatus(
        readiness = KeyReadinessStatus.NOT_READY,
        message = "Keys: not ready"
    )

    override suspend fun connect(): DeviceConnectionStatus {
        return deviceServiceManager.login()
    }

    override suspend fun disconnect() {
        deviceServiceManager.logout()
    }

    override suspend fun getConnectionStatus(): DeviceConnectionStatus {
        return deviceServiceManager.getStatus()
    }

    override suspend fun getKeyStatus(): KeyStatus {
        return keyStatus
    }

    override suspend fun injectDemoKeys(): KeyStatus {
        if (!deviceServiceManager.getStatus().isConnected) {
            keyStatus = KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                message = "Keys: connect device service before loading keys"
            )
            return keyStatus
        }

        keyStatus = KeyStatus(
            readiness = KeyReadinessStatus.READY,
            slots = listOf(
                KeySlotMetadata(
                    keyType = KeyType.MASTER,
                    slot = 1,
                    kcv = "A1B2C3",
                    updatedAtMillis = System.currentTimeMillis()
                ),
                KeySlotMetadata(
                    keyType = KeyType.MAC,
                    slot = 21,
                    kcv = "D4E5F6",
                    updatedAtMillis = System.currentTimeMillis()
                )
            ),
            message = "Keys: ready"
        )
        return keyStatus
    }

    override suspend fun clearKeys(): KeyStatus {
        keyStatus = KeyStatus(
            readiness = KeyReadinessStatus.CLEARED,
            message = "Keys: cleared"
        )
        return keyStatus
    }

    override suspend fun startSale(
        request: SaleRequest,
        events: SaleEventSink
    ): SaleDeviceResult {
        events.onEvent(SaleEvent.Progress("Mock sale flow is not implemented yet."))
        return SaleDeviceResult.Failed("Mock sale flow is not implemented yet.")
    }

    override suspend fun cancelCurrentOperation() {
        // No operation is active in the mock adapter yet.
    }
}
