package com.franktardencilla.mfdemoapp.domain.model

enum class AppLogCategory(
    val displayName: String
) {
    DEVICE("Device"),
    KEYS("Keys"),
    SALE("Sale"),
    EMV("EMV"),
    ISO8583("ISO8583"),
    SECURITY("Security")
}
