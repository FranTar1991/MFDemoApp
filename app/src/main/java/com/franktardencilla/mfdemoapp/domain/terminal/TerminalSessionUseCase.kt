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
import com.franktardencilla.mfdemoapp.repository.NetworkRepository
import com.franktardencilla.mfdemoapp.repository.TerminalSessionPreferenceRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import kotlinx.coroutines.delay

class TerminalSessionUseCase(
    private val deviceRepository: DeviceRepository,
    private val keyRepository: KeyRepository,
    private val networkRepository: NetworkRepository,
    private val transactionRepository: TransactionRepository,
    private val appLogRepository: AppLogRepository,
    private val terminalSessionPreferenceRepository: TerminalSessionPreferenceRepository
) {
    fun shouldRestoreConnectedSession(): Boolean {
        return terminalSessionPreferenceRepository.shouldRestoreConnectedSession()
    }

    suspend fun connectAndPrepare(
        progress: (TerminalProgress) -> Unit
    ): TerminalSessionResult {
        val result = openAndValidateSession(
            progress = progress,
            openingTitle = "Connecting",
            openingMessage = "Opening device service session...",
            keyReadyLog = "Track A keys already ready during device connection",
            keyInjectionStartLog = "Starting Track A key injection during device connection",
            keyInjectionFinishedLog = "Track A key injection finished during device connection"
        )
        terminalSessionPreferenceRepository.setShouldRestoreConnectedSession(
            result is TerminalSessionResult.Connected
        )
        return result
    }

    suspend fun restoreConnectedSession(
        progress: (TerminalProgress) -> Unit
    ): TerminalSessionResult {
        if (!terminalSessionPreferenceRepository.shouldRestoreConnectedSession()) {
            return TerminalSessionResult.NotRequested
        }

        val result = openAndValidateSession(
            progress = progress,
            openingTitle = "Restoring session",
            openingMessage = "Reopening device service session...",
            keyReadyLog = "Track A keys found during session restore",
            keyInjectionStartLog = "Track A keys missing during restore; starting injection",
            keyInjectionFinishedLog = "Track A key injection finished during session restore"
        )
        terminalSessionPreferenceRepository.setShouldRestoreConnectedSession(
            result is TerminalSessionResult.Connected
        )
        return result
    }

    suspend fun disconnectAndErase(
        progress: (TerminalProgress) -> Unit
    ) {
        terminalSessionPreferenceRepository.setShouldRestoreConnectedSession(false)

        progress(TerminalProgress("Disconnecting", "Deleting stored transactions..."))
        transactionRepository.clearTransactions()
        delay(PROGRESS_STEP_DELAY_MILLIS)

        progress(TerminalProgress("Disconnecting", "Deleting loaded keys..."))
        keyRepository.clearKeys()
        delay(PROGRESS_STEP_DELAY_MILLIS)

        progress(TerminalProgress("Disconnecting", "Deleting app logs..."))
        appLogRepository.clearAndWait()
        delay(PROGRESS_STEP_DELAY_MILLIS)

        progress(TerminalProgress("Disconnecting", "Closing device service session..."))
        deviceRepository.disconnect()
        delay(PROGRESS_STEP_DELAY_MILLIS)
    }

    private suspend fun openAndValidateSession(
        progress: (TerminalProgress) -> Unit,
        openingTitle: String,
        openingMessage: String,
        keyReadyLog: String,
        keyInjectionStartLog: String,
        keyInjectionFinishedLog: String
    ): TerminalSessionResult {
        progress(TerminalProgress(openingTitle, openingMessage))
        val connectionStatus = deviceRepository.connect()
        if (!connectionStatus.isConnected) {
            return TerminalSessionResult.Failed(connectionStatus.message)
        }

        val session = deviceRepository.getSession()
        if (session == null) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed("Device session unavailable. Try reconnecting.")
        }

        val moduleValidation = validateModules(
            session.modules.toConnectionChecks(),
            progress
        )
        if (moduleValidation is ModuleValidationResult.Failed) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed(moduleValidation.message)
        }
        val statusLines = (moduleValidation as ModuleValidationResult.Ready).statusLines

        val networkFailure = validateNetwork(statusLines, progress)
        if (networkFailure != null) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed(networkFailure)
        }

        val keyResult = ensureKeysReady(
            statusLines = statusLines,
            progress = progress,
            keyReadyLog = keyReadyLog,
            keyInjectionStartLog = keyInjectionStartLog,
            keyInjectionFinishedLog = keyInjectionFinishedLog
        )
        if (keyResult is KeyPreparationResult.Failed) {
            deviceRepository.disconnect()
            return TerminalSessionResult.Failed(keyResult.message)
        }

        return TerminalSessionResult.Connected(
            DeviceConnectionStatus(
                isConnected = true,
                message = statusLines.joinToString("\n") + "\nKeys: ${keyResult.message}"
            )
        )
    }

    private suspend fun validateModules(
        checks: List<ModuleCheck>,
        progress: (TerminalProgress) -> Unit
    ): ModuleValidationResult {
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
            return ModuleValidationResult.Failed("${requiredFailure.name} is required but unavailable.")
        }
        return ModuleValidationResult.Ready(statusLines)
    }

    private suspend fun validateNetwork(
        statusLines: MutableList<String>,
        progress: (TerminalProgress) -> Unit
    ): String? {
        progress(
            TerminalProgress(
                title = "Checking Android network",
                message = statusLines.joinToString("\n") + "\nChecking POS network connectivity..."
            )
        )
        val networkStatus = networkRepository.getNetworkStatus()
        statusLines += "Android network: ${if (networkStatus.isConnected) "available" else "unavailable"}"
        return if (networkStatus.isConnected) null else networkStatus.message
    }

    private suspend fun ensureKeysReady(
        statusLines: MutableList<String>,
        progress: (TerminalProgress) -> Unit,
        keyReadyLog: String,
        keyInjectionStartLog: String,
        keyInjectionFinishedLog: String
    ): KeyPreparationResult {
        progress(
            TerminalProgress(
                title = "Checking keys",
                message = statusLines.joinToString("\n") + "\nChecking Track A key readiness..."
            )
        )
        val existingKeyStatus = keyRepository.getKeyStatus()
        val existingReadiness = TrackAKeyReadinessValidator.validate(existingKeyStatus)
        if (existingReadiness.isReady) {
            appLogRepository.add(AppLogCategory.KEYS, keyReadyLog)
            return KeyPreparationResult.Ready(existingReadiness.message)
        }

        progress(
            TerminalProgress(
                title = "Injecting keys",
                message = statusLines.joinToString("\n") + "\nPreparing Track A keys..."
            )
        )
        appLogRepository.add(AppLogCategory.KEYS, keyInjectionStartLog)
        val injectedKeyStatus = keyRepository.injectTrackAKeys(
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

        val injectedReadiness = TrackAKeyReadinessValidator.validate(injectedKeyStatus)
        if (!injectedReadiness.isReady) {
            val failureMessage = "Error: ${injectedReadiness.message}\n${injectedKeyStatus.message}"
            appLogRepository.add(AppLogCategory.KEYS, failureMessage)
            return KeyPreparationResult.Failed(failureMessage)
        }

        appLogRepository.add(AppLogCategory.KEYS, keyInjectionFinishedLog)
        return KeyPreparationResult.Ready(injectedReadiness.message)
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

    private sealed interface KeyPreparationResult {
        val message: String

        data class Ready(override val message: String) : KeyPreparationResult
        data class Failed(override val message: String) : KeyPreparationResult
    }

    private sealed interface ModuleValidationResult {
        data class Ready(val statusLines: MutableList<String>) : ModuleValidationResult
        data class Failed(val message: String) : ModuleValidationResult
    }

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
    data object NotRequested : TerminalSessionResult
}
