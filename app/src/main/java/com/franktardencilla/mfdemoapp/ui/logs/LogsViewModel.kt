package com.franktardencilla.mfdemoapp.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class LogsViewModel(
    transactionRepository: TransactionRepository
) : ViewModel() {
    private val _transactionSummary = MutableLiveData(
        if (transactionRepository.getRecentTransactions().isEmpty()) {
            "Transactions: none yet"
        } else {
            "Transactions: ${transactionRepository.getRecentTransactions().size}"
        }
    )
    val transactionSummary: LiveData<String> = _transactionSummary
}
