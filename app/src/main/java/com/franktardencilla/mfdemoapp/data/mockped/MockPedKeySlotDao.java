package com.franktardencilla.mfdemoapp.data.mockped;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MockPedKeySlotDao {
    @Query("SELECT * FROM mock_ped_key_slots ORDER BY slot ASC")
    List<MockPedKeySlotEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<MockPedKeySlotEntity> slots);

    @Query("DELETE FROM mock_ped_key_slots")
    void deleteAll();
}
