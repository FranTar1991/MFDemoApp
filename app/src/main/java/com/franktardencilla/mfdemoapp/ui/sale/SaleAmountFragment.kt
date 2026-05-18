package com.franktardencilla.mfdemoapp.ui.sale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class SaleAmountFragment : Fragment() {
    private val viewModel: SaleViewModel by activityViewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sale_amount, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amountInput = view.findViewById<EditText>(R.id.saleAmountInput)
        val amountStatusText = view.findViewById<TextView>(R.id.amountStatusText)

        viewModel.screenStatus.observe(viewLifecycleOwner) { status ->
            amountStatusText.text = status
        }
        viewModel.saleReady.observe(viewLifecycleOwner) { isReady ->
            amountInput.isEnabled = isReady
            view.findViewById<Button>(R.id.continueToCardButton).isEnabled = isReady
        }

        view.findViewById<Button>(R.id.continueToCardButton).setOnClickListener {
            if (viewModel.setAmount(amountInput.text.toString())) {
                findNavController().navigate(R.id.action_saleAmountFragment_to_saleCardFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkSaleReadiness()
    }
}
