package com.franktardencilla.mfdemoapp.domain.model

data class Iso8583Message(
    val mti: String,
    val fields: Map<Int, String>
) {
    init {
        require(mti.length == 4 && mti.all(Char::isDigit)) {
            "MTI must be 4 numeric characters."
        }
        require(fields.keys.all { it in 2..128 }) {
            "ISO8583 fields must be between 2 and 128."
        }
    }

    fun get(field: Int): String? = fields[field]

    fun withField(
        field: Int,
        value: String
    ): Iso8583Message {
        return copy(fields = fields + (field to value))
    }

    fun withoutField(field: Int): Iso8583Message {
        return copy(fields = fields - field)
    }
}
