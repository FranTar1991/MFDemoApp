package com.franktardencilla.mfdemoapp.ui.sale

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository

class SaleViewModel(
    deviceRepository: DeviceRepository,
    keyRepository: KeyRepository
) : ViewModel() {
    private val _screenStatus = MutableLiveData(
        "${deviceRepository.getConnectionStatus().message}\n${keyRepository.getKeyStatus().message}"
    )
    val screenStatus: LiveData<String> = _screenStatus
}
