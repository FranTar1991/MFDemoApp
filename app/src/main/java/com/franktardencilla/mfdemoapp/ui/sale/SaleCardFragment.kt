package com.franktardencilla.mfdemoapp.ui.sale

import android.app.AlertDialog
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

class SaleCardFragment : Fragment() {
    private val viewModel: SaleViewModel by activityViewModels {
        appViewModelFactory()
    }
    private var navigatedToProcessing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sale_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardStatusText = view.findViewById<TextView>(R.id.cardStatusText)
        val cardAmountText = view.findViewById<TextView>(R.id.cardAmountText)
        viewModel.screenStatus.observe(viewLifecycleOwner) { status ->
            cardStatusText.text = status
        }
        viewModel.amountSummary.observe(viewLifecycleOwner) { amount ->
            cardAmountText.text = amount
        }
        viewModel.saleActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive && !navigatedToProcessing) {
                navigatedToProcessing = true
                findNavController().navigate(R.id.action_saleCardFragment_to_saleProcessingFragment)
            }
        }
        viewModel.blockingAlert.observe(viewLifecycleOwner) { alert ->
            if (alert == null) {
                return@observe
            }
            AlertDialog.Builder(requireContext())
                .setTitle(alert.title)
                .setMessage(alert.message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    viewModel.clearBlockingAlert()
                    findNavController().navigate(R.id.homeFragment)
                }
                .show()
        }

        view.findViewById<Button>(R.id.presentCardButton).setOnClickListener {
            navigatedToProcessing = false
            viewModel.startSale()
        }
        view.findViewById<Button>(R.id.cancelCardSaleButton).setOnClickListener {
            viewModel.cancelSale()
            findNavController().navigate(R.id.action_saleCardFragment_to_saleVoucherFragment)
        }
    }
}
