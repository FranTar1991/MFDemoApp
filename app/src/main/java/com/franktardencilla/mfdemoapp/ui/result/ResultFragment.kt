package com.franktardencilla.mfdemoapp.ui.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class ResultFragment : Fragment() {
    private val viewModel: ResultViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionList = view.findViewById<LinearLayout>(R.id.transactionList)
        val transactionDetailText = view.findViewById<TextView>(R.id.transactionDetailText)
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionList.removeAllViews()
            if (transactions.isEmpty()) {
                transactionList.addView(buildEmptyListText())
            } else {
                transactions.forEach { transaction ->
                    transactionList.addView(
                        buildTransactionButton(
                            text = viewModel.run { transaction.toListText() },
                            transactionId = transaction.id
                        )
                    )
                }
            }
        }
        viewModel.detailSummary.observe(viewLifecycleOwner) { summary ->
            transactionDetailText.text = summary
        }
        view.findViewById<Button>(R.id.refreshResultsButton).setOnClickListener {
            viewModel.refresh()
        }
        view.findViewById<Button>(R.id.clearResultsButton).setOnClickListener {
            viewModel.clearTransactions()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun buildTransactionButton(
        text: String,
        transactionId: String
    ): Button {
        return Button(requireContext()).apply {
            this.text = text
            isAllCaps = false
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            setOnClickListener {
                viewModel.selectTransaction(transactionId)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.result_item_spacing)
            }
        }
    }

    private fun buildEmptyListText(): TextView {
        return TextView(requireContext()).apply {
            text = getString(R.string.no_transactions)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
}
