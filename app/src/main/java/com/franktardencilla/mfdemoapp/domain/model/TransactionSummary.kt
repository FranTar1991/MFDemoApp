package com.franktardencilla.mfdemoapp.domain.model

data class TransactionSummary(
    val id: String,
    val amount: MoneyAmount,
    val createdAtMillis: Long,
    val status: TransactionStatus,
    val stan: String?,
    val entryMode: CardEntryMode?,
    val maskedPan: MaskedPan?,
    val responseCode: String?,
    val authCode: String?,
    val message: String?,
    val isoRequestSummary: String?,
    val isoResponseSummary: String?,
    val emvTagSummary: String?
)
