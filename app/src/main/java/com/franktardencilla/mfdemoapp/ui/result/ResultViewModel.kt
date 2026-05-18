package com.franktardencilla.mfdemoapp.ui.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class ResultViewModel(
    transactionRepository: TransactionRepository
) : ViewModel() {
    private val _resultSummary = MutableLiveData(transactionRepository.getTransactionSummary())
    val resultSummary: LiveData<String> = _resultSummary
}
