package com.franktardencilla.mfdemoapp.data.mockped;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mock_ped_key_slots")
public class MockPedKeySlotEntity {
    @PrimaryKey
    public int slot;

    @NonNull
    public String keyType;

    @Nullable
    public String kcv;

    public long updatedAtMillis;

    public MockPedKeySlotEntity(
            int slot,
            @NonNull String keyType,
            @Nullable String kcv,
            long updatedAtMillis
    ) {
        this.slot = slot;
        this.keyType = keyType;
        this.kcv = kcv;
        this.updatedAtMillis = updatedAtMillis;
    }
}
