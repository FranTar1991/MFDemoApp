package com.franktardencilla.mfdemoapp.domain.model

import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

class SaleIsoRequestBuilder(
    private val terminalId: String = "DEMO920",
    private val merchantId: String = "MFDemoMerchant",
    private val clock: Clock = Clock.systemDefaultZone(),
    private val stanProvider: () -> String
) {
    fun build(
        request: SaleRequest,
        entryMode: CardEntryMode,
        field55Data: Field55Data? = null
    ): Iso8583Message {
        val transactionDateTime = LocalDateTime.now(clock)
        return Iso8583Message(
            mti = SALE_MTI,
            fields = buildMap {
                putAll(
                    mapOf(
                        FIELD_PROCESSING_CODE to SALE_PROCESSING_CODE,
                        FIELD_AMOUNT to request.amount.isoAmount12(),
                        FIELD_TRANSMISSION_DATE_TIME to transactionDateTime.format(TRANSMISSION_DATE_TIME_FORMAT),
                        FIELD_STAN to stanProvider(),
                        FIELD_LOCAL_TIME to transactionDateTime.format(LOCAL_TIME_FORMAT),
                        FIELD_LOCAL_DATE to transactionDateTime.format(LOCAL_DATE_FORMAT),
                        FIELD_ENTRY_MODE to entryMode.posEntryMode,
                        FIELD_NII to NETWORK_INTERNATIONAL_ID,
                        FIELD_POS_CONDITION_CODE to NORMAL_PRESENTMENT_CONDITION_CODE,
                        FIELD_TERMINAL_ID to terminalId,
                        FIELD_MERCHANT_ID to merchantId,
                        FIELD_CURRENCY_CODE to request.amount.currencyCode
                    )
                )
                if (field55Data != null) {
                    put(FIELD_55, field55Data.tlvHex)
                }
            }
        )
    }

    private companion object {
        const val SALE_MTI = "0200"
        const val SALE_PROCESSING_CODE = "000000"
        const val NETWORK_INTERNATIONAL_ID = "001"
        const val NORMAL_PRESENTMENT_CONDITION_CODE = "00"
        const val FIELD_PROCESSING_CODE = 3
        const val FIELD_AMOUNT = 4
        const val FIELD_TRANSMISSION_DATE_TIME = 7
        const val FIELD_STAN = 11
        const val FIELD_LOCAL_TIME = 12
        const val FIELD_LOCAL_DATE = 13
        const val FIELD_ENTRY_MODE = 22
        const val FIELD_NII = 24
        const val FIELD_POS_CONDITION_CODE = 25
        const val FIELD_TERMINAL_ID = 41
        const val FIELD_MERCHANT_ID = 42
        const val FIELD_CURRENCY_CODE = 49
        const val FIELD_55 = 55
        val TRANSMISSION_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMddHHmmss")
        val LOCAL_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")
        val LOCAL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")
    }
}
