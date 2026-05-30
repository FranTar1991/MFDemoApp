package com.franktardencilla.mfdemoapp.repository

import android.graphics.Bitmap
import com.franktardencilla.mfdemoapp.device.PosDeviceAdapter
import com.franktardencilla.mfdemoapp.device.PrintResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrinterRepository(
    private val posDeviceAdapter: PosDeviceAdapter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun printVoucher(voucherBitmap: Bitmap): PrintResult {
        return withContext(ioDispatcher) {
            posDeviceAdapter.printVoucher(voucherBitmap)
        }
    }
}
