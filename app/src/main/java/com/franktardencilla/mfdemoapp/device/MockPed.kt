package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.data.mockped.MockPedKeyStore
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeySpec

class MockPed(
    private val keyStore: MockPedKeyStore
) {
    suspend fun getKeySlots(): List<KeySlotMetadata> {
        return keyStore.getKeySlots()
    }

    suspend fun injectMasterKey(keySpec: TrackAKeySpec): KeySlotMetadata {
        require(keySpec.keyType == KeyType.MASTER) {
            "Master injection requires a master key spec."
        }

        return saveSlot(keySpec)
    }

    suspend fun injectWorkingKey(
        masterKeySlot: Int,
        workingKey: TrackAKeySpec
    ): KeySlotMetadata {
        require(workingKey.keyType != KeyType.MASTER) {
            "Working key injection cannot use master key type."
        }
        require(masterKeyExists(masterKeySlot)) {
            "Master key slot $masterKeySlot must exist before injecting a working key."
        }

        return saveSlot(workingKey)
    }

    fun verifyKcv(
        keySlot: KeySlotMetadata,
        expectedKcv: String
    ): Boolean {
        return keySlot.kcv == expectedKcv
    }

    suspend fun clearKeys() {
        keyStore.clearKeySlots()
    }

    private suspend fun masterKeyExists(slot: Int): Boolean {
        return keyStore.getKeySlots().any { storedSlot ->
            storedSlot.slot == slot && storedSlot.keyType == KeyType.MASTER
        }
    }

    private suspend fun saveSlot(keySpec: TrackAKeySpec): KeySlotMetadata {
        val newSlot = KeySlotMetadata(
            keyType = keySpec.keyType,
            slot = keySpec.slot,
            kcv = keySpec.expectedKcv,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedSlots = keyStore.getKeySlots()
            .filterNot { existingSlot ->
                existingSlot.slot == newSlot.slot
            } + newSlot

        keyStore.saveKeySlots(updatedSlots)
        return newSlot
    }
}
