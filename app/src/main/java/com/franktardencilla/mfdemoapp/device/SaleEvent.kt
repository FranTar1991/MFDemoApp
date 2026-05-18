package com.franktardencilla.mfdemoapp.device

sealed interface SaleEvent {
    data class Progress(val message: String) : SaleEvent

    data class Error(val message: String) : SaleEvent
}
