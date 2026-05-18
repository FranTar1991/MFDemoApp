package com.franktardencilla.mfdemoapp.ui.keys

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
        val keyDetailsText = view.findViewById<TextView>(R.id.keyDetailsText)
        val injectionLogText = view.findViewById<TextView>(R.id.injectionLogText)

        viewModel.keyStatus.observe(viewLifecycleOwner) { status ->
            keyStatusText.text = status
        }
        viewModel.keyDetails.observe(viewLifecycleOwner) { details ->
            keyDetailsText.text = details
        }
        viewModel.injectionLog.observe(viewLifecycleOwner) { log ->
            injectionLogText.text = log
        }

        view.findViewById<Button>(R.id.injectTrackAKeysButton).setOnClickListener {
            viewModel.injectTrackADemoKeys()
        }
        view.findViewById<Button>(R.id.clearKeysButton).setOnClickListener {
            showClearKeysConfirmation()
        }
        view.findViewById<Button>(R.id.refreshKeysButton).setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun showClearKeysConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_keys_warning_title)
            .setMessage(R.string.clear_keys_warning_message)
            .setPositiveButton(R.string.clear_keys) { _, _ ->
                viewModel.clearKeys()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
