package com.franktardencilla.mfdemoapp.domain.model

data class IsoMessageSummary(
    val mti: String,
    val stan: String?,
    val responseCode: String? = null,
    val authCode: String? = null,
    val redactedMessage: String
)
