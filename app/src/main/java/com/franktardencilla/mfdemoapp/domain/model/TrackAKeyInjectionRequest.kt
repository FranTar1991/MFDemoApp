package com.franktardencilla.mfdemoapp.domain.model

data class TrackAKeyInjectionRequest(
    val masterKey: TrackAKeySpec,
    val macWorkingKey: TrackAKeySpec,
    val pinWorkingKey: TrackAKeySpec? = null
) {
    init {
        require(masterKey.keyType == KeyType.MASTER) {
            "Track A requires a master key."
        }
        require(macWorkingKey.keyType == KeyType.MAC) {
            "Track A requires a MAC working key."
        }
        require(pinWorkingKey == null || pinWorkingKey.keyType == KeyType.PIN) {
            "Optional Track A PIN key must use PIN key type."
        }
    }

    companion object {
        fun demo(): TrackAKeyInjectionRequest {
            return TrackAKeyInjectionRequest(
                masterKey = TrackAKeySpec(
                    keyType = KeyType.MASTER,
                    slot = 1,
                    expectedKcv = "A1B2C3",
                    label = "Demo TMK",
                    keyDataHex = "00112233445566778899AABBCCDDEEFF"
                ),
                macWorkingKey = TrackAKeySpec(
                    keyType = KeyType.MAC,
                    slot = 21,
                    expectedKcv = "D4E5F6",
                    label = "Demo MAC working key",
                    keyDataHex = "112233445566778899AABBCCDDEEFF00"
                ),
                pinWorkingKey = TrackAKeySpec(
                    keyType = KeyType.PIN,
                    slot = 22,
                    expectedKcv = "789ABC",
                    label = "Demo PIN working key",
                    keyDataHex = "2233445566778899AABBCCDDEEFF0011"
                )
            )
        }
    }
}

data class TrackAKeySpec(
    val keyType: KeyType,
    val slot: Int,
    val expectedKcv: String,
    val label: String,
    val keyDataHex: String
) {
    init {
        require(slot >= 0) {
            "Key slot cannot be negative."
        }
        require(expectedKcv.length == KCV_LENGTH) {
            "KCV must be $KCV_LENGTH characters."
        }
        require(keyDataHex.isNotBlank()) {
            "Key data cannot be blank."
        }
        require(keyDataHex.length % 2 == 0 && keyDataHex.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
            "Key data must be an even-length hexadecimal string."
        }
    }

    private companion object {
        const val KCV_LENGTH = 6
    }
}
