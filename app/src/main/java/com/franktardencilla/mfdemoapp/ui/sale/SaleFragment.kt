package com.franktardencilla.mfdemoapp.ui.sale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class SaleFragment : Fragment() {
    private val viewModel: SaleViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sale, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saleStatusText = view.findViewById<TextView>(R.id.saleStatusText)
        viewModel.screenStatus.observe(viewLifecycleOwner) { status ->
            saleStatusText.text = status
        }
    }
}
