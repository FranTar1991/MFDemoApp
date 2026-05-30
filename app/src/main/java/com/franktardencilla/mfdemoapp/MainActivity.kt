package com.franktardencilla.mfdemoapp

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.franktardencilla.mfdemoapp.app.DemoApplication
import com.franktardencilla.mfdemoapp.domain.terminal.TerminalSessionResult
import com.franktardencilla.mfdemoapp.ui.common.AppViewModelFactory
import com.franktardencilla.mfdemoapp.ui.sale.SaleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val appContainer by lazy {
        (application as DemoApplication).appContainer
    }
    private lateinit var connectionButton: Button
    private lateinit var bottomNavBar: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionButton = findViewById(R.id.deviceConnectionButton)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        val navController = (
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        ).navController
        val saleViewModel = ViewModelProvider(
            this,
            AppViewModelFactory(appContainer)
        )[SaleViewModel::class.java]

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
        saleViewModel.saleActive.observe(this) { isActive ->
            setOperatorNavigationLocked(isActive)
        }

        refreshConnectionButton()
        restoreConnectedSessionIfNeeded()
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
            val dialogView = layoutInflater.inflate(R.layout.dialog_terminal_progress, null)
            val titleText = dialogView.findViewById<TextView>(R.id.dialogProgressTitle)
            val messageText = dialogView.findViewById<TextView>(R.id.dialogProgressMessage)
            val progressBar = dialogView.findViewById<ProgressBar>(R.id.dialogProgressBar)
            titleText.text = getString(R.string.device_connection_title)
            messageText.text = getString(R.string.device_connection_progress)
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(dialogView)
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
                    progressBar.isIndeterminate = false
                    progressBar.progress = PROGRESS_COMPLETE
                    titleText.text = getString(R.string.device_connected)
                    messageText.text = result.status.message
                }
                is TerminalSessionResult.Failed -> {
                    progressBar.isIndeterminate = false
                    progressBar.progress = PROGRESS_COMPLETE
                    titleText.text = "Connection failed"
                    messageText.text = result.message
                }
                TerminalSessionResult.NotRequested -> Unit
            }

            delay(DIALOG_RESULT_DELAY_MILLIS)
            dialog.dismiss()
            connectionButton.isEnabled = true
            refreshConnectionButton()
            notifyDeviceConnectionChanged()
        }
    }

    private fun restoreConnectedSessionIfNeeded() {
        if (!appContainer.terminalSessionUseCase.shouldRestoreConnectedSession()) {
            return
        }

        lifecycleScope.launch {
            val dialogView = layoutInflater.inflate(R.layout.dialog_terminal_progress, null)
            val titleText = dialogView.findViewById<TextView>(R.id.dialogProgressTitle)
            val messageText = dialogView.findViewById<TextView>(R.id.dialogProgressMessage)
            val progressBar = dialogView.findViewById<ProgressBar>(R.id.dialogProgressBar)
            titleText.text = "Restoring session"
            messageText.text = "Reopening device service session..."
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            dialog.show()
            connectionButton.isEnabled = false

            val result = appContainer.terminalSessionUseCase.restoreConnectedSession { progress ->
                runOnUiThread {
                    titleText.text = progress.title
                    messageText.text = progress.message
                }
            }
            when (result) {
                is TerminalSessionResult.Connected -> {
                    progressBar.isIndeterminate = false
                    progressBar.progress = PROGRESS_COMPLETE
                    titleText.text = getString(R.string.device_connected)
                    messageText.text = result.status.message
                }
                is TerminalSessionResult.Failed -> {
                    progressBar.isIndeterminate = false
                    progressBar.progress = PROGRESS_COMPLETE
                    titleText.text = "Session restore failed"
                    messageText.text = result.message
                }
                TerminalSessionResult.NotRequested -> {
                    dialog.dismiss()
                    connectionButton.isEnabled = true
                    refreshConnectionButton()
                    return@launch
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
            val dialogView = layoutInflater.inflate(R.layout.dialog_terminal_progress, null)
            val titleText = dialogView.findViewById<TextView>(R.id.dialogProgressTitle)
            val messageText = dialogView.findViewById<TextView>(R.id.dialogProgressMessage)
            val progressBar = dialogView.findViewById<ProgressBar>(R.id.dialogProgressBar)
            titleText.text = getString(R.string.disconnect_progress_title)
            messageText.text = getString(R.string.disconnect_progress_start)
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(dialogView)
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
            progressBar.isIndeterminate = false
            progressBar.progress = PROGRESS_COMPLETE
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

    private fun setOperatorNavigationLocked(isLocked: Boolean) {
        bottomNavBar.alpha = if (isLocked) NAV_LOCKED_ALPHA else NAV_UNLOCKED_ALPHA
        bottomNavBar.setChildrenEnabled(!isLocked)
        connectionButton.isEnabled = !isLocked
    }

    private fun View.setChildrenEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        if (this is ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).setChildrenEnabled(isEnabled)
            }
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
        const val PROGRESS_COMPLETE = 100
        const val NAV_LOCKED_ALPHA = 0.38f
        const val NAV_UNLOCKED_ALPHA = 1f
    }
}
