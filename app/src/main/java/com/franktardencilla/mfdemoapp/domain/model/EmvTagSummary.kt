package com.franktardencilla.mfdemoapp.domain.model

data class EmvTagSummary(
    val aid: String? = null,
    val maskedPan: MaskedPan? = null,
    val tags: List<EmvTag> = emptyList()
) {
    fun isEmpty(): Boolean {
        return aid == null && maskedPan == null && tags.isEmpty()
    }
}
