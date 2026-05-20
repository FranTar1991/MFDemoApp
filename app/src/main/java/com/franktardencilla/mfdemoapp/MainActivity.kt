package com.franktardencilla.mfdemoapp

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.franktardencilla.mfdemoapp.app.DemoApplication
import com.franktardencilla.mfdemoapp.domain.terminal.TerminalSessionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val appContainer by lazy {
        (application as DemoApplication).appContainer
    }
    private lateinit var connectionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionButton = findViewById(R.id.deviceConnectionButton)
        val navController = (
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        ).navController

        connectionButton.setOnClickListener {
            showConnectionMenu()
        }
        findViewById<View>(R.id.bottomHomeButton).setOnClickListener {
            navController.navigateSingleTop(R.id.homeFragment)
        }
        findViewById<View>(R.id.bottomKeysButton).setOnClickListener {
            navController.navigateSingleTop(R.id.keyManagementFragment)
        }
        findViewById<View>(R.id.bottomSaleButton).setOnClickListener {
            navController.navigateSingleTop(R.id.saleAmountFragment)
        }
        findViewById<View>(R.id.bottomLogsButton).setOnClickListener {
            navController.navigateSingleTop(R.id.logsFragment)
        }
        findViewById<View>(R.id.bottomHostButton).setOnClickListener {
            navController.navigateSingleTop(R.id.hostSettingsFragment)
        }

        refreshConnectionButton()
    }

    private fun NavController.navigateSingleTop(destinationId: Int) {
        if (currentDestination?.id == destinationId) {
            return
        }
        navigate(destinationId)
    }

    private fun showConnectionMenu() {
        lifecycleScope.launch {
            val isConnected = appContainer.deviceRepository.getConnectionStatus().isConnected
            PopupMenu(this@MainActivity, connectionButton).apply {
                if (isConnected) {
                    menu.add(getString(R.string.device_disconnect))
                } else {
                    menu.add(getString(R.string.device_connect))
                }
                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        getString(R.string.device_connect) -> {
                            connectWithModuleCheck()
                            true
                        }
                        getString(R.string.device_disconnect) -> {
                            confirmDisconnectAndErase()
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    private fun connectWithModuleCheck() {
        lifecycleScope.launch {
            val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
            val titleText = dialogView.findViewById<TextView>(android.R.id.text1)
            val messageText = dialogView.findViewById<TextView>(android.R.id.text2)
            titleText.text = getString(R.string.device_connection_title)
            messageText.text = getString(R.string.device_connection_progress)

            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 36, 48, 24)
                addView(dialogView)
                addView(ProgressBar(this@MainActivity).apply {
                    isIndeterminate = true
                })
            }
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(container)
                .setCancelable(false)
                .create()
            dialog.show()
            connectionButton.isEnabled = false

            val result = appContainer.terminalSessionUseCase.connectAndPrepare { progress ->
                runOnUiThread {
                    titleText.text = progress.title
                    messageText.text = progress.message
                }
            }
            when (result) {
                is TerminalSessionResult.Connected -> {
                    titleText.text = getString(R.string.device_connected)
                    messageText.text = result.status.message
                }
                is TerminalSessionResult.Failed -> {
                    titleText.text = "Connection failed"
                    messageText.text = result.message
                }
            }

            delay(DIALOG_RESULT_DELAY_MILLIS)
            dialog.dismiss()
            connectionButton.isEnabled = true
            refreshConnectionButton()
            notifyDeviceConnectionChanged()
        }
    }

    private fun confirmDisconnectAndErase() {
        AlertDialog.Builder(this)
            .setTitle(R.string.disconnect_warning_title)
            .setMessage(R.string.disconnect_warning_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.disconnect_and_erase) { _, _ ->
                disconnectAndEraseData()
            }
            .show()
    }

    private fun disconnectAndEraseData() {
        lifecycleScope.launch {
            val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
            val titleText = dialogView.findViewById<TextView>(android.R.id.text1)
            val messageText = dialogView.findViewById<TextView>(android.R.id.text2)
            titleText.text = getString(R.string.disconnect_progress_title)
            messageText.text = getString(R.string.disconnect_progress_start)

            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 36, 48, 24)
                addView(dialogView)
                addView(ProgressBar(this@MainActivity).apply {
                    isIndeterminate = true
                })
            }
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(container)
                .setCancelable(false)
                .create()
            dialog.show()
            connectionButton.isEnabled = false

            appContainer.terminalSessionUseCase.disconnectAndErase { progress ->
                runOnUiThread {
                    titleText.text = progress.title
                    messageText.text = progress.message
                }
            }

            titleText.text = getString(R.string.disconnect_complete_title)
            messageText.text = getString(R.string.disconnect_complete_message)
            delay(DIALOG_RESULT_DELAY_MILLIS)
            dialog.dismiss()

            connectionButton.isEnabled = true
            refreshConnectionButton()
            notifyDeviceConnectionChanged()
            notifyDataCleared()
            navController().navigateSingleTop(R.id.homeFragment)
        }
    }

    private fun refreshConnectionButton() {
        lifecycleScope.launch {
            val isConnected = appContainer.deviceRepository.getConnectionStatus().isConnected
            connectionButton.text = if (isConnected) {
                getString(R.string.device_connected)
            } else {
                getString(R.string.device_disconnected)
            }
            val color = if (isConnected) {
                getColor(R.color.connection_connected)
            } else {
                getColor(R.color.connection_disconnected)
            }
            connectionButton.backgroundTintList = ColorStateList.valueOf(color)
            connectionButton.setTextColor(getColor(R.color.white))
        }
    }

    private fun notifyDeviceConnectionChanged() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment ?: return
        navHostFragment.childFragmentManager.setFragmentResult(
            DEVICE_CONNECTION_CHANGED_REQUEST_KEY,
            Bundle.EMPTY
        )
    }

    private fun notifyDataCleared() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment ?: return
        navHostFragment.childFragmentManager.setFragmentResult(
            DATA_CLEARED_REQUEST_KEY,
            Bundle.EMPTY
        )
    }

    private fun navController(): NavController {
        return (
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        ).navController
    }

    companion object {
        const val DEVICE_CONNECTION_CHANGED_REQUEST_KEY = "device_connection_changed"
        const val DATA_CLEARED_REQUEST_KEY = "data_cleared"
        const val DIALOG_RESULT_DELAY_MILLIS = 900L
    }
}
