package com.franktardencilla.mfdemoapp.domain.model

data class DeviceServiceSession(
    val isLoggedIn: Boolean,
    val modules: DeviceModuleAvailability,
    val message: String
) {
    val canStartPaymentFlow: Boolean
        get() = isLoggedIn &&
            modules.emvAvailable &&
            (modules.pinPadAvailable || modules.pedAvailable)
}
