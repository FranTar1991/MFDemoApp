package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus

class KeyRepository {
    fun getKeyStatus(): KeyStatus {
        return KeyStatus(
            readiness = KeyReadinessStatus.NOT_READY,
            message = "Keys: not ready"
        )
    }
}
