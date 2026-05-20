package com.franktardencilla.mfdemoapp.ui.sale

import com.franktardencilla.mfdemoapp.domain.model.SaleAmountBreakdown
import com.franktardencilla.mfdemoapp.domain.model.SaleResult
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import com.franktardencilla.mfdemoapp.domain.model.TransactionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoucherMapper {
    fun fromSaleResult(
        saleResult: SaleResult,
        createdAtMillis: Long = System.currentTimeMillis()
    ): VoucherUiModel {
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = saleResult.status.toVoucherStatus(),
            cardLine = "${saleResult.entryMode?.displayName ?: "CARD"} ${saleResult.maskedPan?.value ?: "unavailable"}",
            authorizationLine = "AUTH: ${saleResult.authCode ?: "--"}",
            invoiceLine = "FACT: ${saleResult.stan?.padStart(6, '0') ?: "--"}",
            referenceLine = "REF: ${saleResult.stan ?: "none"}",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date(createdAtMillis))}",
            amountRows = saleResult.amountBreakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: ${saleResult.responseCode ?: "none"}",
            verificationLine = if (saleResult.status == TransactionStatus.APPROVED) {
                VALID_WITHOUT_SIGNATURE
            } else {
                saleResult.message
            },
            copyLine = CUSTOMER_COPY
        )
    }

    fun fromTransaction(transaction: TransactionSummary): VoucherUiModel {
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = transaction.status.toVoucherStatus(),
            cardLine = "${transaction.entryMode?.displayName ?: "CARD"} ${transaction.maskedPan?.value ?: "unavailable"}",
            authorizationLine = "AUTH: ${transaction.authCode ?: "--"}",
            invoiceLine = "FACT: ${transaction.stan?.padStart(6, '0') ?: "--"}",
            referenceLine = "REF: ${transaction.stan ?: "none"}",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date(transaction.createdAtMillis))}",
            amountRows = transaction.amountBreakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: ${transaction.responseCode ?: "none"}",
            verificationLine = if (transaction.status == TransactionStatus.APPROVED) {
                VALID_WITHOUT_SIGNATURE
            } else {
                transaction.message ?: "Transaction was not approved."
            },
            copyLine = CUSTOMER_COPY
        )
    }

    fun error(
        breakdown: SaleAmountBreakdown,
        message: String,
        cardLine: String = "CARD: unavailable"
    ): VoucherUiModel {
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = "ERROR",
            cardLine = cardLine,
            authorizationLine = "AUTH: --",
            invoiceLine = "FACT: --",
            referenceLine = "REF: none",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date())}",
            amountRows = breakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: none",
            verificationLine = message,
            copyLine = CUSTOMER_COPY
        )
    }

    fun canceled(
        breakdown: SaleAmountBreakdown,
        cardLine: String = "CARD: unavailable"
    ): VoucherUiModel {
        return VoucherUiModel(
            merchantName = MERCHANT_NAME,
            terminalId = TERMINAL_ID,
            merchantId = MERCHANT_ID,
            transactionName = "SALE",
            status = "CANCELED",
            cardLine = cardLine,
            authorizationLine = "AUTH: --",
            invoiceLine = "FACT: --",
            referenceLine = "REF: none",
            dateLine = "DATE: ${receiptTimeFormatter.format(Date())}",
            amountRows = breakdown.toVoucherAmountRows(),
            responseLine = "RESPONSE: canceled",
            verificationLine = "Sale canceled before completion.",
            copyLine = CUSTOMER_COPY
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

    private fun TransactionStatus.toVoucherStatus(): String {
        return when (this) {
            TransactionStatus.APPROVED -> "APPROVED"
            TransactionStatus.DECLINED -> "DECLINED"
            TransactionStatus.CANCELED -> "CANCELED"
            TransactionStatus.ERROR -> "ERROR"
            TransactionStatus.PENDING -> "PENDING"
        }
    }

    private companion object {
        val receiptTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        const val MERCHANT_NAME = "MFDemo Merchant"
        const val MERCHANT_ID = "MFDemoMerchant"
        const val TERMINAL_ID = "DEMO920"
        const val VALID_WITHOUT_SIGNATURE = "** VALID WITHOUT SIGNATURE **"
        const val CUSTOMER_COPY = "-- CUSTOMER COPY --"
    }
}
