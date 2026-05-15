package com.franktardencilla.mfdemoapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.franktardencilla.mfdemoapp.R

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }
}
