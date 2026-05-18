package com.franktardencilla.mfdemoapp.app

import android.content.Context
import androidx.room.Room
import com.franktardencilla.mfdemoapp.data.transaction.TransactionDatabase
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.HostConfigRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.SaleRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class AppContainer(
    context: Context,
    runtimeMode: AppRuntimeMode = AppRuntimeMode.MOCK
) {
    val hostConfigRepository = HostConfigRepository(context)
    private val transactionDatabase = Room.databaseBuilder(
        context.applicationContext,
        TransactionDatabase::class.java,
        "transactions.db"
    ).build()
    private val posDependencies = PosDependencyFactory(
        context,
        hostConfigRepository
    )
        .create(runtimeMode)

    val deviceRepository = DeviceRepository(posDependencies.deviceServiceManager)
    val keyRepository = KeyRepository(posDependencies.posDeviceAdapter)
    val saleRepository = SaleRepository(posDependencies.posDeviceAdapter)
    val transactionRepository = TransactionRepository(
        transactionDatabase.transactionDao()
    )
    val appLogRepository = AppLogRepository()
}
