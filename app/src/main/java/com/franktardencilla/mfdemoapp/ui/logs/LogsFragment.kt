package com.franktardencilla.mfdemoapp.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class LogsFragment : Fragment() {
    private val viewModel: LogsViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionSummaryText = view.findViewById<TextView>(R.id.transactionSummaryText)
        viewModel.transactionSummary.observe(viewLifecycleOwner) { summary ->
            transactionSummaryText.text = summary
        }
    }
}
