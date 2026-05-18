package com.franktardencilla.mfdemoapp.data.applog;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {AppLogEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppLogDatabase extends RoomDatabase {
    public abstract AppLogDao appLogDao();
}
