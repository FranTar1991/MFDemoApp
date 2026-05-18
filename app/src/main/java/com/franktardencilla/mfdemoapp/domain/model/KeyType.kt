package com.franktardencilla.mfdemoapp.domain.model

enum class KeyType(
    val displayName: String
) {
    MASTER("Master key"),
    MAC("MAC working key"),
    PIN("PIN working key"),
    DATA("Data working key"),
    DUKPT("DUKPT")
}
