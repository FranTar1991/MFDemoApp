package com.franktardencilla.mfdemoapp.ui.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val resultSummaryText = view.findViewById<TextView>(R.id.resultSummaryText)
        viewModel.resultSummary.observe(viewLifecycleOwner) { summary ->
            resultSummaryText.text = summary
        }
    }
}
