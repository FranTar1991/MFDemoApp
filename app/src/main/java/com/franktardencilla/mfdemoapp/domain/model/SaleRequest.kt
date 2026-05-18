package com.franktardencilla.mfdemoapp.domain.model

data class SaleRequest(
    val amount: MoneyAmount,
    val preferredEntryModes: List<CardEntryMode> = listOf(
        CardEntryMode.CONTACT,
        CardEntryMode.CONTACTLESS
    )
)
