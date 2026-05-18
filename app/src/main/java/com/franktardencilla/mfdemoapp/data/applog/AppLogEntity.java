package com.franktardencilla.mfdemoapp.data.applog;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_logs")
public class AppLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestampMillis;

    @NonNull
    public String category;

    @NonNull
    public String message;

    public AppLogEntity(
            long timestampMillis,
            @NonNull String category,
            @NonNull String message
    ) {
        this.timestampMillis = timestampMillis;
        this.category = category;
        this.message = message;
    }
}
