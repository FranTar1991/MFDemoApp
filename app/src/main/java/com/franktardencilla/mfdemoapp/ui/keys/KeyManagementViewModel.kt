package com.franktardencilla.mfdemoapp.ui.keys

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franktardencilla.mfdemoapp.repository.KeyRepository

class KeyManagementViewModel(
    keyRepository: KeyRepository
) : ViewModel() {
    private val _keyStatus = MutableLiveData(keyRepository.getKeyStatus().message)
    val keyStatus: LiveData<String> = _keyStatus
}
