package com.franktardencilla.mfdemoapp.device

import com.franktardencilla.mfdemoapp.domain.model.SaleResult

sealed interface SaleDeviceResult {
    data class Completed(val saleResult: SaleResult) : SaleDeviceResult

    data class Failed(val message: String) : SaleDeviceResult

    data object Canceled : SaleDeviceResult
}
