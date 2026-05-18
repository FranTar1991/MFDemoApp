package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.Field55Data
import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleIsoRequestBuilder
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaleIsoRequestBuilderTest {
    @Test
    fun build_usesInjectedStanProvider() {
        val builder = SaleIsoRequestBuilder(
            stanProvider = { "123456" }
        )

        val message = builder.build(
            request = SaleRequest(
                amount = MoneyAmount(minorUnits = 1234)
            ),
            entryMode = CardEntryMode.CONTACT,
            field55Data = Field55Data(
                tlvHex = "9F0206000000001234",
                includedTags = listOf("9F02")
            )
        )

        assertEquals("123456", message.get(11))
    }
}
