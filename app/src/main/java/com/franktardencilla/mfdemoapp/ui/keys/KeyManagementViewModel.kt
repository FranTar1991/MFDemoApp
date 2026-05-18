package com.franktardencilla.mfdemoapp.ui.keys

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.device.TrackAKeyInjectionEvent
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class KeyManagementViewModel(
    private val keyRepository: KeyRepository
) : ViewModel() {
    private val _keyStatus = MutableLiveData("Keys: checking...")
    val keyStatus: LiveData<String> = _keyStatus

    private val _keyDetails = MutableLiveData("No key metadata available.")
    val keyDetails: LiveData<String> = _keyDetails

    private val injectionLogLines = mutableListOf<String>()
    private val _injectionLog = MutableLiveData("No injection steps recorded.")
    val injectionLog: LiveData<String> = _injectionLog

    init {
        refresh()
    }

    fun injectTrackADemoKeys() {
        viewModelScope.launch {
            resetInjectionLog()
            _keyStatus.value = "Starting Track A key injection..."
            val status = keyRepository.injectTrackAKeys(
                request = TrackAKeyInjectionRequest.demo()
            ) { event ->
                when (event) {
                    is TrackAKeyInjectionEvent.Progress -> {
                        appendInjectionLog(event.message)
                        _keyStatus.postValue(event.message)
                    }
                }
            }
            appendInjectionLog("Track A key injection finished.")
            updateKeyStatus(status)
        }
    }

    fun clearKeys() {
        viewModelScope.launch {
            _keyStatus.value = "Clearing keys..."
            updateKeyStatus(keyRepository.clearKeys())
        }
    }

    fun refresh() {
        viewModelScope.launch {
            updateKeyStatus(keyRepository.getKeyStatus())
        }
    }

    private fun resetInjectionLog() {
        injectionLogLines.clear()
        _injectionLog.value = "Starting Track A key injection..."
    }

    private fun appendInjectionLog(message: String) {
        injectionLogLines += message
        _injectionLog.postValue(
            injectionLogLines.joinToString(separator = "\n")
        )
    }

    private fun updateKeyStatus(status: KeyStatus) {
        val trackAReadiness = TrackAKeyReadinessValidator.validate(status)
        _keyStatus.value = listOf(
            status.message,
            "Track A readiness: ${trackAReadiness.message}"
        ).joinToString(separator = "\n")
        _keyDetails.value = if (status.slots.isEmpty()) {
            "No key metadata available."
        } else {
            status.slots.joinToString(separator = "\n\n") { slot ->
                slot.toDisplayText()
            }
        }
    }

    private fun KeySlotMetadata.toDisplayText(): String {
        return listOf(
            "Type: ${keyType.displayName}",
            "Slot: $slot",
            "KCV: ${kcv ?: "not available"}",
            "Updated: ${timestampFormatter.format(Date(updatedAtMillis))}"
        ).joinToString(separator = "\n")
    }

    private companion object {
        val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}
