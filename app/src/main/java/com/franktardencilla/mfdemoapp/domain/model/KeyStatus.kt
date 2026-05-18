package com.franktardencilla.mfdemoapp.domain.model

data class KeyStatus(
    val readiness: KeyReadinessStatus,
    val slots: List<KeySlotMetadata> = emptyList(),
    val message: String
) {
    val isReady: Boolean
        get() = readiness == KeyReadinessStatus.READY
}
