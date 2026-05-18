package com.franktardencilla.mfdemoapp.ui.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.domain.model.TransactionSummary
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ResultViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _resultSummary = MutableLiveData(
        "Loading transactions..."
    )
    val resultSummary: LiveData<String> = _resultSummary

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val transactions = transactionRepository.getRecentTransactions()
            _resultSummary.value = if (transactions.isEmpty()) {
                "Complete a sale to see the result here."
            } else {
                transactions.joinToString(separator = "\n\n") { transaction ->
                    transaction.toDisplayText()
                }
            }
        }
    }

    fun clearTransactions() {
        viewModelScope.launch {
            transactionRepository.clearTransactions()
            _resultSummary.value = "Complete a sale to see the result here."
        }
    }

    private fun TransactionSummary.toDisplayText(): String {
        return listOf(
            "${status.name} ${amount.formatted()}",
            "Time: ${timeFormatter.format(Date(createdAtMillis))}",
            "STAN: ${stan ?: "none"}",
            "Entry: ${entryMode?.displayName ?: "unknown"}",
            "Card: ${maskedPan?.value ?: "unavailable"}"
        ).joinToString(separator = "\n")
    }

    private companion object {
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}
