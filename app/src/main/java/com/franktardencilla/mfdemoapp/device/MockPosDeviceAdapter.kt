package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.SaleResult
import com.franktardencilla.mfdemoapp.domain.model.SaleState
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import kotlinx.coroutines.delay

class MockPosDeviceAdapter(
    private val deviceServiceManager: DeviceServiceManager,
    private val mockPed: MockPed
) : PosDeviceAdapter {
    private var saleCanceled = false

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
        saleCanceled = false
        emitSaleStep(SaleState.WAITING_FOR_CARD, "Waiting for card", events)?.let {
            return it
        }
        emitSaleStep(SaleState.CARD_DETECTED, "Mock card detected", events)?.let {
            return it
        }
        emitSaleStep(SaleState.READING_EMV, "Reading mock EMV application", events)?.let {
            return it
        }
        emitSaleStep(SaleState.EMV_DATA_READY, "Mock EMV data ready", events)?.let {
            return it
        }
        emitSaleStep(SaleState.WAITING_FOR_HOST, "Waiting for host simulator", events)?.let {
            return it
        }

        return SaleDeviceResult.Failed("Host simulator is not implemented yet.")
    }

    override suspend fun cancelCurrentOperation() {
        saleCanceled = true
    }

    private suspend fun emitSaleStep(
        state: SaleState,
        message: String,
        events: SaleEventSink
    ): SaleDeviceResult? {
        if (saleCanceled) {
            return SaleDeviceResult.Canceled
        }
        events.onEvent(
            SaleEvent.StateChanged(
                state = state,
                message = message
            )
        )
        delay(MOCK_SALE_STEP_DELAY_MILLIS)
        return if (saleCanceled) {
            events.onEvent(SaleEvent.StateChanged(SaleState.CANCELED, "Sale canceled"))
            SaleDeviceResult.Canceled
        } else {
            null
        }
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

    private companion object {
        const val MOCK_SALE_STEP_DELAY_MILLIS = 600L
    }
}
