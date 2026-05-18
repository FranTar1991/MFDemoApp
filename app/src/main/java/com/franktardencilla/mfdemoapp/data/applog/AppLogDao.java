package com.franktardencilla.mfdemoapp.data.applog;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppLogDao {
    @Query("SELECT * FROM app_logs ORDER BY timestampMillis DESC, id DESC LIMIT :limit")
    LiveData<List<AppLogEntity>> getRecentLive(int limit);

    @Insert
    void insert(AppLogEntity entry);

    @Query("DELETE FROM app_logs WHERE id NOT IN (SELECT id FROM app_logs ORDER BY timestampMillis DESC, id DESC LIMIT :limit)")
    void pruneToLatest(int limit);

    @Query("DELETE FROM app_logs")
    void deleteAll();
}
