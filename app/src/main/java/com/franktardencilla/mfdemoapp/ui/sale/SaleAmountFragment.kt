package com.franktardencilla.mfdemoapp.ui.sale

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.franktardencilla.mfdemoapp.MainActivity
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

        val baseAmountInput = view.findViewById<EditText>(R.id.baseAmountInput)
        val tipAmountInput = view.findViewById<EditText>(R.id.tipAmountInput)
        val taxAmountInput = view.findViewById<EditText>(R.id.taxAmountInput)
        val amountStatusText = view.findViewById<TextView>(R.id.amountStatusText)
        val continueButton = view.findViewById<Button>(R.id.continueToCardButton)

        listOf(baseAmountInput, tipAmountInput, taxAmountInput).forEach { input ->
            input.addTextChangedListener(PosAmountTextWatcher(input))
        }

        viewModel.screenStatus.observe(viewLifecycleOwner) { status ->
            amountStatusText.text = status
        }
        viewModel.saleReady.observe(viewLifecycleOwner) { isReady ->
            baseAmountInput.isEnabled = isReady
            tipAmountInput.isEnabled = isReady
            taxAmountInput.isEnabled = isReady
            continueButton.isEnabled = isReady
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
        parentFragmentManager.setFragmentResultListener(
            MainActivity.DEVICE_CONNECTION_CHANGED_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.checkSaleReadiness()
        }

        fun continueToCard() {
            if (
                viewModel.setAmountBreakdown(
                    baseInput = baseAmountInput.text.toString(),
                    tipInput = tipAmountInput.text.toString(),
                    taxInput = taxAmountInput.text.toString()
                )
            ) {
                findNavController().navigate(R.id.action_saleAmountFragment_to_saleCardFragment)
            }
        }

        baseAmountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                tipAmountInput.requestFocus()
                true
            } else {
                false
            }
        }
        tipAmountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                taxAmountInput.requestFocus()
                true
            } else {
                false
            }
        }
        taxAmountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                continueToCard()
                true
            } else {
                false
            }
        }

        continueButton.setOnClickListener {
            continueToCard()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkSaleReadiness()
    }

    private class PosAmountTextWatcher(
        private val editText: EditText
    ) : TextWatcher {
        private var isFormatting = false

        override fun beforeTextChanged(
            text: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) = Unit

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) = Unit

        override fun afterTextChanged(editable: Editable?) {
            if (isFormatting) {
                return
            }

            val digits = editable
                ?.toString()
                .orEmpty()
                .filter { it.isDigit() }
                .trimStart('0')
                .ifEmpty { "0" }
                .take(MAX_MINOR_UNIT_DIGITS)
            val minorUnits = digits.toLongOrNull() ?: 0L
            val formattedAmount = formatMinorUnits(minorUnits)

            isFormatting = true
            editText.setText(formattedAmount)
            editText.setSelection(formattedAmount.length)
            isFormatting = false
        }

        private fun formatMinorUnits(minorUnits: Long): String {
            val major = minorUnits / 100
            val cents = minorUnits % 100
            return "$major.${cents.toString().padStart(2, '0')}"
        }

        private companion object {
            const val MAX_MINOR_UNIT_DIGITS = 12
        }
    }
}
