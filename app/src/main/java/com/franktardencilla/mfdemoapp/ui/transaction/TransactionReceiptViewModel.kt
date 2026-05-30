package com.franktardencilla.mfdemoapp.ui.transaction

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.repository.PrinterRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import com.franktardencilla.mfdemoapp.ui.sale.VoucherMapper
import com.franktardencilla.mfdemoapp.ui.sale.VoucherUiModel
import kotlinx.coroutines.launch

class TransactionReceiptViewModel(
    private val transactionRepository: TransactionRepository,
    private val printerRepository: PrinterRepository,
    private val voucherMapper: VoucherMapper
) : ViewModel() {
    private val _voucher = MutableLiveData(VoucherUiModel.empty())
    val voucher: LiveData<VoucherUiModel> = _voucher

    private val _status = MutableLiveData("Loading receipt...")
    val status: LiveData<String> = _status
    private val _printStatus = MutableLiveData("")
    val printStatus: LiveData<String> = _printStatus

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransaction(transactionId)
            if (transaction == null) {
                _status.value = "Transaction was not found."
                _voucher.value = VoucherUiModel.empty()
                return@launch
            }

            _status.value = transaction.message ?: "Receipt loaded."
            _voucher.value = voucherMapper.fromTransaction(transaction)
        }
    }

    fun printVoucher(voucherBitmap: Bitmap) {
        viewModelScope.launch {
            _printStatus.value = "Printing voucher..."
            _printStatus.value = printerRepository.printVoucher(voucherBitmap).message
        }
    }
}
