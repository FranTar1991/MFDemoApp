package com.franktardencilla.mfdemoapp.domain.model

data class TransactionSummary(
    val id: String,
    val amount: MoneyAmount,
    val createdAtMillis: Long,
    val status: TransactionStatus,
    val stan: String?,
    val entryMode: CardEntryMode?,
    val maskedPan: MaskedPan?
)
