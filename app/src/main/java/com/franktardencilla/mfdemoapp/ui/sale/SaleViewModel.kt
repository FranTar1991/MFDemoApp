package com.franktardencilla.mfdemoapp.ui.sale

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
    private val appLogRepository: AppLogRepository
) : ViewModel() {
    private val _screenStatus = MutableLiveData("Checking sale readiness...")
    val screenStatus: LiveData<String> = _screenStatus
    private val _saleComplete = MutableLiveData(false)
    val saleComplete: LiveData<Boolean> = _saleComplete
    private val _voucherSummary = MutableLiveData("No sale result yet.")
    val voucherSummary: LiveData<String> = _voucherSummary
    private val _voucherDetails = MutableLiveData(VoucherUiModel.empty())
    val voucherDetails: LiveData<VoucherUiModel> = _voucherDetails
    private val _amountSummary = MutableLiveData("Amount: not set")
    val amountSummary: LiveData<String> = _amountSummary
    private val _saleReady = MutableLiveData(false)
    val saleReady: LiveData<Boolean> = _saleReady
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
        }
    }

    fun startSale() {
        viewModelScope.launch {
            _saleComplete.value = false
            saleStateMachine.reset()
            publishState(SaleState.CHECKING_READINESS, "Checking sale readiness")

            val currentBreakdown = saleAmountBreakdown
            if (currentBreakdown == null) {
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
                amount = currentBreakdown.totalAmount,
                amountBreakdown = currentBreakdown
            )
            appLogRepository.add(
                AppLogCategory.SALE,
                "Starting mock sale for ${request.amount.formatted()} | " +
                    request.amountBreakdown.formattedSummary().replace("\n", " | ")
            )
            val result = saleRepository.startSale(request) { event ->
                when (event) {
                    is SaleEvent.StateChanged -> publishState(event.state, event.message)
                    is SaleEvent.Progress -> {
                        _screenStatus.postValue(event.message)
                        appLogRepository.add(AppLogCategory.SALE, event.message)
                    }
                    is SaleEvent.EmvDataReady -> {
                        emvTagSummary = event.summary
                        field55Data = Field55Builder.build(event.summary)
                        _voucherSummary.postValue(
                            "Sale in progress\nAmount: ${request.amount.formatted()}\n${event.summary.toVoucherCardSummary()}"
                        )
                        appLogRepository.add(
                            AppLogCategory.EMV,
                            event.summary.toLogText()
                        )
                        appLogRepository.add(
                            AppLogCategory.ISO8583,
                            field55Data!!.toLogText()
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
                        voucher = result.saleResult.toVoucherUiModel(currentBreakdown)
                    )
                }
                is SaleDeviceResult.Failed -> {
                    publishState(SaleState.ERROR, result.message)
                    finishSale(
                        summary = buildErrorReceiptPreview(
                            amount = request.amount,
                            message = result.message
                        ),
                        voucher = buildErrorVoucherUiModel(
                            breakdown = currentBreakdown,
                            message = result.message
                        )
                    )
                }
                SaleDeviceResult.Canceled -> {
                    publishState(SaleState.CANCELED, "Sale canceled")
                    finishSale(
                        summary = buildCanceledReceiptPreview(request.amount),
                        voucher = buildCanceledVoucherUiModel(currentBreakdown)
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
        _amountSummary.value = "Amount: not set"
        _screenStatus.value = "Ready for new sale"
        _voucherSummary.value = "No sale result yet."
        _voucherDetails.value = VoucherUiModel.empty()
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

    private fun finishSale(
        summary: String,
        voucher: VoucherUiModel = VoucherUiModel.empty()
    ) {
        _voucherSummary.postValue(summary)
        _voucherDetails.postValue(voucher)
        _saleComplete.postValue(true)
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
        return "Mock EMV data ready | AID=${aid ?: "unknown"} | PAN=${maskedPan?.value ?: "unavailable"} | $tagLines"
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

    private fun com.franktardencilla.mfdemoapp.domain.model.SaleResult.toVoucherUiModel(
        breakdown: SaleAmountBreakdown
    ): VoucherUiModel {
        val statusLine = when (status) {
            TransactionStatus.APPROVED -> "APPROVED"
            TransactionStatus.DECLINED -> "DECLINED"
            TransactionStatus.CANCELED -> "CANCELED"
            TransactionStatus.ERROR -> "ERROR"
            TransactionStatus.PENDING -> "PENDING"
        }
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = statusLine,
            cardLine = "${entryMode?.displayName ?: "CARD"} ${maskedPan?.value ?: "unavailable"}",
            authorizationLine = "AUTH: ${authCode ?: "--"}",
            invoiceLine = "FACT: ${stan?.padStart(6, '0') ?: "--"}",
            referenceLine = "REF: ${stan ?: "none"}",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date())}",
            amountRows = breakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: ${responseCode ?: "none"}",
            verificationLine = if (status == TransactionStatus.APPROVED) {
                "** VALID WITHOUT SIGNATURE **"
            } else {
                message
            },
            copyLine = "-- CUSTOMER COPY --"
        )
    }

    private fun buildErrorVoucherUiModel(
        breakdown: SaleAmountBreakdown,
        message: String
    ): VoucherUiModel {
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = "ERROR",
            cardLine = "CARD: ${emvTagSummary?.maskedPan?.value ?: "unavailable"}",
            authorizationLine = "AUTH: --",
            invoiceLine = "FACT: --",
            referenceLine = "REF: none",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date())}",
            amountRows = breakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: none",
            verificationLine = message,
            copyLine = "-- CUSTOMER COPY --"
        )
    }

    private fun buildCanceledVoucherUiModel(
        breakdown: SaleAmountBreakdown
    ): VoucherUiModel {
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = "CANCELED",
            cardLine = "CARD: ${emvTagSummary?.maskedPan?.value ?: "unavailable"}",
            authorizationLine = "AUTH: --",
            invoiceLine = "FACT: --",
            referenceLine = "REF: none",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date())}",
            amountRows = breakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: canceled",
            verificationLine = "Sale canceled before completion.",
            copyLine = "-- CUSTOMER COPY --"
        )
    }

    private fun SaleAmountBreakdown.toVoucherAmountRows(): List<VoucherAmountRow> {
        val rows = mutableListOf<VoucherAmountRow>()
        rows += VoucherAmountRow("BASE", baseAmount.formatted())
        if (tipAmount.minorUnits > 0) {
            rows += VoucherAmountRow("TIP", tipAmount.formatted())
        }
        if (taxAmount.minorUnits > 0) {
            rows += VoucherAmountRow("TAX", taxAmount.formatted())
        }
        rows += VoucherAmountRow("TOTAL", totalAmount.formatted(), isTotal = true)
        return rows
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

    private companion object {
        val AMOUNT_PATTERN = Regex("^\\d+(\\.\\d{1,2})?$")
        val receiptTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        const val MERCHANT_NAME = "MFDemo Merchant"
        const val MERCHANT_ID = "MFDemoMerchant"
        const val TERMINAL_ID = "DEMO920"
        const val RECEIPT_SEPARATOR = "------------------------------"
    }
}
