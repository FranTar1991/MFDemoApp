package com.franktardencilla.mfdemoapp.domain.model

data class MaskedPan(
    val value: String
) {
    init {
        require(value.contains('*')) { "Masked PAN must not contain a full unmasked PAN." }
    }

    companion object {
        fun fromPlainPan(pan: String): MaskedPan {
            val cleanPan = pan.filter { it.isDigit() }
            require(cleanPan.length >= 10) { "PAN must contain at least 10 digits to be masked safely." }

            val firstSix = cleanPan.take(6)
            val lastFour = cleanPan.takeLast(4)
            val mask = "*".repeat(cleanPan.length - firstSix.length - lastFour.length)
            return MaskedPan(firstSix + mask + lastFour)
        }
    }
}
