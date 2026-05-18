package com.franktardencilla.mfdemoapp.ui.host

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.domain.model.HostConfig
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.HostConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

class HostSettingsViewModel(
    private val hostConfigRepository: HostConfigRepository,
    private val appLogRepository: AppLogRepository
) : ViewModel() {
    private val _hostConfig = MutableLiveData(hostConfigRepository.getHostConfig())
    val hostConfig: LiveData<HostConfig> = _hostConfig
    private val _status = MutableLiveData("Host settings loaded.")
    val status: LiveData<String> = _status

    fun save(
        primaryHost: String,
        fallbackHost: String,
        portText: String,
        timeoutText: String
    ): Boolean {
        val config = parseConfig(primaryHost, fallbackHost, portText, timeoutText)
            ?: return false
        hostConfigRepository.saveHostConfig(config)
        _hostConfig.value = config
        _status.value = "Host settings saved."
        appLogRepository.add(
            com.franktardencilla.mfdemoapp.domain.model.AppLogCategory.ISO8583,
            "Host settings saved | hosts=${config.hosts.joinToString()} | port=${config.port}"
        )
        return true
    }

    fun testConnection(
        primaryHost: String,
        fallbackHost: String,
        portText: String,
        timeoutText: String
    ) {
        val config = parseConfig(primaryHost, fallbackHost, portText, timeoutText)
            ?: return
        _status.value = "Testing host connection..."
        viewModelScope.launch(Dispatchers.IO) {
            val result = config.hosts.firstNotNullOfOrNull { host ->
                runCatching {
                    Socket().use { socket ->
                        socket.connect(
                            InetSocketAddress(host, config.port),
                            config.timeoutMillis
                        )
                    }
                    host
                }.getOrNull()
            }
            _status.postValue(
                if (result == null) {
                    "Host connection failed for ${config.hosts.joinToString()}."
                } else {
                    "Host connection successful: $result:${config.port}"
                }
            )
        }
    }

    private fun parseConfig(
        primaryHost: String,
        fallbackHost: String,
        portText: String,
        timeoutText: String
    ): HostConfig? {
        val port = portText.trim().toIntOrNull()
        if (port == null) {
            _status.value = "Enter a valid port."
            return null
        }
        val timeoutMillis = timeoutText.trim().toIntOrNull()
        if (timeoutMillis == null) {
            _status.value = "Enter a valid timeout in milliseconds."
            return null
        }
        return runCatching {
            HostConfig(
                primaryHost = primaryHost.trim(),
                fallbackHost = fallbackHost.trim(),
                port = port,
                timeoutMillis = timeoutMillis
            )
        }.getOrElse { error ->
            _status.value = error.message ?: "Invalid host settings."
            null
        }
    }
}
