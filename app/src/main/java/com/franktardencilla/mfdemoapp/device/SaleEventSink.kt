package com.franktardencilla.mfdemoapp.device

fun interface SaleEventSink {
    fun onEvent(event: SaleEvent)
}
