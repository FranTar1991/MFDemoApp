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
                    expectedKcv = "82E13665",
                    label = "MoreFun demo TMK",
                    keyDataHex = "111111111111111111111111111111111111111111111111"
                ),
                macWorkingKey = TrackAKeySpec(
                    keyType = KeyType.MAC,
                    slot = 9,
                    expectedKcv = "6FB23EAD",
                    label = "MoreFun demo MAC working key",
                    keyDataHex = "9E90DE82745E68529E90DE82745E68529E90DE82745E6852"
                ),
                pinWorkingKey = TrackAKeySpec(
                    keyType = KeyType.PIN,
                    slot = 1,
                    expectedKcv = "6FB23EAD",
                    label = "MoreFun demo PIN working key",
                    keyDataHex = "9E90DE82745E68529E90DE82745E68529E90DE82745E6852"
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
        require(expectedKcv.length in KCV_LENGTHS) {
            "KCV must be 6 or 8 hexadecimal characters."
        }
        require(expectedKcv.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
            "KCV must be hexadecimal."
        }
        require(keyDataHex.isNotBlank()) {
            "Key data cannot be blank."
        }
        require(keyDataHex.length % 2 == 0 && keyDataHex.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
            "Key data must be an even-length hexadecimal string."
        }
    }

    private companion object {
        val KCV_LENGTHS = setOf(6, 8)
    }
}
