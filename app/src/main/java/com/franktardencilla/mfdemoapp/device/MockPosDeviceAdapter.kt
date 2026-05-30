package com.franktardencilla.mfdemoapp.device

import android.graphics.Bitmap
import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.Field55Data
import com.franktardencilla.mfdemoapp.domain.model.HostSaleResponse
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Packager
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Message
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.SaleResult
import com.franktardencilla.mfdemoapp.domain.model.SaleState
import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.SaleIsoRequestBuilder
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import kotlinx.coroutines.delay

class MockPosDeviceAdapter(
    private val deviceServiceManager: DeviceServiceManager,
    private val pedKeyManager: PedKeyManager,
    private val emvCardReader: EmvCardReader,
    private val emvTransactionProcessor: EmvTransactionProcessor,
    private val hostClient: HostClient,
    private val saleIsoRequestBuilder: SaleIsoRequestBuilder
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
            slots = pedKeyManager.getKeySlots(),
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
                slots = pedKeyManager.getKeySlots(),
                message = "Keys: connect device service before loading keys"
            )
        }

        events.onEvent(TrackAKeyInjectionEvent.Progress("loadMainKey(slot=${request.masterKey.slot})"))
        val mainKeyResult = pedKeyManager.loadMainKey(
            slot = request.masterKey.slot,
            keyDataHex = request.masterKey.keyDataHex,
            expectedKcv = request.masterKey.expectedKcv
        )
        if (mainKeyResult is PedKeyOperationResult.Failed) {
            return keyLoadFailed(mainKeyResult.message)
        }
        events.onEvent(TrackAKeyInjectionEvent.Progress("calcKcv(MASTER, slot=${request.masterKey.slot})"))
        val masterKcv = pedKeyManager.calcKcv(KeyType.MASTER, request.masterKey.slot)
        if (masterKcv != request.masterKey.expectedKcv) {
            return keyLoadFailed("Master key KCV verification failed.")
        }

        events.onEvent(TrackAKeyInjectionEvent.Progress("loadWorkKey(MAC, slot=${request.macWorkingKey.slot})"))
        val macKeyResult = pedKeyManager.loadWorkKey(
            keyType = KeyType.MAC,
            masterKeySlot = request.masterKey.slot,
            workKeySlot = request.macWorkingKey.slot,
            keyDataHex = request.macWorkingKey.keyDataHex,
            expectedKcv = request.macWorkingKey.expectedKcv
        )
        if (macKeyResult is PedKeyOperationResult.Failed) {
            return keyLoadFailed(macKeyResult.message)
        }
        events.onEvent(TrackAKeyInjectionEvent.Progress("calcKcv(MAC, slot=${request.macWorkingKey.slot})"))
        val macKcv = pedKeyManager.calcKcv(KeyType.MAC, request.macWorkingKey.slot)
        if (macKcv != request.macWorkingKey.expectedKcv) {
            return keyLoadFailed("MAC working key KCV verification failed.")
        }

        request.pinWorkingKey?.let { pinKey ->
            events.onEvent(TrackAKeyInjectionEvent.Progress("loadWorkKey(PIN, slot=${pinKey.slot})"))
            val pinKeyResult = pedKeyManager.loadWorkKey(
                keyType = KeyType.PIN,
                masterKeySlot = request.masterKey.slot,
                workKeySlot = pinKey.slot,
                keyDataHex = pinKey.keyDataHex,
                expectedKcv = pinKey.expectedKcv
            )
            if (pinKeyResult is PedKeyOperationResult.Failed) {
                return keyLoadFailed(pinKeyResult.message)
            }
            events.onEvent(TrackAKeyInjectionEvent.Progress("calcKcv(PIN, slot=${pinKey.slot})"))
            val pinKcv = pedKeyManager.calcKcv(KeyType.PIN, pinKey.slot)
            if (pinKcv != pinKey.expectedKcv) {
                return keyLoadFailed("PIN working key KCV verification failed.")
            }
        }

        return buildTrackAStatus(
            slots = pedKeyManager.getKeySlots(),
            emptyMessage = "Keys: not ready"
        )
    }

    private suspend fun keyLoadFailed(message: String): KeyStatus {
        return KeyStatus(
            readiness = KeyReadinessStatus.NOT_READY,
            slots = pedKeyManager.getKeySlots(),
            message = message
        )
    }

    override suspend fun clearKeys(): KeyStatus {
        pedKeyManager.clearKeys()
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
        var hostResponse: HostSaleResponse? = null
        return try {
            emitSaleStep(SaleState.WAITING_FOR_CARD, "EmvHandler.searchCard started", events)?.let {
                return it
            }
            val cardSearchResult = emvCardReader.searchCard(
                entryModes = request.preferredEntryModes,
                timeoutSeconds = CARD_SEARCH_TIMEOUT_SECONDS
            )
            val detectedCard = when (cardSearchResult) {
                is CardSearchResult.Detected -> cardSearchResult.card
                is CardSearchResult.Failed -> return SaleDeviceResult.Failed(cardSearchResult.message)
                CardSearchResult.Timeout -> return SaleDeviceResult.Failed("Card search timed out.")
                CardSearchResult.Canceled -> return SaleDeviceResult.Canceled
            }

            emitSaleStep(SaleState.CARD_DETECTED, detectedCard.description, events)?.let {
                return it
            }
            emitSaleStep(SaleState.READING_EMV, "EmvHandler.emvTrans started", events)?.let {
                return it
            }
            val emvTransactionResult = runCatching {
                emvTransactionProcessor.emvTrans(
                    request = request,
                    card = detectedCard,
                    callbacks = buildMockEmvCallbacks(
                        request = request,
                        detectedCard = detectedCard,
                        events = events
                    ) { response ->
                        hostResponse = response
                    }
                )
            }.getOrElse { error ->
                return SaleDeviceResult.Failed(
                    "Could not reach host simulator during onOnlineProc. Start it on port 8001 and try again. ${error.message.orEmpty()}"
                )
            }
            val emvTagSummary = when (emvTransactionResult) {
                is EmvTransactionResult.Completed -> emvTransactionResult.emvTagSummary
                is EmvTransactionResult.Failed -> return SaleDeviceResult.Failed(emvTransactionResult.message)
                EmvTransactionResult.Canceled -> return SaleDeviceResult.Canceled
            }
            val completedHostResponse = hostResponse
                ?: return SaleDeviceResult.Failed("EMV online processing did not complete host authorization.")
            emitSaleStep(SaleState.EMV_DATA_READY, "Mock EMV data ready", events)?.let {
                return it
            }
            events.onEvent(SaleEvent.EmvDataReady(emvTagSummary))
            emitSaleStep(SaleState.WAITING_FOR_HOST, "Host simulator responded during onOnlineProc", events)?.let {
                return it
            }

            if (completedHostResponse.isApproved) {
                SaleDeviceResult.Completed(
                    SaleResult(
                        status = TransactionStatus.APPROVED,
                        amount = request.amount,
                        amountBreakdown = request.amountBreakdown,
                        stan = completedHostResponse.responseSummary.stan,
                        entryMode = detectedCard.entryMode,
                        maskedPan = emvTagSummary.maskedPan,
                        responseCode = completedHostResponse.responseSummary.responseCode,
                        authCode = completedHostResponse.responseSummary.authCode,
                        emvTagSummary = emvTagSummary,
                        isoRequest = completedHostResponse.requestSummary,
                        isoResponse = completedHostResponse.responseSummary,
                        message = "Host approved sale. Auth: ${completedHostResponse.responseSummary.authCode ?: "none"}"
                    )
                )
            } else {
                SaleDeviceResult.Completed(
                    SaleResult(
                        status = TransactionStatus.DECLINED,
                        amount = request.amount,
                        amountBreakdown = request.amountBreakdown,
                        stan = completedHostResponse.responseSummary.stan,
                        entryMode = detectedCard.entryMode,
                        maskedPan = emvTagSummary.maskedPan,
                        responseCode = completedHostResponse.responseSummary.responseCode,
                        authCode = completedHostResponse.responseSummary.authCode,
                        emvTagSummary = emvTagSummary,
                        isoRequest = completedHostResponse.requestSummary,
                        isoResponse = completedHostResponse.responseSummary,
                        message = "Host declined sale. Response code: ${completedHostResponse.responseSummary.responseCode ?: "unknown"}"
                    )
                )
            }
        } finally {
            emvCardReader.stopSearch()
            emvTransactionProcessor.endPBOC()
        }
    }

    override suspend fun cancelCurrentOperation() {
        saleCanceled = true
        emvCardReader.stopSearch()
        emvTransactionProcessor.endPBOC()
    }

    override suspend fun printVoucher(voucherBitmap: Bitmap): PrintResult {
        delay(MOCK_PRINT_DELAY_MILLIS)
        return PrintResult(
            isSuccess = true,
            message = "Mock printer accepted voucher image."
        )
    }

    private fun buildMockEmvCallbacks(
        request: SaleRequest,
        detectedCard: DetectedCard,
        events: SaleEventSink,
        onHostResponse: (HostSaleResponse) -> Unit
    ): EmvTransactionCallbacks {
        return object : EmvTransactionCallbacks {
            override fun onSelApp(
                appNameList: List<String>,
                isFirstSelect: Boolean
            ): String {
                val selectedApp = appNameList.firstOrNull().orEmpty()
                events.onEvent(
                    SaleEvent.Progress(
                        "onSelApp apps=${appNameList.joinToString()} selected=$selectedApp first=$isFirstSelect"
                    )
                )
                return selectedApp
            }

            override fun onConfirmCardNo(cardNo: String): Boolean {
                events.onEvent(SaleEvent.Progress("onConfirmCardNo received masked PAN"))
                return true
            }

            override fun onCardHolderInputPin(
                isOnlinePin: Boolean,
                offlinePinType: Int
            ): PinInputResult {
                events.onEvent(
                    SaleEvent.Progress(
                        "onCardHolderInputPin online=$isOnlinePin offlineType=$offlinePinType"
                    )
                )
                return PinInputResult(
                    accepted = true,
                    pinBlock = if (isOnlinePin) MOCK_PIN_BLOCK else null,
                    ksn = if (isOnlinePin) MOCK_KSN else null
                )
            }

            override suspend fun onOnlineProc(data: EmvOnlineData): EmvOnlineResponse {
                events.onEvent(
                    SaleEvent.Progress(
                        "EMV online data ready DE55 length=${data.field55Hex.length} amount=${data.amount}"
                    )
                )
                val field55Data = Field55Data(
                    tlvHex = data.field55Hex,
                    includedTags = emptyList()
                )
                val isoRequest = saleIsoRequestBuilder.build(
                    request = request,
                    entryMode = detectedCard.entryMode,
                    field55Data = field55Data
                )
                val macKeySlot = getLoadedMacKeySlot()
                events.onEvent(SaleEvent.Progress("calcMac(MAC, slot=$macKeySlot)"))
                val macResult = pedKeyManager.calcMac(
                    macKeySlot = macKeySlot,
                    dataHex = Iso8583Packager.pack(isoRequest).toHex()
                )
                val macHex = when (macResult) {
                    is PedMacResult.Calculated -> macResult.macHex
                    is PedMacResult.Failed -> error(macResult.message)
                }
                val securedIsoRequest = isoRequest.withField(
                    field = ISO_FIELD_MAC,
                    value = macHex
                )
                events.onEvent(SaleEvent.Progress("ISO8583 field 64 MAC attached"))
                events.onEvent(SaleEvent.Progress("Sending ISO8583 authorization to host simulator"))
                val response = hostClient.authorizeSale(securedIsoRequest)
                verifyResponseMacIfPresent(response.responseMessage, macKeySlot)
                onHostResponse(response)
                events.onEvent(SaleEvent.IsoRequestReady(response.requestSummary))
                events.onEvent(SaleEvent.IsoResponseReady(response.responseSummary))
                return EmvOnlineResponse(
                    approved = response.isApproved,
                    responseCode = response.responseSummary.responseCode,
                    issuerAuthData = MOCK_ISSUER_AUTH_DATA
                )
            }
        }
    }

    private suspend fun getLoadedMacKeySlot(): Int {
        return pedKeyManager.getKeySlots()
            .firstOrNull { slot ->
                slot.keyType == KeyType.MAC
            }
            ?.slot
            ?: error("MAC key is not loaded.")
    }

    private suspend fun verifyResponseMacIfPresent(
        responseMessage: Iso8583Message,
        macKeySlot: Int
    ) {
        val responseMac = responseMessage.get(ISO_FIELD_MAC) ?: return
        val macResult = pedKeyManager.calcMac(
            macKeySlot = macKeySlot,
            dataHex = Iso8583Packager.pack(responseMessage.withoutField(ISO_FIELD_MAC)).toHex()
        )
        when (macResult) {
            is PedMacResult.Calculated -> require(macResult.macHex == responseMac) {
                "Host response MAC verification failed."
            }
            is PedMacResult.Failed -> error(macResult.message)
        }
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
        const val MOCK_PRINT_DELAY_MILLIS = 500L
        const val CARD_SEARCH_TIMEOUT_SECONDS = 20
        const val ISO_FIELD_MAC = 64
        const val MOCK_PIN_BLOCK = "0123456789ABCDEF"
        const val MOCK_KSN = "FFFF9876543210E00001"
        const val MOCK_ISSUER_AUTH_DATA = "910A11223344556677883030"
    }
}

private fun ByteArray.toHex(): String {
    return joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }.uppercase()
}
