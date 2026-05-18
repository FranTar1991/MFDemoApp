package com.franktardencilla.mfdemoapp.ui.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ResultViewModel : ViewModel() {
    private val _resultSummary = MutableLiveData(
        "Complete a sale to see the result here."
    )
    val resultSummary: LiveData<String> = _resultSummary
}
