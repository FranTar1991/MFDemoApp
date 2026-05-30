package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.Field55Data
import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleIsoRequestBuilder
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaleIsoRequestBuilderTest {
    @Test
    fun build_includesMinimumSaleFields() {
        val builder = SaleIsoRequestBuilder(
            clock = Clock.fixed(
                Instant.parse("2026-05-20T14:35:40Z"),
                ZoneOffset.UTC
            ),
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

        assertEquals("0200", message.mti)
        assertEquals("000000", message.get(3))
        assertEquals("000000001234", message.get(4))
        assertEquals("0520143540", message.get(7))
        assertEquals("123456", message.get(11))
        assertEquals("143540", message.get(12))
        assertEquals("0520", message.get(13))
        assertEquals("051", message.get(22))
        assertEquals("001", message.get(24))
        assertEquals("00", message.get(25))
        assertEquals("DEMO920", message.get(41))
        assertEquals("MFDemoMerchant", message.get(42))
        assertEquals("840", message.get(49))
        assertEquals("9F0206000000001234", message.get(55))
    }

    @Test
    fun build_omitsField55ForMagstripeRequests() {
        val builder = SaleIsoRequestBuilder(
            clock = Clock.fixed(
                Instant.parse("2026-05-20T14:35:40Z"),
                ZoneOffset.UTC
            ),
            stanProvider = { "123456" }
        )

        val message = builder.build(
            request = SaleRequest(
                amount = MoneyAmount(minorUnits = 1234)
            ),
            entryMode = CardEntryMode.MAGSTRIPE
        )

        assertEquals("0200", message.mti)
        assertEquals("022", message.get(22))
        assertNull(message.get(55))
    }
}
