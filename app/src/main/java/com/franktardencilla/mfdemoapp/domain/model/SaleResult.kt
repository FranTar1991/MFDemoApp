package com.franktardencilla.mfdemoapp.domain.model

data class SaleResult(
    val status: TransactionStatus,
    val amount: MoneyAmount,
    val amountBreakdown: SaleAmountBreakdown = SaleAmountBreakdown.fromBaseAmount(amount),
    val stan: String?,
    val entryMode: CardEntryMode?,
    val maskedPan: MaskedPan?,
    val responseCode: String?,
    val authCode: String?,
    val emvTagSummary: EmvTagSummary,
    val isoRequest: IsoMessageSummary?,
    val isoResponse: IsoMessageSummary?,
    val message: String
)
