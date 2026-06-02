package com.franktardencilla.mfdemoapp.app

import android.content.Context
import com.franktardencilla.mfdemoapp.device.SocketIso8583HostClient
import com.franktardencilla.mfdemoapp.device.morefun.MorefunDeviceServiceManager
import com.franktardencilla.mfdemoapp.device.morefun.RealYsdkPosDeviceAdapter
import com.franktardencilla.mfdemoapp.domain.model.SaleIsoRequestBuilder
import com.franktardencilla.mfdemoapp.repository.HostConfigRepository
import com.franktardencilla.mfdemoapp.repository.StanRepository

class PosDependencyFactory(
    private val context: Context,
    private val hostConfigRepository: HostConfigRepository,
    private val stanRepository: StanRepository
) {
    fun create(): PosDependencies {
        val deviceServiceManager = MorefunDeviceServiceManager(context)
        val hostClient = SocketIso8583HostClient(hostConfigRepository)
        val saleIsoRequestBuilder = SaleIsoRequestBuilder(
            stanProvider = stanRepository::nextStan
        )
        return PosDependencies(
            deviceServiceManager = deviceServiceManager,
            posDeviceAdapter = RealYsdkPosDeviceAdapter(
                serviceManager = deviceServiceManager,
                hostClient = hostClient,
                saleIsoRequestBuilder = saleIsoRequestBuilder
            )
        )
    }
}
