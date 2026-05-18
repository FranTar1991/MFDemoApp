package com.franktardencilla.mfdemoapp.domain.model

data class DeviceModuleAvailability(
    val emvAvailable: Boolean,
    val pinPadAvailable: Boolean,
    val pedAvailable: Boolean,
    val networkAvailable: Boolean,
    val printerAvailable: Boolean,
    val beeperAvailable: Boolean
) {
    fun displayLines(): List<String> {
        return listOf(
            "EMV: ${emvAvailable.displayValue()}",
            "PinPad: ${pinPadAvailable.displayValue()}",
            "PED: ${pedAvailable.displayValue()}",
            "Network: ${networkAvailable.displayValue()}",
            "Printer: ${printerAvailable.displayValue()}",
            "Beeper: ${beeperAvailable.displayValue()}"
        )
    }

    private fun Boolean.displayValue(): String {
        return if (this) "available" else "unavailable"
    }
}
