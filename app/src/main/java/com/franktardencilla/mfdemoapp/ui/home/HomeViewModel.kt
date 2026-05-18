package com.franktardencilla.mfdemoapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val deviceRepository: DeviceRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {
    private val _serviceStatus = MutableLiveData("Device service: checking...")
    val serviceStatus: LiveData<String> = _serviceStatus

    private val _moduleStatus = MutableLiveData("Payment modules: checking...")
    val moduleStatus: LiveData<String> = _moduleStatus

    private val _keyStatus = MutableLiveData("Keys: checking...")
    val keyStatus: LiveData<String> = _keyStatus

    init {
        refresh()
    }

    fun connectDeviceService() {
        viewModelScope.launch {
            _serviceStatus.value = "Device service: connecting..."
            _serviceStatus.value = deviceRepository.connect().message
            updateModuleStatus()
            _keyStatus.value = keyRepository.getKeyStatus().message
        }
    }

    fun disconnectDeviceService() {
        viewModelScope.launch {
            _serviceStatus.value = "Device service: disconnecting..."
            deviceRepository.disconnect()
            _serviceStatus.value = deviceRepository.getConnectionStatus().message
            updateModuleStatus()
            _keyStatus.value = keyRepository.getKeyStatus().message
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _serviceStatus.value = deviceRepository.getConnectionStatus().message
            updateModuleStatus()
            _keyStatus.value = keyRepository.getKeyStatus().message
        }
    }

    private suspend fun updateModuleStatus() {
        val session = deviceRepository.getSession()
        _moduleStatus.value = if (session == null) {
            "Payment modules: unavailable until device service is connected"
        } else {
            buildString {
                appendLine(session.message)
                append(session.modules.displayLines().joinToString(separator = "\n"))
            }
        }
    }
}
