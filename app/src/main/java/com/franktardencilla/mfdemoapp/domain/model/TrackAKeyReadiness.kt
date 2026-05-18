package com.franktardencilla.mfdemoapp.domain.model

data class TrackAKeyReadiness(
    val readiness: KeyReadinessStatus,
    val message: String
) {
    val isReady: Boolean
        get() = readiness == KeyReadinessStatus.READY
}

object TrackAKeyReadinessValidator {
    fun validate(status: KeyStatus): TrackAKeyReadiness {
        if (status.readiness == KeyReadinessStatus.CLEARED) {
            return TrackAKeyReadiness(
                readiness = KeyReadinessStatus.NOT_READY,
                message = "Not ready: keys cleared"
            )
        }

        val masterKey = status.slots.firstOrNull { slot ->
            slot.keyType == KeyType.MASTER
        }
        val macKey = status.slots.firstOrNull { slot ->
            slot.keyType == KeyType.MAC
        }

        return when {
            masterKey == null -> TrackAKeyReadiness(
                readiness = KeyReadinessStatus.NOT_READY,
                message = "Not ready: master key missing"
            )
            macKey == null -> TrackAKeyReadiness(
                readiness = KeyReadinessStatus.NOT_READY,
                message = "Not ready: MAC key missing"
            )
            masterKey.kcv.isNullOrBlank() -> TrackAKeyReadiness(
                readiness = KeyReadinessStatus.NOT_READY,
                message = "Not ready: master key KCV missing"
            )
            macKey.kcv.isNullOrBlank() -> TrackAKeyReadiness(
                readiness = KeyReadinessStatus.NOT_READY,
                message = "Not ready: MAC key KCV missing"
            )
            else -> TrackAKeyReadiness(
                readiness = KeyReadinessStatus.READY,
                message = "Ready: master and MAC keys loaded"
            )
        }
    }
}
