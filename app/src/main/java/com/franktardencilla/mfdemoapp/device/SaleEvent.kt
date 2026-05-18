package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.IsoMessageSummary
import com.franktardencilla.mfdemoapp.domain.model.SaleState

sealed interface SaleEvent {
    data class StateChanged(
        val state: SaleState,
        val message: String
    ) : SaleEvent

    data class Progress(val message: String) : SaleEvent

    data class EmvDataReady(val summary: EmvTagSummary) : SaleEvent

    data class IsoRequestReady(val summary: IsoMessageSummary) : SaleEvent

    data class IsoResponseReady(val summary: IsoMessageSummary) : SaleEvent

    data class Error(val message: String) : SaleEvent
}
