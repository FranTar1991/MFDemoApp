package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator

class MockPosDeviceAdapter(
    private val deviceServiceManager: DeviceServiceManager,
    private val mockPed: MockPed
) : PosDeviceAdapter {
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
        return buildTrackAStatus(
            slots = mockPed.getKeySlots(),
            emptyMessage = "Keys: not ready"
        )
    }

    override suspend fun injectTrackAKeys(
        request: TrackAKeyInjectionRequest,
        events: TrackAKeyInjectionEventSink
    ): KeyStatus {
        if (!deviceServiceManager.getStatus().isConnected) {
            return KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                slots = mockPed.getKeySlots(),
                message = "Keys: connect device service before loading keys"
            )
        }

        events.onEvent(TrackAKeyInjectionEvent.Progress("Injecting master key into slot ${request.masterKey.slot}"))
        val masterKeySlot = mockPed.injectMasterKey(request.masterKey)
        events.onEvent(TrackAKeyInjectionEvent.Progress("Verifying master key KCV"))
        require(mockPed.verifyKcv(masterKeySlot, request.masterKey.expectedKcv)) {
            "Master key KCV verification failed."
        }

        events.onEvent(TrackAKeyInjectionEvent.Progress("Injecting MAC working key into slot ${request.macWorkingKey.slot}"))
        val macKeySlot = mockPed.injectWorkingKey(
            masterKeySlot = request.masterKey.slot,
            workingKey = request.macWorkingKey
        )
        events.onEvent(TrackAKeyInjectionEvent.Progress("Verifying MAC working key KCV"))
        require(mockPed.verifyKcv(macKeySlot, request.macWorkingKey.expectedKcv)) {
            "MAC working key KCV verification failed."
        }

        request.pinWorkingKey?.let { pinKey ->
            events.onEvent(TrackAKeyInjectionEvent.Progress("Injecting optional PIN working key into slot ${pinKey.slot}"))
            val pinKeySlot = mockPed.injectWorkingKey(
                masterKeySlot = request.masterKey.slot,
                workingKey = pinKey
            )
            events.onEvent(TrackAKeyInjectionEvent.Progress("Verifying optional PIN working key KCV"))
            require(mockPed.verifyKcv(pinKeySlot, pinKey.expectedKcv)) {
                "PIN working key KCV verification failed."
            }
        }

        return buildTrackAStatus(
            slots = mockPed.getKeySlots(),
            emptyMessage = "Keys: not ready"
        )
    }

    override suspend fun clearKeys(): KeyStatus {
        mockPed.clearKeys()
        return KeyStatus(
            readiness = KeyReadinessStatus.CLEARED,
            message = "Keys: cleared"
        )
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

    private fun buildTrackAStatus(
        slots: List<KeySlotMetadata>,
        emptyMessage: String
    ): KeyStatus {
        if (slots.isEmpty()) {
            return KeyStatus(
                readiness = KeyReadinessStatus.NOT_READY,
                message = emptyMessage
            )
        }

        val readiness = TrackAKeyReadinessValidator.validate(
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                slots = slots,
                message = "Track A key metadata loaded from mock PED"
            )
        )
        return KeyStatus(
            readiness = readiness.readiness,
            slots = slots,
            message = readiness.message
        )
    }
}
