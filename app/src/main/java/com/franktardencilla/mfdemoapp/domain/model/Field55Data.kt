package com.franktardencilla.mfdemoapp.domain.model

data class Field55Data(
    val tlvHex: String,
    val includedTags: List<String>
) {
    val byteLength: Int
        get() = tlvHex.length / 2
}
