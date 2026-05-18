package com.franktardencilla.mfdemoapp.data.mockped

import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyType

class RoomMockPedKeyStore(
    private val dao: MockPedKeySlotDao
) : MockPedKeyStore {
    override suspend fun getKeySlots(): List<KeySlotMetadata> {
        return dao.getAll().mapNotNull { entity ->
            entity.toDomain()
        }
    }

    override suspend fun saveKeySlots(slots: List<KeySlotMetadata>) {
        dao.upsertAll(
            slots.map { slot ->
                slot.toEntity()
            }
        )
    }

    override suspend fun clearKeySlots() {
        dao.deleteAll()
    }

    private fun MockPedKeySlotEntity.toDomain(): KeySlotMetadata? {
        val parsedKeyType = runCatching {
            KeyType.valueOf(keyType)
        }.getOrNull() ?: return null

        return KeySlotMetadata(
            keyType = parsedKeyType,
            slot = slot,
            kcv = kcv,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun KeySlotMetadata.toEntity(): MockPedKeySlotEntity {
        return MockPedKeySlotEntity(
            slot,
            keyType.name,
            kcv,
            updatedAtMillis
        )
    }
}
