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

        val voucherSummaryText = view.findViewById<TextView>(R.id.voucherSummaryText)
        viewModel.voucherSummary.observe(viewLifecycleOwner) { summary ->
            voucherSummaryText.text = summary
        }

        view.findViewById<Button>(R.id.finishSaleButton).setOnClickListener {
            viewModel.resetSale()
            findNavController().navigate(R.id.action_saleVoucherFragment_to_homeFragment)
        }
    }
}
