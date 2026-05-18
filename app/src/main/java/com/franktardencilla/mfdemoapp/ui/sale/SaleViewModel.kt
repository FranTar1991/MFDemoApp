package com.franktardencilla.mfdemoapp.ui.sale

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.device.SaleDeviceResult
import com.franktardencilla.mfdemoapp.device.SaleEvent
import com.franktardencilla.mfdemoapp.domain.model.AppLogCategory
import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.SaleState
import com.franktardencilla.mfdemoapp.domain.model.SaleStateMachine
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.SaleRepository
import java.math.BigDecimal
import kotlinx.coroutines.launch

class SaleViewModel(
    private val deviceRepository: DeviceRepository,
    private val keyRepository: KeyRepository,
    private val saleRepository: SaleRepository,
    private val appLogRepository: AppLogRepository
) : ViewModel() {
    private val _screenStatus = MutableLiveData("Checking sale readiness...")
    val screenStatus: LiveData<String> = _screenStatus
    private val _saleComplete = MutableLiveData(false)
    val saleComplete: LiveData<Boolean> = _saleComplete
    private val _voucherSummary = MutableLiveData("No sale result yet.")
    val voucherSummary: LiveData<String> = _voucherSummary
    private val _amountSummary = MutableLiveData("Amount: not set")
    val amountSummary: LiveData<String> = _amountSummary
    private val _saleReady = MutableLiveData(false)
    val saleReady: LiveData<Boolean> = _saleReady
    private val saleStateMachine = SaleStateMachine()
    private var saleAmount: MoneyAmount? = null

    init {
        refresh()
    }

    fun checkSaleReadiness() {
        viewModelScope.launch {
            val readinessMessage = getSaleReadinessMessage()
            val isReady = readinessMessage == null
            _saleReady.value = isReady
            _screenStatus.value = readinessMessage ?: "Ready for amount entry."
        }
    }

    fun startSale() {
        viewModelScope.launch {
            _saleComplete.value = false
            saleStateMachine.reset()
            publishState(SaleState.CHECKING_READINESS, "Checking sale readiness")

            val currentAmount = saleAmount
            if (currentAmount == null) {
                publishState(SaleState.ERROR, "Enter an amount before starting sale")
                finishSale("Sale could not start.\nAmount missing.")
                return@launch
            }

            val connectionStatus = deviceRepository.getConnectionStatus()
            if (!connectionStatus.isConnected) {
                publishState(SaleState.ERROR, "Device service must be connected before sale")
                finishSale("Sale could not start.\nDevice service is disconnected.")
                return@launch
            }

            val keyStatus = keyRepository.getKeyStatus()
            val keyReadiness = TrackAKeyReadinessValidator.validate(keyStatus)
            if (!keyReadiness.isReady) {
                publishState(SaleState.ERROR, keyReadiness.message)
                finishSale("Sale could not start.\n${keyReadiness.message}")
                return@launch
            }

            val request = SaleRequest(
                amount = currentAmount
            )
            appLogRepository.add(
                AppLogCategory.SALE,
                "Starting mock sale for ${request.amount.formatted()}"
            )
            val result = saleRepository.startSale(request) { event ->
                when (event) {
                    is SaleEvent.StateChanged -> publishState(event.state, event.message)
                    is SaleEvent.Progress -> {
                        _screenStatus.postValue(event.message)
                        appLogRepository.add(AppLogCategory.SALE, event.message)
                    }
                    is SaleEvent.Error -> publishState(SaleState.ERROR, event.message)
                }
            }

            when (result) {
                is SaleDeviceResult.Completed -> {
                    publishState(SaleState.APPROVED, result.saleResult.message)
                    finishSale(
                        "Approved\nAmount: ${request.amount.formatted()}\n${result.saleResult.message}"
                    )
                }
                is SaleDeviceResult.Failed -> {
                    publishState(SaleState.ERROR, result.message)
                    finishSale(
                        "Not approved\nAmount: ${request.amount.formatted()}\n${result.message}"
                    )
                }
                SaleDeviceResult.Canceled -> {
                    publishState(SaleState.CANCELED, "Sale canceled")
                    finishSale(
                        "Canceled\nAmount: ${request.amount.formatted()}"
                    )
                }
            }
        }
    }

    fun setAmount(input: String): Boolean {
        val amountResult = parseAmount(input)
        if (amountResult is AmountParseResult.Invalid) {
            _screenStatus.value = amountResult.message
            return false
        }

        val parsedAmount = (amountResult as AmountParseResult.Valid).amount
        saleAmount = parsedAmount
        _amountSummary.value = "Amount: ${parsedAmount.formatted()}"
        _screenStatus.value = "Amount accepted: ${parsedAmount.formatted()}"
        _voucherSummary.value = "Sale in progress\nAmount: ${parsedAmount.formatted()}"
        _saleComplete.value = false
        return true
    }

    fun cancelSale() {
        viewModelScope.launch {
            saleRepository.cancelSale()
            publishState(SaleState.CANCELED, "Sale cancellation requested")
            val amountText = saleAmount?.formatted() ?: "not set"
            finishSale("Canceled\nAmount: $amountText")
        }
    }

    fun resetSale() {
        saleStateMachine.reset()
        saleAmount = null
        _saleComplete.value = false
        _amountSummary.value = "Amount: not set"
        _screenStatus.value = "Ready for new sale"
        _voucherSummary.value = "No sale result yet."
    }

    fun refresh() {
        viewModelScope.launch {
            _screenStatus.value = listOf(
                deviceRepository.getConnectionStatus().message,
                keyRepository.getKeyStatus().message
            ).joinToString(separator = "\n")
        }
    }

    private suspend fun getSaleReadinessMessage(): String? {
        val connectionStatus = deviceRepository.getConnectionStatus()
        if (!connectionStatus.isConnected) {
            return "Connect device service before starting a sale."
        }

        val keyReadiness = TrackAKeyReadinessValidator.validate(
            keyRepository.getKeyStatus()
        )
        if (!keyReadiness.isReady) {
            return "Inject Track A keys before starting a sale.\n${keyReadiness.message}"
        }

        return null
    }

    private fun publishState(
        state: SaleState,
        message: String
    ) {
        val transition = runCatching {
            saleStateMachine.transitionTo(state, message)
        }.getOrElse {
            saleStateMachine.reset()
            saleStateMachine.transitionTo(state, message)
        }
        val displayText = "${transition.state.displayName}\n${transition.message}"
        _screenStatus.postValue(displayText)
        appLogRepository.add(AppLogCategory.SALE, displayText)
    }

    private fun finishSale(summary: String) {
        _voucherSummary.postValue(summary)
        _saleComplete.postValue(true)
    }

    private fun parseAmount(input: String): AmountParseResult {
        val normalizedInput = input.trim()
        if (normalizedInput.isEmpty()) {
            return AmountParseResult.Invalid("Enter an amount.")
        }
        if (!AMOUNT_PATTERN.matches(normalizedInput)) {
            return AmountParseResult.Invalid("Use a valid amount with up to 2 decimals.")
        }

        return runCatching {
            val decimalAmount = BigDecimal(normalizedInput).setScale(2)
            if (decimalAmount <= BigDecimal.ZERO) {
                return AmountParseResult.Invalid("Amount must be greater than $0.00.")
            }

            val amount = MoneyAmount(
                minorUnits = decimalAmount
                    .movePointRight(2)
                    .longValueExact()
            )
            amount.isoAmount12()
            AmountParseResult.Valid(amount)
        }.getOrElse {
            AmountParseResult.Invalid("Amount is too large.")
        }
    }

    private sealed interface AmountParseResult {
        data class Valid(val amount: MoneyAmount) : AmountParseResult
        data class Invalid(val message: String) : AmountParseResult
    }

    private companion object {
        val AMOUNT_PATTERN = Regex("^\\d+(\\.\\d{1,2})?$")
    }
}
