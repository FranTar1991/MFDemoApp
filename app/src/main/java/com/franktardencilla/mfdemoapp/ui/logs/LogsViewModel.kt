package com.franktardencilla.mfdemoapp.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class LogsViewModel(
    transactionRepository: TransactionRepository
) : ViewModel() {
    private val _transactionSummary = MutableLiveData(transactionRepository.getTransactionSummary())
    val transactionSummary: LiveData<String> = _transactionSummary
}
