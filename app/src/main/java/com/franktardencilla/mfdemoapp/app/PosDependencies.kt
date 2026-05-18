package com.franktardencilla.mfdemoapp.app

import com.franktardencilla.mfdemoapp.device.DeviceServiceManager
import com.franktardencilla.mfdemoapp.device.PosDeviceAdapter

data class PosDependencies(
    val deviceServiceManager: DeviceServiceManager,
    val posDeviceAdapter: PosDeviceAdapter
)
