package com.franktardencilla.mfdemoapp.ui.keys

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import kotlinx.coroutines.launch

class KeyManagementViewModel(
    private val keyRepository: KeyRepository
) : ViewModel() {
    private val _keyStatus = MutableLiveData("Keys: checking...")
    val keyStatus: LiveData<String> = _keyStatus

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _keyStatus.value = keyRepository.getKeyStatus().message
        }
    }
}
