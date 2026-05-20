package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.EmvTag
import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.Field55Builder
import com.franktardencilla.mfdemoapp.domain.model.MaskedPan
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import kotlinx.coroutines.delay

class MockEmvProcessor : EmvCardReader, EmvTransactionProcessor {
    private var searchCanceled = false
    private var transactionCanceled = false

    override suspend fun searchCard(
        entryModes: List<CardEntryMode>,
        timeoutSeconds: Int
    ): CardSearchResult {
        searchCanceled = false
        delay(MOCK_CARD_SEARCH_DELAY_MILLIS)
        if (searchCanceled) {
            return CardSearchResult.Canceled
        }
        if (timeoutSeconds <= 0) {
            return CardSearchResult.Timeout
        }
        val entryMode = entryModes.firstOrNull()
            ?: return CardSearchResult.Failed("No card entry modes are enabled.")
        return CardSearchResult.Detected(
            DetectedCard(
                entryMode = entryMode,
                description = "Mock ${entryMode.displayName} card detected"
            )
        )
    }

    override suspend fun stopSearch() {
        searchCanceled = true
    }

    override suspend fun emvTrans(
        request: SaleRequest,
        card: DetectedCard,
        callbacks: EmvTransactionCallbacks
    ): EmvTransactionResult {
        transactionCanceled = false
        val selectedApp = callbacks.onSelApp(listOf(MOCK_APP_LABEL), isFirstSelect = true)
        if (selectedApp.isBlank()) {
            return EmvTransactionResult.Failed("No EMV application selected.")
        }
        if (!callbacks.onConfirmCardNo(MOCK_PAN)) {
            return EmvTransactionResult.Canceled
        }
        val pinResult = callbacks.onCardHolderInputPin(
            isOnlinePin = card.entryMode != CardEntryMode.CONTACTLESS,
            offlinePinType = MOCK_OFFLINE_PIN_TYPE
        )
        if (!pinResult.accepted) {
            return EmvTransactionResult.Canceled
        }
        delay(MOCK_EMV_TRANS_DELAY_MILLIS)
        if (transactionCanceled) {
            return EmvTransactionResult.Canceled
        }

        val tagSummary = buildTagSummary(request)
        val field55Data = Field55Builder.build(tagSummary)
        val onlineResponse = callbacks.onOnlineProc(
            EmvOnlineData(
                field55Hex = field55Data.tlvHex,
                amount = request.amount.isoAmount12()
            )
        )
        if (onlineResponse.responseCode == null) {
            return EmvTransactionResult.Failed("EMV online processing did not return a host response code.")
        }

        return EmvTransactionResult.Completed(tagSummary)
    }

    override suspend fun endPBOC() {
        transactionCanceled = true
    }

    private fun buildTagSummary(request: SaleRequest): EmvTagSummary {
        val maskedPan = MaskedPan.fromPlainPan(MOCK_PAN)
        return EmvTagSummary(
            aid = MOCK_AID,
            maskedPan = maskedPan,
            tags = listOf(
                EmvTag("5A", maskedPan.value, "Application PAN (masked)"),
                EmvTag("5F24", "271231", "Application expiration date"),
                EmvTag("9F02", request.amount.isoAmount12(), "Authorized amount"),
                EmvTag("5F2A", request.amount.currencyCode, "Transaction currency code"),
                EmvTag("95", "0000000000", "Terminal verification results"),
                EmvTag("9A", "260518", "Transaction date"),
                EmvTag("9C", "00", "Transaction type"),
                EmvTag("9F26", "1122334455667788", "Application cryptogram"),
                EmvTag("9F27", "80", "Cryptogram information data"),
                EmvTag("9F10", "06011203A0B800", "Issuer application data"),
                EmvTag("4F", MOCK_AID, "Application identifier"),
                EmvTag("84", MOCK_AID, "Dedicated file name")
            )
        )
    }

    private companion object {
        const val MOCK_PAN = "4111111111111111"
        const val MOCK_AID = "A0000000031010"
        const val MOCK_APP_LABEL = "VISA CREDIT"
        const val MOCK_OFFLINE_PIN_TYPE = 0
        const val MOCK_CARD_SEARCH_DELAY_MILLIS = 350L
        const val MOCK_EMV_TRANS_DELAY_MILLIS = 350L
    }
}
