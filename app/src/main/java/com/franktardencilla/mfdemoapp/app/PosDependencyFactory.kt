package com.franktardencilla.mfdemoapp.app

import android.content.Context
import androidx.room.Room
import com.franktardencilla.mfdemoapp.data.mockped.MockPedDatabase
import com.franktardencilla.mfdemoapp.data.mockped.RoomMockPedKeyStore
import com.franktardencilla.mfdemoapp.device.MockDeviceServiceManager
import com.franktardencilla.mfdemoapp.device.MockEmvProcessor
import com.franktardencilla.mfdemoapp.device.MockPed
import com.franktardencilla.mfdemoapp.device.MockPosDeviceAdapter
import com.franktardencilla.mfdemoapp.device.SocketIso8583HostClient
import com.franktardencilla.mfdemoapp.repository.HostConfigRepository

class PosDependencyFactory(
    private val context: Context,
    private val hostConfigRepository: HostConfigRepository
) {
    fun create(runtimeMode: AppRuntimeMode): PosDependencies {
        return when (runtimeMode) {
            AppRuntimeMode.MOCK -> createMockDependencies()
            AppRuntimeMode.REAL_YSDK -> {
                error("REAL_YSDK mode is not wired yet. Add YsdkDeviceServiceManager and YsdkPosDeviceAdapter first.")
            }
        }
    }

    private fun createMockDependencies(): PosDependencies {
        val mockPedDatabase = Room.databaseBuilder(
            context.applicationContext,
            MockPedDatabase::class.java,
            "mock_ped.db"
        ).build()
        val mockPedKeyStore = RoomMockPedKeyStore(
            mockPedDatabase.mockPedKeySlotDao()
        )
        val mockPed = MockPed(mockPedKeyStore)
        val emvProcessor = MockEmvProcessor()
        val hostClient = SocketIso8583HostClient(hostConfigRepository)
        val deviceServiceManager = MockDeviceServiceManager()
        val posDeviceAdapter = MockPosDeviceAdapter(
            deviceServiceManager,
            mockPed,
            emvProcessor,
            hostClient
        )

        return PosDependencies(
            deviceServiceManager = deviceServiceManager,
            posDeviceAdapter = posDeviceAdapter
        )
    }
}
