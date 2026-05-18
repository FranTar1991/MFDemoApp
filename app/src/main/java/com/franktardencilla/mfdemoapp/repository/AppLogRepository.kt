package com.franktardencilla.mfdemoapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.franktardencilla.mfdemoapp.domain.model.AppLogCategory
import com.franktardencilla.mfdemoapp.domain.model.AppLogEntry

class AppLogRepository {
    private val entries = mutableListOf<AppLogEntry>()
    private val _logs = MutableLiveData<List<AppLogEntry>>(emptyList())
    val logs: LiveData<List<AppLogEntry>> = _logs

    fun add(
        category: AppLogCategory,
        message: String
    ) {
        entries += AppLogEntry(
            timestampMillis = System.currentTimeMillis(),
            category = category,
            message = redact(message)
        )
        _logs.postValue(entries.toList())
    }

    fun clear() {
        entries.clear()
        _logs.value = emptyList()
    }

    private fun redact(message: String): String {
        return message
            .replace(HEX_SECRET_PATTERN, "[REDACTED]")
            .replace(PAN_PATTERN, "[REDACTED_PAN]")
    }

    private companion object {
        val HEX_SECRET_PATTERN = Regex("\\b[0-9A-Fa-f]{16,}\\b")
        val PAN_PATTERN = Regex("\\b\\d{12,19}\\b")
    }
}
