package com.franktardencilla.mfdemoapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.domain.model.TransactionSummary
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _recentTransactions = MutableLiveData<List<TransactionSummary>>(emptyList())
    val recentTransactions: LiveData<List<TransactionSummary>> = _recentTransactions

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            updateRecentTransactions()
        }
    }

    private suspend fun updateRecentTransactions() {
        _recentTransactions.value = transactionRepository.getRecentTransactions(HOME_TRANSACTION_LIMIT)
    }

    private companion object {
        const val HOME_TRANSACTION_LIMIT = 10
    }
}
