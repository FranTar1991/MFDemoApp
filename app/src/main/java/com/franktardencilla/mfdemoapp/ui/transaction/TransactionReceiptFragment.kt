package com.franktardencilla.mfdemoapp.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory
import com.franktardencilla.mfdemoapp.ui.sale.VoucherBitmapRenderer
import com.franktardencilla.mfdemoapp.ui.sale.VoucherReceiptView

class TransactionReceiptFragment : Fragment() {
    private val viewModel: TransactionReceiptViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transaction_receipt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusText = view.findViewById<TextView>(R.id.transactionReceiptStatusText)
        val printStatusText = view.findViewById<TextView>(R.id.transactionReceiptPrintStatusText)
        val receiptView = view.findViewById<VoucherReceiptView>(R.id.transactionReceiptView)

        viewModel.status.observe(viewLifecycleOwner) { status ->
            statusText.text = status
        }
        viewModel.voucher.observe(viewLifecycleOwner) { voucher ->
            receiptView.setVoucher(voucher)
        }
        viewModel.printStatus.observe(viewLifecycleOwner) { status ->
            printStatusText.text = status
            printStatusText.visibility = if (status.isBlank()) View.GONE else View.VISIBLE
        }

        view.findViewById<Button>(R.id.printTransactionReceiptButton).setOnClickListener {
            viewModel.printVoucher(VoucherBitmapRenderer.render(receiptView))
        }

        view.findViewById<Button>(R.id.backToTransactionsButton).setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.loadTransaction(
            requireArguments().getString(ARG_TRANSACTION_ID).orEmpty()
        )
    }

    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"

        fun createArguments(transactionId: String): Bundle {
            return bundleOf(ARG_TRANSACTION_ID to transactionId)
        }
    }
}
