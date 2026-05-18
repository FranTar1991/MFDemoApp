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
    private val _transactions = MutableLiveData<List<TransactionSummary>>(emptyList())
    val transactions: LiveData<List<TransactionSummary>> = _transactions
    private val _detailSummary = MutableLiveData("Select a transaction to view details.")
    val detailSummary: LiveData<String> = _detailSummary

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val transactions = transactionRepository.getRecentTransactions()
            _transactions.value = transactions
            _detailSummary.value = transactions.firstOrNull()?.toDetailText()
                ?: "Complete a sale to see the result here."
        }
    }

    fun selectTransaction(transactionId: String) {
        val transaction = _transactions.value.orEmpty().firstOrNull { transaction ->
            transaction.id == transactionId
        }
        _detailSummary.value = transaction?.toDetailText()
            ?: "Selected transaction was not found."
    }

    fun clearTransactions() {
        viewModelScope.launch {
            transactionRepository.clearTransactions()
            _transactions.value = emptyList()
            _detailSummary.value = "Complete a sale to see the result here."
        }
    }

    fun TransactionSummary.toListText(): String {
        return listOf(
            "${status.name} ${amount.formatted()}",
            timeFormatter.format(Date(createdAtMillis)),
            "STAN: ${stan ?: "none"} | ${entryMode?.displayName ?: "unknown"}",
            "Card: ${maskedPan?.value ?: "unavailable"}"
        ).joinToString(separator = "\n")
    }

    private fun TransactionSummary.toDetailText(): String {
        return listOf(
            "${status.name} ${amount.formatted()}",
            "Time: ${timeFormatter.format(Date(createdAtMillis))}",
            "STAN: ${stan ?: "none"}",
            "Entry: ${entryMode?.displayName ?: "unknown"}",
            "Card: ${maskedPan?.value ?: "unavailable"}",
            "Response: ${responseCode ?: "none"}",
            "Auth: ${authCode ?: "none"}",
            "Message: ${message ?: "none"}",
            "",
            "ISO8583 Request",
            isoRequestSummary ?: "No ISO8583 request stored.",
            "",
            "ISO8583 Response",
            isoResponseSummary ?: "No ISO8583 response stored.",
            "",
            "EMV Summary",
            emvTagSummary ?: "No EMV tag summary stored."
        ).joinToString(separator = "\n")
    }

    private companion object {
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}
