package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.SaleState

sealed interface SaleEvent {
    data class StateChanged(
        val state: SaleState,
        val message: String
    ) : SaleEvent

    data class Progress(val message: String) : SaleEvent

    data class Error(val message: String) : SaleEvent
}
