package com.franktardencilla.mfdemoapp.ui.host

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

class HostSettingsFragment : Fragment() {
    private val viewModel: HostSettingsViewModel by viewModels {
        appViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_host_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val primaryHostInput = view.findViewById<EditText>(R.id.primaryHostInput)
        val fallbackHostInput = view.findViewById<EditText>(R.id.fallbackHostInput)
        val portInput = view.findViewById<EditText>(R.id.portInput)
        val timeoutInput = view.findViewById<EditText>(R.id.timeoutInput)
        val statusText = view.findViewById<TextView>(R.id.hostSettingsStatusText)

        viewModel.hostConfig.observe(viewLifecycleOwner) { config ->
            primaryHostInput.setText(config.primaryHost)
            fallbackHostInput.setText(config.fallbackHost)
            portInput.setText(config.port.toString())
            timeoutInput.setText(config.timeoutMillis.toString())
        }
        viewModel.status.observe(viewLifecycleOwner) { status ->
            statusText.text = status
        }

        view.findViewById<Button>(R.id.saveHostSettingsButton).setOnClickListener {
            viewModel.save(
                primaryHost = primaryHostInput.text.toString(),
                fallbackHost = fallbackHostInput.text.toString(),
                portText = portInput.text.toString(),
                timeoutText = timeoutInput.text.toString()
            )
        }
        view.findViewById<Button>(R.id.testHostConnectionButton).setOnClickListener {
            viewModel.testConnection(
                primaryHost = primaryHostInput.text.toString(),
                fallbackHost = fallbackHostInput.text.toString(),
                portText = portInput.text.toString(),
                timeoutText = timeoutInput.text.toString()
            )
        }
    }
}
