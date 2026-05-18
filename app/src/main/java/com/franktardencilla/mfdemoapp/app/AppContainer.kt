package com.franktardencilla.mfdemoapp.app

import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class AppContainer {
    val deviceRepository = DeviceRepository()
    val keyRepository = KeyRepository()
    val transactionRepository = TransactionRepository()
}
