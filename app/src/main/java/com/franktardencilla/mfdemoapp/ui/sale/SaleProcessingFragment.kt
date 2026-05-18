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

class SaleProcessingFragment : Fragment() {
    private val viewModel: SaleViewModel by activityViewModels {
        appViewModelFactory()
    }
    private var navigatedToVoucher = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sale_processing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val processingStatusText = view.findViewById<TextView>(R.id.processingStatusText)
        val processingAmountText = view.findViewById<TextView>(R.id.processingAmountText)
        viewModel.screenStatus.observe(viewLifecycleOwner) { status ->
            processingStatusText.text = status
        }
        viewModel.amountSummary.observe(viewLifecycleOwner) { amount ->
            processingAmountText.text = amount
        }
        viewModel.saleComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete && !navigatedToVoucher) {
                navigatedToVoucher = true
                findNavController().navigate(R.id.action_saleProcessingFragment_to_saleVoucherFragment)
            }
        }

        view.findViewById<Button>(R.id.cancelProcessingSaleButton).setOnClickListener {
            viewModel.cancelSale()
        }
    }
}
