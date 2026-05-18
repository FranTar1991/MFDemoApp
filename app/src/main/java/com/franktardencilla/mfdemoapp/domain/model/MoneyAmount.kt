package com.franktardencilla.mfdemoapp.domain.model

data class MoneyAmount(
    val minorUnits: Long,
    val currencyCode: String = "840",
    val currencySymbol: String = "$"
) {
    init {
        require(minorUnits >= 0) { "Amount cannot be negative." }
        require(currencyCode.length == 3) { "Currency code must be a 3-digit ISO numeric code." }
    }

    fun formatted(): String {
        val major = minorUnits / 100
        val cents = minorUnits % 100
        return "$currencySymbol$major.${cents.toString().padStart(2, '0')}"
    }

    fun isoAmount12(): String {
        require(minorUnits <= MAX_ISO_AMOUNT_MINOR_UNITS) {
            "ISO8583 field 4 amount must fit in 12 digits."
        }
        return minorUnits.toString().padStart(12, '0')
    }

    companion object {
        private const val MAX_ISO_AMOUNT_MINOR_UNITS = 999_999_999_999L
    }
}
