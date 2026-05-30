package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackAKeyReadinessValidatorTest {
    @Test
    fun validate_requiresMasterKey() {
        val readiness = TrackAKeyReadinessValidator.validate(
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                slots = listOf(keySlot(KeyType.MAC, slot = 9)),
                message = "MAC only"
            )
        )

        assertFalse(readiness.isReady)
        assertEquals("Not ready: master key missing", readiness.message)
    }

    @Test
    fun validate_requiresMacKey() {
        val readiness = TrackAKeyReadinessValidator.validate(
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                slots = listOf(keySlot(KeyType.MASTER, slot = 1)),
                message = "Master only"
            )
        )

        assertFalse(readiness.isReady)
        assertEquals("Not ready: MAC key missing", readiness.message)
    }

    @Test
    fun validate_acceptsMasterAndMacWithKcv() {
        val readiness = TrackAKeyReadinessValidator.validate(
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                slots = listOf(
                    keySlot(KeyType.MASTER, slot = 1),
                    keySlot(KeyType.MAC, slot = 9)
                ),
                message = "Track A keys loaded"
            )
        )

        assertTrue(readiness.isReady)
        assertEquals("Ready: master and MAC keys loaded", readiness.message)
    }

    private fun keySlot(
        keyType: KeyType,
        slot: Int
    ): KeySlotMetadata {
        return KeySlotMetadata(
            keyType = keyType,
            slot = slot,
            kcv = "ABC123",
            updatedAtMillis = 1_779_317_740_000L
        )
    }
}
