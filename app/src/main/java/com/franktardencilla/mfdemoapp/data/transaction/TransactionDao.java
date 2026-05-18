package com.franktardencilla.mfdemoapp.data.transaction;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transaction_records ORDER BY createdAtMillis DESC LIMIT :limit")
    List<TransactionEntity> getRecent(int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TransactionEntity transaction);

    @Query("DELETE FROM transaction_records")
    void deleteAll();
}
