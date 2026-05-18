package com.franktardencilla.mfdemoapp.domain.model

data class KeySlotMetadata(
    val keyType: KeyType,
    val slot: Int,
    val kcv: String?,
    val updatedAtMillis: Long
) {
    init {
        require(slot >= 0) { "Key slot cannot be negative." }
    }
}
