package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.data.mockped.MockPedKeyStore
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import java.security.MessageDigest

class MockPed(
    private val keyStore: MockPedKeyStore
) : PedKeyManager {
    override suspend fun getKeySlots(): List<KeySlotMetadata> {
        return keyStore.getKeySlots()
    }

    override suspend fun loadMainKey(
        slot: Int,
        keyDataHex: String,
        expectedKcv: String
    ): PedKeyOperationResult {
        if (!keyDataHex.isValidHex()) {
            return PedKeyOperationResult.Failed("Main key data must be an even-length hexadecimal string.")
        }
        return PedKeyOperationResult.Loaded(
            saveSlot(
                keyType = KeyType.MASTER,
                slot = slot,
                expectedKcv = expectedKcv
            )
        )
    }

    override suspend fun loadWorkKey(
        keyType: KeyType,
        masterKeySlot: Int,
        workKeySlot: Int,
        keyDataHex: String,
        expectedKcv: String
    ): PedKeyOperationResult {
        if (keyType == KeyType.MASTER) {
            return PedKeyOperationResult.Failed("Working key load cannot use master key type.")
        }
        if (!keyDataHex.isValidHex()) {
            return PedKeyOperationResult.Failed("Working key data must be an even-length hexadecimal string.")
        }
        if (!masterKeyExists(masterKeySlot)) {
            return PedKeyOperationResult.Failed(
                "Master key slot $masterKeySlot must exist before loading a working key."
            )
        }

        return PedKeyOperationResult.Loaded(
            saveSlot(
                keyType = keyType,
                slot = workKeySlot,
                expectedKcv = expectedKcv
            )
        )
    }

    override suspend fun calcKcv(
        keyType: KeyType,
        slot: Int
    ): String? {
        return keyStore.getKeySlots()
            .firstOrNull { storedSlot ->
                storedSlot.keyType == keyType && storedSlot.slot == slot
            }
            ?.kcv
    }

    override suspend fun calcMac(
        macKeySlot: Int,
        dataHex: String
    ): PedMacResult {
        if (!dataHex.isValidHex()) {
            return PedMacResult.Failed("MAC input data must be an even-length hexadecimal string.")
        }
        val macSlot = keyStore.getKeySlots()
            .firstOrNull { storedSlot ->
                storedSlot.keyType == KeyType.MAC && storedSlot.slot == macKeySlot
            }
            ?: return PedMacResult.Failed("MAC key slot $macKeySlot is not loaded.")

        val digestInput = "slot=${macSlot.slot};kcv=${macSlot.kcv};data=$dataHex"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(Charsets.UTF_8))
        return PedMacResult.Calculated(
            macHex = digest.toHex().take(MAC_HEX_LENGTH)
        )
    }

    override suspend fun clearKeys() {
        keyStore.clearKeySlots()
    }

    private suspend fun masterKeyExists(slot: Int): Boolean {
        return keyStore.getKeySlots().any { storedSlot ->
            storedSlot.slot == slot && storedSlot.keyType == KeyType.MASTER
        }
    }

    private suspend fun saveSlot(
        keyType: KeyType,
        slot: Int,
        expectedKcv: String
    ): KeySlotMetadata {
        val newSlot = KeySlotMetadata(
            keyType = keyType,
            slot = slot,
            kcv = expectedKcv,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedSlots = keyStore.getKeySlots()
            .filterNot { existingSlot ->
                existingSlot.slot == newSlot.slot
            } + newSlot

        keyStore.saveKeySlots(updatedSlots)
        return newSlot
    }

    private fun String.isValidHex(): Boolean {
        return isNotBlank() &&
            length % 2 == 0 &&
            all { character ->
                character in '0'..'9' ||
                    character in 'A'..'F' ||
                    character in 'a'..'f'
            }
    }

    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }.uppercase()
    }

    private companion object {
        const val MAC_HEX_LENGTH = 16
    }
}
