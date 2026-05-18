package com.franktardencilla.mfdemoapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository

class HomeViewModel(
    deviceRepository: DeviceRepository,
    keyRepository: KeyRepository
) : ViewModel() {
    private val _serviceStatus = MutableLiveData(deviceRepository.getConnectionStatus().message)
    val serviceStatus: LiveData<String> = _serviceStatus

    private val _keyStatus = MutableLiveData(keyRepository.getKeyStatus().message)
    val keyStatus: LiveData<String> = _keyStatus
}
