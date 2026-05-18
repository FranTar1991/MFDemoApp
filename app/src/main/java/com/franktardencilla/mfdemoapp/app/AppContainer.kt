package com.franktardencilla.mfdemoapp.app

import com.franktardencilla.mfdemoapp.device.MockDeviceServiceManager
import com.franktardencilla.mfdemoapp.device.MockPosDeviceAdapter
import com.franktardencilla.mfdemoapp.repository.DeviceRepository
import com.franktardencilla.mfdemoapp.repository.KeyRepository
import com.franktardencilla.mfdemoapp.repository.TransactionRepository

class AppContainer {
    private val deviceServiceManager = MockDeviceServiceManager()
    private val posDeviceAdapter = MockPosDeviceAdapter(deviceServiceManager)

    val deviceRepository = DeviceRepository(deviceServiceManager)
    val keyRepository = KeyRepository(posDeviceAdapter)
    val transactionRepository = TransactionRepository()
}
