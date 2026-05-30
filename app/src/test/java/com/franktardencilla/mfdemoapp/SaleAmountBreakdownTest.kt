package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleAmountBreakdown
import org.junit.Assert.assertEquals
import org.junit.Test

class SaleAmountBreakdownTest {
    @Test
    fun fromParts_totalsBaseTipAndTax() {
        val breakdown = SaleAmountBreakdown.fromParts(
            baseAmount = MoneyAmount(minorUnits = 10_00),
            tipAmount = MoneyAmount(minorUnits = 2_50),
            taxAmount = MoneyAmount(minorUnits = 1_15)
        )

        assertEquals(13_65, breakdown.totalAmount.minorUnits)
        assertEquals(
            "Base: $10.00\nTip: $2.50\nTax: $1.15\nTotal: $13.65",
            breakdown.formattedSummary()
        )
    }
}
