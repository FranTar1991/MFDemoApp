package com.franktardencilla.mfdemoapp.ui.sale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class SaleVoucherFragment : Fragment() {
    private val viewModel: SaleViewModel by activityViewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sale_voucher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val voucherReceiptView = view.findViewById<VoucherReceiptView>(R.id.voucherReceiptView)
        val printStatusText = view.findViewById<TextView>(R.id.saleVoucherPrintStatusText)
        viewModel.voucherDetails.observe(viewLifecycleOwner) { voucher ->
            voucherReceiptView.setVoucher(voucher)
        }
        viewModel.printStatus.observe(viewLifecycleOwner) { status ->
            printStatusText.text = status
            printStatusText.visibility = if (status.isBlank()) View.GONE else View.VISIBLE
        }

        view.findViewById<Button>(R.id.printSaleVoucherButton).setOnClickListener {
            viewModel.printVoucher(VoucherBitmapRenderer.render(voucherReceiptView))
        }

        view.findViewById<Button>(R.id.finishSaleButton).setOnClickListener {
            viewModel.resetSale()
            findNavController().navigate(R.id.action_saleVoucherFragment_to_homeFragment)
        }
    }
}
