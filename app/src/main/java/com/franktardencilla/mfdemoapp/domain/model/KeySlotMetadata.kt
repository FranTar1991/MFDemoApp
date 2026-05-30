package com.franktardencilla.mfdemoapp.domain.model

data class KeySlotMetadata(
    val keyType: KeyType,
    val slot: Int,
    val kcv: String?,
    val updatedAtMillis: Long,
    val storageLabel: String = DEFAULT_STORAGE_LABEL
) {
    init {
        require(slot >= 0) { "Key slot cannot be negative." }
    }

    companion object {
        const val DEFAULT_STORAGE_LABEL = "Secure slot"
    }
}
