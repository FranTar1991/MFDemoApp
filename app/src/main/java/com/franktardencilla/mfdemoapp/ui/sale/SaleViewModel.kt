package com.franktardencilla.mfdemoapp.ui.sale

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franktardencilla.mfdemoapp.device.SaleDeviceResult
import com.franktardencilla.mfdemoapp.device.SaleEvent
import com.franktardencilla.mfdemoapp.domain.model.AppLogCategory
import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.Field55Builder
import com.franktardencilla.mfdemoapp.domain.model.Field55Data
import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleAmountBreakdown
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.SaleState
import com.franktardencilla.mfdemoapp.domain.model.SaleStateMachine
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyReadinessValidator
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.NetworkRepository
import com.franktardencilla.mfdemoapp.repository.PrinterRepository
import com.franktardencilla.mfdemoapp.repository.SaleRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class SaleViewModel(
    private val deviceRepository: DeviceRepository,
    private val keyRepository: KeyRepository,
    private val saleRepository: SaleRepository,
    private val transactionRepository: TransactionRepository,
    private val appLogRepository: AppLogRepository,
    private val networkRepository: NetworkRepository,
    private val printerRepository: PrinterRepository,
    private val voucherMapper: VoucherMapper
) : ViewModel() {
    private val _screenStatus = MutableLiveData("Checking sale readiness...")
    val screenStatus: LiveData<String> = _screenStatus
    private val _saleComplete = MutableLiveData(false)
    val saleComplete: LiveData<Boolean> = _saleComplete
    private val _saleActive = MutableLiveData(false)
    val saleActive: LiveData<Boolean> = _saleActive
    private val _blockingAlert = MutableLiveData<SaleBlockingAlert?>()
    val blockingAlert: LiveData<SaleBlockingAlert?> = _blockingAlert
    private val _operatorSteps = MutableLiveData(buildOperatorSteps(SaleWorkflowStep.READINESS))
    val operatorSteps: LiveData<String> = _operatorSteps
    private val _voucherSummary = MutableLiveData("No sale result yet.")
    val voucherSummary: LiveData<String> = _voucherSummary
    private val _voucherDetails = MutableLiveData(VoucherUiModel.empty())
    val voucherDetails: LiveData<VoucherUiModel> = _voucherDetails
    private val _amountSummary = MutableLiveData("Amount: not set")
    val amountSummary: LiveData<String> = _amountSummary
    private val _saleReady = MutableLiveData(false)
    val saleReady: LiveData<Boolean> = _saleReady
    private val _printStatus = MutableLiveData("")
    val printStatus: LiveData<String> = _printStatus
    private val saleStateMachine = SaleStateMachine()
    private var saleAmountBreakdown: SaleAmountBreakdown? = null
    private var emvTagSummary: EmvTagSummary? = null
    private var field55Data: Field55Data? = null

    init {
        refresh()
    }

    fun checkSaleReadiness() {
        viewModelScope.launch {
            val readinessMessage = getSaleReadinessMessage()
            val isReady = readinessMessage == null
            _saleReady.value = isReady
            _screenStatus.value = readinessMessage ?: "Ready for amount entry."
            if (!isReady && readinessMessage?.contains("Payment keys are not ready", ignoreCase = true) == true) {
                _blockingAlert.value = SaleBlockingAlert(
                    title = "Keys Not Ready",
                    message = readinessMessage
                )
            }
        }
    }

    fun startSale() {
        viewModelScope.launch {
            _saleComplete.value = false
            _saleActive.value = false
            saleStateMachine.reset()
            publishState(SaleState.CHECKING_READINESS, "Checking sale readiness")

            val currentBreakdown = saleAmountBreakdown
            if (currentBreakdown == null) {
                blockSaleStart(
                    title = "Amount Required",
                    message = "Enter an amount before starting the sale."
                )
                return@launch
            }

            val connectionStatus = deviceRepository.getConnectionStatus()
            if (!connectionStatus.isConnected) {
                blockSaleStart(
                    title = "Device Not Connected",
                    message = "Connect the device service before starting the sale."
                )
                return@launch
            }

            val networkStatus = networkRepository.getNetworkStatus()
            if (!networkStatus.isConnected) {
                blockSaleStart(
                    title = "Network Unavailable",
                    message = networkStatus.message
                )
                return@launch
            }

            val keyStatus = keyRepository.getKeyStatus()
            val keyReadiness = TrackAKeyReadinessValidator.validate(keyStatus)
            if (!keyReadiness.isReady) {
                blockSaleStart(
                    title = "Keys Not Ready",
                    message = "The transaction cannot continue because payment keys are missing or not ready.\n\n" +
                        keyReadiness.message +
                        "\n\nReconnect the terminal to inject and verify keys before starting another sale."
                )
                return@launch
            }

            _saleActive.value = true
            val request = SaleRequest(
                amount = currentBreakdown.totalAmount,
                amountBreakdown = currentBreakdown
            )
            appLogRepository.add(
                AppLogCategory.SALE,
                "Starting sale for ${request.amount.formatted()} | " +
                    request.amountBreakdown.formattedSummary().replace("\n", " | ")
            )
            val result = saleRepository.startSale(request) { event ->
                when (event) {
                    is SaleEvent.StateChanged -> publishState(event.state, event.message)
                    is SaleEvent.Progress -> {
                        _screenStatus.postValue(event.message)
                        _operatorSteps.postValue(buildOperatorSteps(event.message.toWorkflowStep()))
                        appLogRepository.add(AppLogCategory.SALE, event.message)
                    }
                    is SaleEvent.EmvDataReady -> {
                        emvTagSummary = event.summary
                        field55Data = event.summary.tags
                            .takeIf { tags -> tags.isNotEmpty() }
                            ?.let { Field55Builder.build(event.summary) }
                        _voucherSummary.postValue(
                            "Sale in progress\nAmount: ${request.amount.formatted()}\n${event.summary.toVoucherCardSummary()}"
                        )
                        appLogRepository.add(
                            AppLogCategory.EMV,
                            event.summary.toLogText()
                        )
                        field55Data?.let { data ->
                            appLogRepository.add(
                                AppLogCategory.ISO8583,
                                data.toLogText()
                            )
                        } ?: appLogRepository.add(
                            AppLogCategory.ISO8583,
                            "Field 55 skipped for magstripe transaction."
                        )
                    }
                    is SaleEvent.IsoRequestReady -> {
                        appLogRepository.add(
                            AppLogCategory.ISO8583,
                            "Authorization request | MTI=${event.summary.mti} | STAN=${event.summary.stan} | ${event.summary.redactedMessage}"
                        )
                    }
                    is SaleEvent.IsoResponseReady -> {
                        appLogRepository.add(
                            AppLogCategory.ISO8583,
                            "Authorization response | MTI=${event.summary.mti} | RC=${event.summary.responseCode ?: "unknown"} | AUTH=${event.summary.authCode ?: "none"} | ${event.summary.redactedMessage}"
                        )
                    }
                    is SaleEvent.Error -> publishState(SaleState.ERROR, event.message)
                }
            }

            when (result) {
                is SaleDeviceResult.Completed -> {
                    val savedTransaction = transactionRepository.saveSaleResult(result.saleResult)
                    appLogRepository.add(
                        AppLogCategory.SALE,
                        "Transaction saved | id=${savedTransaction.id} | status=${savedTransaction.status} | stan=${savedTransaction.stan ?: "none"}"
                    )
                    val finalState = if (result.saleResult.status == TransactionStatus.APPROVED) {
                        SaleState.APPROVED
                    } else {
                        SaleState.DECLINED
                    }
                    publishState(finalState, result.saleResult.message)
                    finishSale(
                        summary = result.saleResult.toReceiptPreview(),
                        voucher = voucherMapper.fromSaleResult(result.saleResult)
                    )
                }
                is SaleDeviceResult.Failed -> {
                    publishState(SaleState.ERROR, result.message)
                    finishSale(
                        summary = buildErrorReceiptPreview(
                            amount = request.amount,
                            message = result.message
                        ),
                        voucher = voucherMapper.error(
                            breakdown = currentBreakdown,
                            message = result.message,
                            cardLine = "CARD: ${emvTagSummary?.maskedPan?.value ?: "unavailable"}"
                        )
                    )
                }
                SaleDeviceResult.Canceled -> {
                    publishState(SaleState.CANCELED, "Sale canceled")
                    finishSale(
                        summary = buildCanceledReceiptPreview(request.amount),
                        voucher = voucherMapper.canceled(
                            breakdown = currentBreakdown,
                            cardLine = "CARD: ${emvTagSummary?.maskedPan?.value ?: "unavailable"}"
                        )
                    )
                }
            }
        }
    }

    fun setAmountBreakdown(
        baseInput: String,
        tipInput: String,
        taxInput: String
    ): Boolean {
        val breakdownResult = parseAmountBreakdown(
            baseInput = baseInput,
            tipInput = tipInput,
            taxInput = taxInput
        )
        if (breakdownResult is AmountBreakdownParseResult.Invalid) {
            _screenStatus.value = breakdownResult.message
            return false
        }

        val parsedBreakdown = (breakdownResult as AmountBreakdownParseResult.Valid).breakdown
        saleAmountBreakdown = parsedBreakdown
        _amountSummary.value = parsedBreakdown.formattedSummary()
        _screenStatus.value = "Amount accepted: ${parsedBreakdown.totalAmount.formatted()}"
        _voucherSummary.value = "Sale in progress\n${parsedBreakdown.formattedSummary()}"
        _saleComplete.value = false
        return true
    }

    fun cancelSale() {
        viewModelScope.launch {
            saleRepository.cancelSale()
            publishState(SaleState.CANCELED, "Sale cancellation requested")
            val amountText = saleAmountBreakdown?.totalAmount?.formatted() ?: "not set"
            finishSale("Canceled\nAmount: $amountText")
        }
    }

    fun resetSale() {
        saleStateMachine.reset()
        saleAmountBreakdown = null
        emvTagSummary = null
        field55Data = null
        _saleComplete.value = false
        _saleActive.value = false
        _amountSummary.value = "Amount: not set"
        _screenStatus.value = "Ready for new sale"
        _operatorSteps.value = buildOperatorSteps(SaleWorkflowStep.READINESS)
        _voucherSummary.value = "No sale result yet."
        _voucherDetails.value = VoucherUiModel.empty()
        _printStatus.value = ""
        _blockingAlert.value = null
    }

    fun clearBlockingAlert() {
        _blockingAlert.value = null
    }

    fun printVoucher(voucherBitmap: Bitmap) {
        viewModelScope.launch {
            _printStatus.value = "Printing voucher..."
            val result = printerRepository.printVoucher(voucherBitmap)
            _printStatus.value = result.message
            appLogRepository.add(
                AppLogCategory.SALE,
                if (result.isSuccess) {
                    "Voucher printed successfully."
                } else {
                    "Voucher print failed: ${result.message}"
                }
            )
        }
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

        val networkStatus = networkRepository.getNetworkStatus()
        if (!networkStatus.isConnected) {
            return networkStatus.message
        }

        val keyReadiness = TrackAKeyReadinessValidator.validate(
            keyRepository.getKeyStatus()
        )
        if (!keyReadiness.isReady) {
            return "Payment keys are not ready.\n\n" +
                "The transaction is blocked and cannot continue until the terminal keys are injected and verified.\n\n" +
                keyReadiness.message +
                "\n\nReconnect the terminal to prepare the keys, then try the sale again."
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
        _operatorSteps.postValue(buildOperatorSteps(state.toWorkflowStep()))
        appLogRepository.add(AppLogCategory.SALE, displayText)
    }

    private fun blockSaleStart(
        title: String,
        message: String
    ) {
        saleStateMachine.reset()
        val displayText = "Sale blocked\n$message"
        _screenStatus.value = displayText
        _operatorSteps.value = buildOperatorSteps(SaleWorkflowStep.READINESS)
        _saleActive.value = false
        _saleComplete.value = false
        _blockingAlert.value = SaleBlockingAlert(
            title = title,
            message = message
        )
        appLogRepository.add(AppLogCategory.SALE, displayText)
    }

    private fun finishSale(
        summary: String,
        voucher: VoucherUiModel = VoucherUiModel.empty()
    ) {
        _voucherSummary.postValue(summary)
        _voucherDetails.postValue(voucher)
        _saleComplete.postValue(true)
        _saleActive.postValue(false)
    }

    private fun SaleState.toWorkflowStep(): SaleWorkflowStep {
        return when (this) {
            SaleState.IDLE,
            SaleState.CHECKING_READINESS -> SaleWorkflowStep.READINESS
            SaleState.WAITING_FOR_CARD,
            SaleState.CARD_DETECTED -> SaleWorkflowStep.CARD
            SaleState.READING_EMV,
            SaleState.EMV_DATA_READY -> SaleWorkflowStep.EMV
            SaleState.WAITING_FOR_HOST -> SaleWorkflowStep.HOST
            SaleState.APPROVED,
            SaleState.DECLINED,
            SaleState.ERROR,
            SaleState.CANCELED -> SaleWorkflowStep.RESULT
        }
    }

    private fun String.toWorkflowStep(): SaleWorkflowStep {
        return when {
            contains("calcMac", ignoreCase = true) ||
                contains("field 64", ignoreCase = true) -> SaleWorkflowStep.MAC
            contains("host", ignoreCase = true) ||
                contains("authorization", ignoreCase = true) -> SaleWorkflowStep.HOST
            contains("emv", ignoreCase = true) ||
                contains("online data", ignoreCase = true) ||
                contains("onOnlineProc", ignoreCase = true) ||
                contains("onSelApp", ignoreCase = true) ||
                contains("onConfirmCardNo", ignoreCase = true) ||
                contains("onCardHolderInputPin", ignoreCase = true) -> SaleWorkflowStep.EMV
            contains("card", ignoreCase = true) ||
                contains("searchCard", ignoreCase = true) -> SaleWorkflowStep.CARD
            else -> SaleWorkflowStep.READINESS
        }
    }

    private enum class SaleWorkflowStep {
        READINESS,
        CARD,
        EMV,
        MAC,
        HOST,
        RESULT
    }

    private companion object {
        val AMOUNT_PATTERN = Regex("^\\d+(\\.\\d{1,2})?$")
        val receiptTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        const val MERCHANT_NAME = "MFDemo Merchant"
        const val MERCHANT_ID = "MFDemoMerchant"
        const val TERMINAL_ID = "DEMO920"
        const val RECEIPT_SEPARATOR = "------------------------------"

        fun buildOperatorSteps(currentStep: SaleWorkflowStep): String {
            val steps = listOf(
                SaleWorkflowStep.READINESS to "Check readiness",
                SaleWorkflowStep.CARD to "Read card",
                SaleWorkflowStep.EMV to "Process EMV",
                SaleWorkflowStep.MAC to "Calculate MAC",
                SaleWorkflowStep.HOST to "Send to host",
                SaleWorkflowStep.RESULT to "Finish result"
            )
            val currentIndex = steps.indexOfFirst { it.first == currentStep }.coerceAtLeast(0)
            return steps.mapIndexed { index, step ->
                val marker = when {
                    index < currentIndex -> "[x]"
                    index == currentIndex -> ">"
                    else -> "[ ]"
                }
                "$marker ${step.second}"
            }.joinToString(separator = "\n")
        }
    }

    private fun parseAmountBreakdown(
        baseInput: String,
        tipInput: String,
        taxInput: String
    ): AmountBreakdownParseResult {
        val baseAmount = parseAmount(
            input = baseInput,
            fieldName = "Base amount",
            allowZero = false
        )
        if (baseAmount is AmountParseResult.Invalid) {
            return AmountBreakdownParseResult.Invalid(baseAmount.message)
        }

        val tipAmount = parseAmount(
            input = tipInput,
            fieldName = "Tip",
            allowZero = true
        )
        if (tipAmount is AmountParseResult.Invalid) {
            return AmountBreakdownParseResult.Invalid(tipAmount.message)
        }

        val taxAmount = parseAmount(
            input = taxInput,
            fieldName = "Tax",
            allowZero = true
        )
        if (taxAmount is AmountParseResult.Invalid) {
            return AmountBreakdownParseResult.Invalid(taxAmount.message)
        }

        return runCatching {
            val breakdown = SaleAmountBreakdown.fromParts(
                baseAmount = (baseAmount as AmountParseResult.Valid).amount,
                tipAmount = (tipAmount as AmountParseResult.Valid).amount,
                taxAmount = (taxAmount as AmountParseResult.Valid).amount
            )
            breakdown.totalAmount.isoAmount12()
            AmountBreakdownParseResult.Valid(breakdown)
        }.getOrElse {
            AmountBreakdownParseResult.Invalid("Total amount is too large.")
        }
    }

    private fun parseAmount(
        input: String,
        fieldName: String,
        allowZero: Boolean
    ): AmountParseResult {
        val normalizedInput = input.trim()
        if (normalizedInput.isEmpty()) {
            if (allowZero) {
                return AmountParseResult.Valid(MoneyAmount(minorUnits = 0))
            }
            return AmountParseResult.Invalid("Enter the base amount.")
        }
        if (!AMOUNT_PATTERN.matches(normalizedInput)) {
            return AmountParseResult.Invalid("$fieldName must use a valid amount with up to 2 decimals.")
        }

        return runCatching {
            val decimalAmount = BigDecimal(normalizedInput).setScale(2)
            if (!allowZero && decimalAmount <= BigDecimal.ZERO) {
                return AmountParseResult.Invalid("Base amount must be greater than $0.00.")
            }
            if (allowZero && decimalAmount < BigDecimal.ZERO) {
                return AmountParseResult.Invalid("$fieldName cannot be negative.")
            }

            val amount = MoneyAmount(
                minorUnits = decimalAmount
                    .movePointRight(2)
                    .longValueExact()
            )
            amount.isoAmount12()
            AmountParseResult.Valid(amount)
        }.getOrElse {
            AmountParseResult.Invalid("$fieldName is too large.")
        }
    }

    private sealed interface AmountBreakdownParseResult {
        data class Valid(val breakdown: SaleAmountBreakdown) : AmountBreakdownParseResult
        data class Invalid(val message: String) : AmountBreakdownParseResult
    }

    private sealed interface AmountParseResult {
        data class Valid(val amount: MoneyAmount) : AmountParseResult
        data class Invalid(val message: String) : AmountParseResult
    }

    private fun EmvTagSummary.toVoucherCardSummary(): String {
        return listOf(
            "",
            "Card: ${maskedPan?.value ?: "unavailable"}",
            "AID: ${aid ?: "unavailable"}"
        ).joinToString(separator = "\n")
    }

    private fun EmvTagSummary.toLogText(): String {
        val tagLines = tags.joinToString(separator = "; ") { tag ->
            "${tag.tag} ${tag.label}=${tag.value}"
        }
        return "EMV data ready | AID=${aid ?: "unknown"} | PAN=${maskedPan?.value ?: "unavailable"} | $tagLines"
    }

    private fun Field55Data.toLogText(): String {
        return "Field 55 prepared | tags=${includedTags.joinToString()} | length=$byteLength bytes"
    }

    private fun com.franktardencilla.mfdemoapp.domain.model.SaleResult.toReceiptPreview(): String {
        val statusLine = when (status) {
            TransactionStatus.APPROVED -> "APPROVED"
            TransactionStatus.DECLINED -> "DECLINED"
            TransactionStatus.CANCELED -> "CANCELED"
            TransactionStatus.ERROR -> "ERROR"
            TransactionStatus.PENDING -> "PENDING"
        }
        return listOf(
            RECEIPT_SEPARATOR,
            MERCHANT_NAME,
            "TERMINAL: $TERMINAL_ID",
            "MERCHANT: $MERCHANT_ID",
            RECEIPT_SEPARATOR,
            statusLine,
            "TRANSACTION: SALE",
            "DATE/TIME: ${receiptTimeFormatter.format(Date())}",
            "AMOUNT: ${amount.formatted()}",
            "",
            "CARD: ${maskedPan?.value ?: "unavailable"}",
            "ENTRY MODE: ${entryMode?.displayName ?: "unknown"}",
            "STAN: ${stan ?: "none"}",
            "AUTH CODE: ${authCode ?: "none"}",
            "RESPONSE CODE: ${responseCode ?: "none"}",
            "",
            message,
            RECEIPT_SEPARATOR,
            "CUSTOMER COPY"
        ).joinToString(separator = "\n")
    }

    private fun buildErrorReceiptPreview(
        amount: MoneyAmount,
        message: String
    ): String {
        return listOf(
            RECEIPT_SEPARATOR,
            MERCHANT_NAME,
            "TERMINAL: $TERMINAL_ID",
            "MERCHANT: $MERCHANT_ID",
            RECEIPT_SEPARATOR,
            "ERROR",
            "TRANSACTION: SALE",
            "DATE/TIME: ${receiptTimeFormatter.format(Date())}",
            "AMOUNT: ${amount.formatted()}",
            "",
            "CARD: ${emvTagSummary?.maskedPan?.value ?: "unavailable"}",
            "ENTRY MODE: unknown",
            "STAN: none",
            "AUTH CODE: none",
            "RESPONSE CODE: none",
            "",
            message,
            RECEIPT_SEPARATOR,
            "CUSTOMER COPY"
        ).joinToString(separator = "\n")
    }

    private fun buildCanceledReceiptPreview(amount: MoneyAmount): String {
        return listOf(
            RECEIPT_SEPARATOR,
            MERCHANT_NAME,
            "TERMINAL: $TERMINAL_ID",
            "MERCHANT: $MERCHANT_ID",
            RECEIPT_SEPARATOR,
            "CANCELED",
            "TRANSACTION: SALE",
            "DATE/TIME: ${receiptTimeFormatter.format(Date())}",
            "AMOUNT: ${amount.formatted()}",
            "",
            "CARD: ${emvTagSummary?.maskedPan?.value ?: "unavailable"}",
            "ENTRY MODE: unknown",
            "STAN: none",
            "AUTH CODE: none",
            "RESPONSE CODE: none",
            "",
            "Sale canceled before completion.",
            RECEIPT_SEPARATOR,
            "CUSTOMER COPY"
        ).joinToString(separator = "\n")
    }

}

data class SaleBlockingAlert(
    val title: String,
    val message: String
)
