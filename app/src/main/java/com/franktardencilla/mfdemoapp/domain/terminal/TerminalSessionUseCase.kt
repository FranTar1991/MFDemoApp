package com.franktardencilla.mfdemoapp.domain.terminal

import com.franktardencilla.mfdemoapp.device.TrackAKeyInjectionEvent
import com.franktardencilla.mfdemoapp.domain.model.AppLogCategory
import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.DeviceModuleAvailability
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import kotlinx.coroutines.delay

class TerminalSessionUseCase(
    private val deviceRepository: DeviceRepository,
    private val keyRepository: KeyRepository,
    private val transactionRepository: TransactionRepository,
    private val appLogRepository: AppLogRepository
) {
    suspend fun connectAndPrepare(
        progress: (TerminalProgress) -> Unit
    ): TerminalSessionResult {
        progress(TerminalProgress("Connecting", "Opening device service session..."))
        val connectionStatus = deviceRepository.connect()
        if (!connectionStatus.isConnected) {
            return TerminalSessionResult.Failed(connectionStatus.message)
        }

        val session = deviceRepository.getSession()
        if (session == null) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed("Device session unavailable. Try reconnecting.")
        }

        val checks = session.modules.toConnectionChecks()
        val statusLines = mutableListOf<String>()
        checks.forEach { check ->
            progress(TerminalProgress("Checking ${check.name}", statusLines.joinToString("\n")))
            delay(PROGRESS_STEP_DELAY_MILLIS)
            statusLines += "${check.name}: ${if (check.available) "available" else "unavailable"}"
        }

        val requiredFailure = checks.firstOrNull { check ->
            check.required && !check.available
        }
        if (requiredFailure != null) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed("${requiredFailure.name} is required but unavailable.")
        }

        progress(
            TerminalProgress(
                title = "Injecting keys",
                message = statusLines.joinToString("\n") + "\nPreparing Track A keys..."
            )
        )
        appLogRepository.add(
            AppLogCategory.KEYS,
            "Starting Track A key injection during device connection"
        )
        val keyStatus = keyRepository.injectTrackAKeys(
            request = TrackAKeyInjectionRequest.demo()
        ) { event ->
            when (event) {
                is TrackAKeyInjectionEvent.Progress -> {
                    appLogRepository.add(AppLogCategory.KEYS, event.message)
                    progress(
                        TerminalProgress(
                            title = "Injecting keys",
                            message = statusLines.joinToString("\n") + "\n${event.message}"
                        )
                    )
                }
            }
        }

        val keyReadiness = TrackAKeyReadinessValidator.validate(keyStatus)
        if (!keyReadiness.isReady) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed("Keys are not ready: ${keyReadiness.message}")
        }

        appLogRepository.add(
            AppLogCategory.KEYS,
            "Track A key injection finished during device connection"
        )
        return TerminalSessionResult.Connected(
            DeviceConnectionStatus(
                isConnected = true,
                message = statusLines.joinToString("\n") + "\nKeys: ${keyReadiness.message}"
            )
        )
    }

    suspend fun disconnectAndErase(
        progress: (TerminalProgress) -> Unit
    ) {
        progress(TerminalProgress("Disconnecting", "Deleting stored transactions..."))
        transactionRepository.clearTransactions()
        delay(PROGRESS_STEP_DELAY_MILLIS)

        progress(TerminalProgress("Disconnecting", "Deleting loaded mock keys..."))
        keyRepository.clearKeys()
        delay(PROGRESS_STEP_DELAY_MILLIS)

        progress(TerminalProgress("Disconnecting", "Deleting app logs..."))
        appLogRepository.clearAndWait()
        delay(PROGRESS_STEP_DELAY_MILLIS)

        progress(TerminalProgress("Disconnecting", "Closing device service session..."))
        deviceRepository.disconnect()
        delay(PROGRESS_STEP_DELAY_MILLIS)
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

    private companion object {
        const val PROGRESS_STEP_DELAY_MILLIS = 350L
    }
}

data class TerminalProgress(
    val title: String,
    val message: String
)

sealed interface TerminalSessionResult {
    data class Connected(val status: DeviceConnectionStatus) : TerminalSessionResult
    data class Failed(val message: String) : TerminalSessionResult
}
