package com.franktardencilla.mfdemoapp.domain.model

data class SaleStateTransition(
    val state: SaleState,
    val message: String
)
