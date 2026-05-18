package com.franktardencilla.mfdemoapp.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.franktardencilla.mfdemoapp.domain.model.AppLogEntry
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsViewModel(
    private val appLogRepository: AppLogRepository
) : ViewModel() {
    val logText: LiveData<String> = appLogRepository.logs.map { logs ->
        if (logs.isEmpty()) {
            "No app events recorded yet."
        } else {
            logs.joinToString(separator = "\n\n") { entry ->
                entry.toDisplayText()
            }
        }
    }

    fun clearLogs() {
        appLogRepository.clear()
    }

    private fun AppLogEntry.toDisplayText(): String {
        return listOf(
            timestampFormatter.format(Date(timestampMillis)),
            category.displayName,
            message
        ).joinToString(separator = " | ")
    }

    private companion object {
        val timestampFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    }
}
