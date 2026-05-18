package com.franktardencilla.mfdemoapp.data.mockped;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {MockPedKeySlotEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class MockPedDatabase extends RoomDatabase {
    public abstract MockPedKeySlotDao mockPedKeySlotDao();
}
