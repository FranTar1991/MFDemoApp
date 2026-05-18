package com.franktardencilla.mfdemoapp.ui.keys

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class KeyManagementFragment : Fragment() {
    private val viewModel: KeyManagementViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_key_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val keyStatusText = view.findViewById<TextView>(R.id.keyStatusText)
        viewModel.keyStatus.observe(viewLifecycleOwner) { status ->
            keyStatusText.text = status
        }
    }
}
