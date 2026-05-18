package com.franktardencilla.mfdemoapp.app

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.franktardencilla.mfdemoapp.data.applog.AppLogDatabase
import com.franktardencilla.mfdemoapp.data.transaction.TransactionDatabase
import com.franktardencilla.mfdemoapp.repository.AppLogRepository
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.HostConfigRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.SaleRepository
import com.franktardencilla.mfdemoapp.repository.StanRepository
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
    )
        .addMigrations(TRANSACTION_DB_1_2)
        .build()
    private val appLogDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppLogDatabase::class.java,
        "app_logs.db"
    ).build()
    val stanRepository = StanRepository(
        transactionDatabase.transactionDao()
    )
    private val posDependencies = PosDependencyFactory(
        context,
        hostConfigRepository,
        stanRepository
    )
        .create(runtimeMode)

    val deviceRepository = DeviceRepository(posDependencies.deviceServiceManager)
    val keyRepository = KeyRepository(posDependencies.posDeviceAdapter)
    val saleRepository = SaleRepository(posDependencies.posDeviceAdapter)
    val transactionRepository = TransactionRepository(
        transactionDatabase.transactionDao()
    )
    val appLogRepository = AppLogRepository(
        appLogDatabase.appLogDao()
    )

    private companion object {
        val TRANSACTION_DB_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transaction_records ADD COLUMN isoRequestSummary TEXT")
                db.execSQL("ALTER TABLE transaction_records ADD COLUMN isoResponseSummary TEXT")
                db.execSQL("ALTER TABLE transaction_records ADD COLUMN emvTagSummary TEXT")
            }
        }
    }
}
