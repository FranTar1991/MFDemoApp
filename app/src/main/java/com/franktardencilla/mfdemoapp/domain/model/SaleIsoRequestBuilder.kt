package com.franktardencilla.mfdemoapp.domain.model

import java.util.concurrent.atomic.AtomicInteger

class SaleIsoRequestBuilder(
    private val terminalId: String = "DEMO920",
    private val merchantId: String = "MFDemoMerchant",
    private val stanProvider: () -> String = { nextStan() }
) {
    fun build(
        request: SaleRequest,
        entryMode: CardEntryMode,
        field55Data: Field55Data
    ): Iso8583Message {
        return Iso8583Message(
            mti = SALE_MTI,
            fields = mapOf(
                FIELD_PROCESSING_CODE to SALE_PROCESSING_CODE,
                FIELD_AMOUNT to request.amount.isoAmount12(),
                FIELD_STAN to stanProvider(),
                FIELD_ENTRY_MODE to entryMode.posEntryMode,
                FIELD_NII to NETWORK_INTERNATIONAL_ID,
                FIELD_TERMINAL_ID to terminalId,
                FIELD_MERCHANT_ID to merchantId,
                FIELD_CURRENCY_CODE to request.amount.currencyCode,
                FIELD_55 to field55Data.tlvHex
            )
        )
    }

    private companion object {
        const val SALE_MTI = "0200"
        const val SALE_PROCESSING_CODE = "000000"
        const val NETWORK_INTERNATIONAL_ID = "001"
        const val FIELD_PROCESSING_CODE = 3
        const val FIELD_AMOUNT = 4
        const val FIELD_STAN = 11
        const val FIELD_ENTRY_MODE = 22
        const val FIELD_NII = 24
        const val FIELD_TERMINAL_ID = 41
        const val FIELD_MERCHANT_ID = 42
        const val FIELD_CURRENCY_CODE = 49
        const val FIELD_55 = 55

        val stanCounter = AtomicInteger(1)

        fun nextStan(): String {
            val nextValue = stanCounter.getAndUpdate { current ->
                if (current >= 999999) 1 else current + 1
            }
            return nextValue.toString().padStart(6, '0')
        }
    }
}
