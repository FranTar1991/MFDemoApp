package com.franktardencilla.mfdemoapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franktardencilla.mfdemoapp.MainActivity
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory
import com.franktardencilla.mfdemoapp.ui.transaction.TransactionReceiptFragment

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val noRecentTransactionsText = view.findViewById<TextView>(R.id.noRecentTransactionsText)
        val transactionAdapter = HomeTransactionAdapter { transaction ->
            findNavController().navigate(
                R.id.action_homeFragment_to_transactionReceiptFragment,
                TransactionReceiptFragment.createArguments(transaction.id)
            )
        }
        val recentTransactionsList = view.findViewById<RecyclerView>(R.id.recentTransactionsList)
        recentTransactionsList.layoutManager = LinearLayoutManager(requireContext())
        recentTransactionsList.adapter = transactionAdapter

        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            noRecentTransactionsText.visibility = if (transactions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        parentFragmentManager.setFragmentResultListener(
            MainActivity.DATA_CLEARED_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
