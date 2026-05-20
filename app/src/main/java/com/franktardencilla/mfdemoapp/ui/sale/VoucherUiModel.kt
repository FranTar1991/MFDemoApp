package com.franktardencilla.mfdemoapp.ui.sale

data class VoucherUiModel(
    val merchantName: String,
    val terminalId: String,
    val merchantId: String,
    val transactionName: String,
    val status: String,
    val cardLine: String,
    val authorizationLine: String,
    val invoiceLine: String,
    val referenceLine: String,
    val dateLine: String,
    val amountRows: List<VoucherAmountRow>,
    val responseLine: String,
    val verificationLine: String,
    val copyLine: String
) {
    companion object {
        fun empty(): VoucherUiModel {
            return VoucherUiModel(
                merchantName = "MFDemo Merchant",
                terminalId = "DEMO920",
                merchantId = "MFDemoMerchant",
                transactionName = "SALE",
                status = "NO RESULT",
                cardLine = "CARD: unavailable",
                authorizationLine = "AUTH: none",
                invoiceLine = "FACT: none",
                referenceLine = "REF: none",
                dateLine = "DATE: --",
                amountRows = listOf(
                    VoucherAmountRow("TOTAL", "$0.00", isTotal = true)
                ),
                responseLine = "RESPONSE: none",
                verificationLine = "",
                copyLine = "-- CUSTOMER COPY --"
            )
        }
    }
}

data class VoucherAmountRow(
    val label: String,
    val value: String,
    val isTotal: Boolean = false
)
