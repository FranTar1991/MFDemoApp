package com.franktardencilla.mfdemoapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.ui.common.appViewModelFactory

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

        val serviceStatusText = view.findViewById<TextView>(R.id.serviceStatusText)
        val moduleStatusText = view.findViewById<TextView>(R.id.moduleStatusText)
        val keyStatusText = view.findViewById<TextView>(R.id.keyStatusText)

        viewModel.serviceStatus.observe(viewLifecycleOwner) { status ->
            serviceStatusText.text = status
        }
        viewModel.moduleStatus.observe(viewLifecycleOwner) { status ->
            moduleStatusText.text = status
        }
        viewModel.keyStatus.observe(viewLifecycleOwner) { status ->
            keyStatusText.text = status
        }

        view.findViewById<Button>(R.id.connectDeviceButton).setOnClickListener {
            viewModel.connectDeviceService()
        }
        view.findViewById<Button>(R.id.disconnectDeviceButton).setOnClickListener {
            viewModel.disconnectDeviceService()
        }
        view.findViewById<Button>(R.id.refreshDeviceButton).setOnClickListener {
            viewModel.refresh()
        }
        view.findViewById<Button>(R.id.keyManagementButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_keyManagementFragment)
        }
        view.findViewById<Button>(R.id.saleButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_saleFragment)
        }
        view.findViewById<Button>(R.id.resultButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_resultFragment)
        }
        view.findViewById<Button>(R.id.logsButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_logsFragment)
        }
        view.findViewById<Button>(R.id.hostSettingsButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_hostSettingsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
