package com.franktardencilla.mfdemoapp.device.morefun

internal fun String.hexToBytes(): ByteArray {
    val normalized = trim()
    require(normalized.length % 2 == 0 && normalized.all { character ->
        character in '0'..'9' ||
            character in 'A'..'F' ||
            character in 'a'..'f'
    }) {
        "Value must be an even-length hexadecimal string."
    }
    return ByteArray(normalized.length / 2) { index ->
        normalized.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}

internal fun ByteArray.toHex(): String {
    return joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }.uppercase()
}
