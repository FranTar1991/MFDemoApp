package com.franktardencilla.mfdemoapp.device

fun interface TrackAKeyInjectionEventSink {
    fun onEvent(event: TrackAKeyInjectionEvent)
}

sealed interface TrackAKeyInjectionEvent {
    data class Progress(val message: String) : TrackAKeyInjectionEvent
}
