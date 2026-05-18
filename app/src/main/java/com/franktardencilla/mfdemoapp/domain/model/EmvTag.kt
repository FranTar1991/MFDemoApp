package com.franktardencilla.mfdemoapp.domain.model

data class EmvTag(
    val tag: String,
    val value: String,
    val label: String = tag
) {
    init {
        require(tag.isNotBlank()) { "EMV tag cannot be blank." }
    }
}
