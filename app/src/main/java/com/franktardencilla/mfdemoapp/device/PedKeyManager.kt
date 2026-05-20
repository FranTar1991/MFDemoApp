package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyType

interface PedKeyManager {
    suspend fun getKeySlots(): List<KeySlotMetadata>

    suspend fun loadMainKey(
        slot: Int,
        keyDataHex: String,
        expectedKcv: String
    ): PedKeyOperationResult

    suspend fun loadWorkKey(
        keyType: KeyType,
        masterKeySlot: Int,
        workKeySlot: Int,
        keyDataHex: String,
        expectedKcv: String
    ): PedKeyOperationResult

    suspend fun calcKcv(
        keyType: KeyType,
        slot: Int
    ): String?

    suspend fun calcMac(
        macKeySlot: Int,
        dataHex: String
    ): PedMacResult

    suspend fun clearKeys()
}

sealed interface PedKeyOperationResult {
    data class Loaded(val slot: KeySlotMetadata) : PedKeyOperationResult
    data class Failed(val message: String) : PedKeyOperationResult
}

sealed interface PedMacResult {
    data class Calculated(val macHex: String) : PedMacResult
    data class Failed(val message: String) : PedMacResult
}
