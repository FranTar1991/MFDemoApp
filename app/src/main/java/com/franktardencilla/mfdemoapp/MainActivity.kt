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
import com.franktardencilla.mfdemoapp.device.TrackAKeyInjectionEvent
import com.franktardencilla.mfdemoapp.domain.model.AppLogCategory
import com.franktardencilla.mfdemoapp.domain.model.DeviceModuleAvailability
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
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
                            disconnectDevice()
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

            titleText.text = "Connecting"
            messageText.text = "Opening device service session..."
            val connectionStatus = appContainer.deviceRepository.connect()
            if (!connectionStatus.isConnected) {
                messageText.text = connectionStatus.message
                delay(DIALOG_RESULT_DELAY_MILLIS)
                dialog.dismiss()
                connectionButton.isEnabled = true
                refreshConnectionButton()
                notifyDeviceConnectionChanged()
                return@launch
            }

            val session = appContainer.deviceRepository.getSession()
            if (session == null) {
                messageText.text = "Device session unavailable. Try reconnecting."
                appContainer.deviceRepository.disconnect()
                delay(DIALOG_RESULT_DELAY_MILLIS)
                dialog.dismiss()
                connectionButton.isEnabled = true
                refreshConnectionButton()
                notifyDeviceConnectionChanged()
                return@launch
            }

            val checks = session.modules.toConnectionChecks()
            val statusLines = mutableListOf<String>()
            checks.forEach { check ->
                titleText.text = "Checking ${check.name}"
                delay(MODULE_CHECK_DELAY_MILLIS)
                statusLines += "${check.name}: ${if (check.available) "available" else "unavailable"}"
                messageText.text = statusLines.joinToString(separator = "\n")
            }

            val requiredFailure = checks.firstOrNull { check ->
                check.required && !check.available
            }
            if (requiredFailure == null) {
                titleText.text = "Injecting keys"
                messageText.text = statusLines.joinToString(separator = "\n") +
                    "\nPreparing Track A keys..."
                appContainer.appLogRepository.add(
                    AppLogCategory.KEYS,
                    "Starting Track A key injection during device connection"
                )
                val keyStatus = appContainer.keyRepository.injectTrackAKeys(
                    request = TrackAKeyInjectionRequest.demo()
                ) { event ->
                    when (event) {
                        is TrackAKeyInjectionEvent.Progress -> {
                            appContainer.appLogRepository.add(AppLogCategory.KEYS, event.message)
                            runOnUiThread {
                                messageText.text = statusLines.joinToString(separator = "\n") +
                                    "\n${event.message}"
                            }
                        }
                    }
                }
                val keyReadiness = TrackAKeyReadinessValidator.validate(keyStatus)
                if (!keyReadiness.isReady) {
                    titleText.text = "Connection failed"
                    messageText.text = "Keys are not ready: ${keyReadiness.message}"
                    appContainer.deviceRepository.disconnect()
                    delay(DIALOG_RESULT_DELAY_MILLIS)
                    dialog.dismiss()
                    connectionButton.isEnabled = true
                    refreshConnectionButton()
                    notifyDeviceConnectionChanged()
                    return@launch
                }
                appContainer.appLogRepository.add(
                    AppLogCategory.KEYS,
                    "Track A key injection finished during device connection"
                )
                titleText.text = "Connected"
                messageText.text = statusLines.joinToString(separator = "\n") +
                    "\nKeys: ${keyReadiness.message}"
            } else {
                titleText.text = "Connection failed"
                messageText.text = "${requiredFailure.name} is required but unavailable."
                appContainer.deviceRepository.disconnect()
            }

            delay(DIALOG_RESULT_DELAY_MILLIS)
            dialog.dismiss()
            connectionButton.isEnabled = true
            refreshConnectionButton()
            notifyDeviceConnectionChanged()
        }
    }

    private fun disconnectDevice() {
        lifecycleScope.launch {
            connectionButton.isEnabled = false
            appContainer.deviceRepository.disconnect()
            connectionButton.isEnabled = true
            refreshConnectionButton()
            notifyDeviceConnectionChanged()
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

    private fun DeviceModuleAvailability.toConnectionChecks(): List<ModuleCheck> {
        return listOf(
            ModuleCheck("EMV", emvAvailable, required = true),
            ModuleCheck("PinPad", pinPadAvailable, required = true),
            ModuleCheck("PED", pedAvailable, required = true),
            ModuleCheck("Network", networkAvailable, required = true),
            ModuleCheck("Printer", printerAvailable, required = false),
            ModuleCheck("Beeper", beeperAvailable, required = false)
        )
    }

    private data class ModuleCheck(
        val name: String,
        val available: Boolean,
        val required: Boolean
    )

    companion object {
        const val DEVICE_CONNECTION_CHANGED_REQUEST_KEY = "device_connection_changed"
        const val MODULE_CHECK_DELAY_MILLIS = 350L
        const val DIALOG_RESULT_DELAY_MILLIS = 900L
    }
}
