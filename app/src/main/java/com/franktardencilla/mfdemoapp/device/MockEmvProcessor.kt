package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.EmvTag
import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.MaskedPan
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest

class MockEmvProcessor : EmvProcessor {
    override suspend fun readEmvData(request: SaleRequest): EmvTagSummary {
        val maskedPan = MaskedPan.fromPlainPan(MOCK_PAN)
        return EmvTagSummary(
            aid = MOCK_AID,
            maskedPan = maskedPan,
            tags = listOf(
                EmvTag("5A", maskedPan.value, "Application PAN (masked)"),
                EmvTag("5F24", "271231", "Application expiration date"),
                EmvTag("9F02", request.amount.isoAmount12(), "Authorized amount"),
                EmvTag("5F2A", request.amount.currencyCode, "Transaction currency code"),
                EmvTag("95", "0000000000", "Terminal verification results"),
                EmvTag("9A", "260518", "Transaction date"),
                EmvTag("9C", "00", "Transaction type"),
                EmvTag("9F26", "1122334455667788", "Application cryptogram"),
                EmvTag("9F27", "80", "Cryptogram information data"),
                EmvTag("9F10", "06011203A0B800", "Issuer application data"),
                EmvTag("4F", MOCK_AID, "Application identifier"),
                EmvTag("84", MOCK_AID, "Dedicated file name")
            )
        )
    }

    private companion object {
        const val MOCK_PAN = "4111111111111111"
        const val MOCK_AID = "A0000000031010"
    }
}
