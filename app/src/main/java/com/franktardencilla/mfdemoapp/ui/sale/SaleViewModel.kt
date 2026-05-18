package com.franktardencilla.mfdemoapp.ui.sale

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import kotlinx.coroutines.launch

class SaleViewModel(
    private val deviceRepository: DeviceRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {
    private val _screenStatus = MutableLiveData("Checking sale readiness...")
    val screenStatus: LiveData<String> = _screenStatus

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _screenStatus.value = listOf(
                deviceRepository.getConnectionStatus().message,
                keyRepository.getKeyStatus().message
            ).joinToString(separator = "\n")
        }
    }
}
