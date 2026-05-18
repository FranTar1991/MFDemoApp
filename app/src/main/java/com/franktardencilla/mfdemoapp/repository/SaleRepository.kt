package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.device.PosDeviceAdapter
import com.franktardencilla.mfdemoapp.device.SaleDeviceResult
import com.franktardencilla.mfdemoapp.device.SaleEventSink
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaleRepository(
    private val posDeviceAdapter: PosDeviceAdapter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun startSale(
        request: SaleRequest,
        events: SaleEventSink
    ): SaleDeviceResult {
        return withContext(ioDispatcher) {
            posDeviceAdapter.startSale(request, events)
        }
    }

    suspend fun cancelSale() {
        withContext(ioDispatcher) {
            posDeviceAdapter.cancelCurrentOperation()
        }
    }
}
