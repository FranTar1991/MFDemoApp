package com.franktardencilla.mfdemoapp.data.transaction;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {TransactionEntity.class},
        version = 3,
        exportSchema = false
)
public abstract class TransactionDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
}
