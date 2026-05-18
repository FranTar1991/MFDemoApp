package com.franktardencilla.mfdemoapp.app

import android.content.Context
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.SaleRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class AppContainer(
    context: Context,
    runtimeMode: AppRuntimeMode = AppRuntimeMode.MOCK
) {
    private val posDependencies = PosDependencyFactory(context)
        .create(runtimeMode)

    val deviceRepository = DeviceRepository(posDependencies.deviceServiceManager)
    val keyRepository = KeyRepository(posDependencies.posDeviceAdapter)
    val saleRepository = SaleRepository(posDependencies.posDeviceAdapter)
    val transactionRepository = TransactionRepository()
    val appLogRepository = AppLogRepository()
}
