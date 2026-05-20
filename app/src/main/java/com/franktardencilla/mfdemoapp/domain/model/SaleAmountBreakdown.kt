package com.franktardencilla.mfdemoapp.domain.model

data class SaleAmountBreakdown(
    val baseAmount: MoneyAmount,
    val tipAmount: MoneyAmount,
    val taxAmount: MoneyAmount,
    val totalAmount: MoneyAmount
) {
    init {
        require(totalAmount.minorUnits == baseAmount.minorUnits + tipAmount.minorUnits + taxAmount.minorUnits) {
            "Total amount must equal base amount plus tip and tax."
        }
        require(baseAmount.currencyCode == tipAmount.currencyCode) {
            "Base and tip currency codes must match."
        }
        require(baseAmount.currencyCode == taxAmount.currencyCode) {
            "Base and tax currency codes must match."
        }
        require(baseAmount.currencySymbol == tipAmount.currencySymbol) {
            "Base and tip currency symbols must match."
        }
        require(baseAmount.currencySymbol == taxAmount.currencySymbol) {
            "Base and tax currency symbols must match."
        }
    }

    fun formattedSummary(): String {
        return listOf(
            "Base: ${baseAmount.formatted()}",
            "Tip: ${tipAmount.formatted()}",
            "Tax: ${taxAmount.formatted()}",
            "Total: ${totalAmount.formatted()}"
        ).joinToString(separator = "\n")
    }

    companion object {
        fun fromBaseAmount(amount: MoneyAmount): SaleAmountBreakdown {
            val zero = MoneyAmount(
                minorUnits = 0,
                currencyCode = amount.currencyCode,
                currencySymbol = amount.currencySymbol
            )
            return fromParts(
                baseAmount = amount,
                tipAmount = zero,
                taxAmount = zero
            )
        }

        fun fromParts(
            baseAmount: MoneyAmount,
            tipAmount: MoneyAmount,
            taxAmount: MoneyAmount
        ): SaleAmountBreakdown {
            return SaleAmountBreakdown(
                baseAmount = baseAmount,
                tipAmount = tipAmount,
                taxAmount = taxAmount,
                totalAmount = MoneyAmount(
                    minorUnits = baseAmount.minorUnits + tipAmount.minorUnits + taxAmount.minorUnits,
                    currencyCode = baseAmount.currencyCode,
                    currencySymbol = baseAmount.currencySymbol
                )
            )
        }
    }
}
