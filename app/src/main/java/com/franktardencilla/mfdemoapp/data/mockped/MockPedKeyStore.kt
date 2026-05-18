package com.franktardencilla.mfdemoapp.data.mockped

import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata

interface MockPedKeyStore {
    suspend fun getKeySlots(): List<KeySlotMetadata>

    suspend fun saveKeySlots(slots: List<KeySlotMetadata>)

    suspend fun clearKeySlots()
}
