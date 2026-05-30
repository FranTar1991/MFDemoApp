package com.franktardencilla.mfdemoapp.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.franktardencilla.mfdemoapp.data.applog.AppLogDao
import com.franktardencilla.mfdemoapp.data.applog.AppLogEntity
import com.franktardencilla.mfdemoapp.domain.model.AppLogCategory
import com.franktardencilla.mfdemoapp.domain.model.AppLogEntry
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppLogRepository(
    private val appLogDao: AppLogDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val executor = Executors.newSingleThreadExecutor()
    val logs: LiveData<List<AppLogEntry>> = appLogDao.getRecentLive(LOG_LIMIT).map { entries ->
        entries.map { entry ->
            entry.toDomain()
        }
    }

    fun add(
        category: AppLogCategory,
        message: String
    ) {
        val redactedMessage = redact(message)
        Log.i(LOGCAT_TAG, "[${category.name}] $redactedMessage")
        executor.execute {
            appLogDao.insert(
                AppLogEntity(
                    System.currentTimeMillis(),
                    category.name,
                    redactedMessage
                )
            )
            appLogDao.pruneToLatest(LOG_LIMIT)
        }
    }

    fun clear() {
        executor.execute {
            appLogDao.deleteAll()
        }
    }

    suspend fun clearAndWait() {
        withContext(ioDispatcher) {
            appLogDao.deleteAll()
        }
    }

    private fun AppLogEntity.toDomain(): AppLogEntry {
        return AppLogEntry(
            timestampMillis = timestampMillis,
            category = runCatching { AppLogCategory.valueOf(category) }
                .getOrDefault(AppLogCategory.SECURITY),
            message = message
        )
    }

    private fun redact(message: String): String {
        return message
            .replace(HEX_SECRET_PATTERN, "[REDACTED]")
            .replace(PAN_PATTERN, "[REDACTED_PAN]")
    }

    private companion object {
        const val LOGCAT_TAG = "MFDemoApp"
        const val LOG_LIMIT = 250
        val HEX_SECRET_PATTERN = Regex("\\b[0-9A-Fa-f]{16,}\\b")
        val PAN_PATTERN = Regex("\\b\\d{12,19}\\b")
    }
}
