package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import org.junit.Assert.assertEquals
import org.junit.Test

class KeySlotMetadataTest {
    @Test
    fun storageLabel_defaultsToSecureSlot() {
        val metadata = KeySlotMetadata(
            keyType = KeyType.MASTER,
            slot = 1,
            kcv = "ABC123",
            updatedAtMillis = 1_779_317_740_000L
        )

        assertEquals("Secure slot", metadata.storageLabel)
    }

    @Test
    fun storageLabel_canDescribePinPadFallbackIndex() {
        val metadata = KeySlotMetadata(
            keyType = KeyType.MAC,
            slot = 0,
            kcv = "ABC123",
            updatedAtMillis = 1_779_317_740_000L,
            storageLabel = "PinPad key index"
        )

        assertEquals("PinPad key index", metadata.storageLabel)
        assertEquals(0, metadata.slot)
    }
}
