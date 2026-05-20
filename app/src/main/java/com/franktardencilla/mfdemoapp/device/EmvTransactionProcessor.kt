package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest

interface EmvTransactionProcessor {
    suspend fun emvTrans(
        request: SaleRequest,
        card: DetectedCard,
        callbacks: EmvTransactionCallbacks
    ): EmvTransactionResult

    suspend fun endPBOC()
}

interface EmvTransactionCallbacks {
    fun onSelApp(appNameList: List<String>, isFirstSelect: Boolean): String
    fun onConfirmCardNo(cardNo: String): Boolean
    fun onCardHolderInputPin(isOnlinePin: Boolean, offlinePinType: Int): PinInputResult
    suspend fun onOnlineProc(data: EmvOnlineData): EmvOnlineResponse
}

sealed interface EmvTransactionResult {
    data class Completed(val emvTagSummary: EmvTagSummary) : EmvTransactionResult
    data class Failed(val message: String) : EmvTransactionResult
    data object Canceled : EmvTransactionResult
}

data class EmvOnlineData(
    val field55Hex: String,
    val amount: String
)

data class EmvOnlineResponse(
    val approved: Boolean,
    val responseCode: String? = null,
    val issuerAuthData: String? = null
)

data class PinInputResult(
    val accepted: Boolean,
    val pinBlock: String? = null,
    val ksn: String? = null
)
