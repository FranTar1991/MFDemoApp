package com.franktardencilla.mfdemoapp.device.morefun

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import com.franktardencilla.mfdemoapp.device.DeviceServiceManager
import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.DeviceModuleAvailability
import com.franktardencilla.mfdemoapp.domain.model.DeviceServiceSession
import com.morefun.yapi.engine.DeviceInfoConstrants
import com.morefun.yapi.engine.DeviceServiceEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MorefunDeviceServiceManager(
    context: Context
) : DeviceServiceManager {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var engine: DeviceServiceEngine? = null
    private var bindResult = CompletableDeferred<DeviceServiceEngine>()
    private var loggedIn = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName,
            service: IBinder
        ) {
            val connectedEngine = DeviceServiceEngine.Stub.asInterface(service)
            synchronized(lock) {
                engine = connectedEngine
                if (!bindResult.isCompleted) {
                    bindResult.complete(connectedEngine)
                }
            }
            linkToDeath(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            markDisconnected()
        }
    }

    override suspend fun login(): DeviceConnectionStatus {
        return loginWithBusinessId(DEFAULT_BUSINESS_ID)
    }

    suspend fun loginWithBusinessId(businessId: String): DeviceConnectionStatus {
        return withContext(Dispatchers.IO) {
            val connectedEngine = bindService()
                ?: return@withContext DeviceConnectionStatus(
                    isConnected = false,
                    message = "YSDK service unavailable. Install/open com.morefun.ysdk on the terminal."
                )

            val loginResult = connectedEngine.login(Bundle(), businessId)
            loggedIn = loginResult == SERVICE_SUCCESS
            if (loggedIn) {
                val modelLine = connectedEngine.deviceModelLine()
                DeviceConnectionStatus(
                    isConnected = true,
                    message = "YSDK service connected businessId=$businessId$modelLine"
                )
            } else {
                DeviceConnectionStatus(
                    isConnected = false,
                    message = "YSDK login failed businessId=$businessId. Result code: $loginResult"
                )
            }
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            runCatching {
                engine?.logout()
            }
            loggedIn = false
            runCatching {
                appContext.unbindService(serviceConnection)
            }
            markDisconnected()
        }
    }

    override suspend fun getStatus(): DeviceConnectionStatus {
        return withContext(Dispatchers.IO) {
            val connected = engine != null && loggedIn
            DeviceConnectionStatus(
                isConnected = connected,
                message = if (connected) {
                    "YSDK service: connected"
                } else {
                    "YSDK service: disconnected. Connect to continue."
                }
            )
        }
    }

    override suspend fun getSession(): DeviceServiceSession? {
        return withContext(Dispatchers.IO) {
            val connectedEngine = engine ?: return@withContext null
            if (!loggedIn) {
                return@withContext null
            }

            DeviceServiceSession(
                isLoggedIn = true,
                modules = connectedEngine.moduleAvailability(),
                message = connectedEngine.deviceModelLine().ifBlank {
                    "MoreFun YSDK session ready"
                }
            )
        }
    }

    suspend fun requireEngine(): DeviceServiceEngine {
        return withContext(Dispatchers.IO) {
            engine ?: bindService() ?: throw RemoteException("YSDK service is not connected.")
        }
    }

    private suspend fun bindService(): DeviceServiceEngine? {
        synchronized(lock) {
            engine?.let { return it }
            if (bindResult.isCompleted) {
                bindResult = CompletableDeferred()
            }
        }

        val intent = Intent().apply {
            action = SERVICE_ACTION
            setPackage(SERVICE_PACKAGE)
        }
        val bindStarted = appContext.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        if (!bindStarted) {
            markDisconnected()
            return null
        }

        return withTimeoutOrNull(BIND_TIMEOUT_MILLIS) {
            bindResult.await()
        }
    }

    private fun DeviceServiceEngine.moduleAvailability(): DeviceModuleAvailability {
        return DeviceModuleAvailability(
            emvAvailable = runCatching { getEmvHandler() != null }.getOrDefault(false),
            pinPadAvailable = runCatching { getPinPad() != null }.getOrDefault(false),
            pedAvailable = runCatching { getPed() != null }.getOrDefault(false),
            networkAvailable = runCatching { getNetWorkHandler() != null }.getOrDefault(false),
            printerAvailable = runCatching { getMultipleAppPrinter() != null }.getOrDefault(false),
            beeperAvailable = runCatching { getBeeper() != null }.getOrDefault(false)
        )
    }

    private fun DeviceServiceEngine.deviceModelLine(): String {
        return runCatching {
            val devInfo = getDevInfo()
            val model = devInfo.getString(DeviceInfoConstrants.COMMOM_MODEL).orEmpty()
            val serial = devInfo.getString(DeviceInfoConstrants.COMMOM_SN).orEmpty()
            val serviceVersion = devInfo.getString(DeviceInfoConstrants.COMMON_SERVICE_VER).orEmpty()
            listOf(
                model.takeIf { it.isNotBlank() }?.let { "model=$it" },
                serial.takeIf { it.isNotBlank() }?.let { "sn=$it" },
                serviceVersion.takeIf { it.isNotBlank() }?.let { "service=$it" }
            ).filterNotNull()
                .joinToString(separator = " | ")
                .takeIf { it.isNotBlank() }
                ?.let { "\n$it" }
                .orEmpty()
        }.getOrDefault("")
    }

    private fun linkToDeath(service: IBinder) {
        runCatching {
            service.linkToDeath(
                {
                    markDisconnected()
                },
                0
            )
        }
    }

    private fun markDisconnected() {
        synchronized(lock) {
            engine = null
            loggedIn = false
            if (!bindResult.isCompleted) {
                bindResult.completeExceptionally(RemoteException("YSDK binder disconnected."))
            }
            bindResult = CompletableDeferred()
        }
    }

    private companion object {
        const val SERVICE_ACTION = "com.morefun.ysdk.service"
        const val SERVICE_PACKAGE = "com.morefun.ysdk"
        const val DEFAULT_BUSINESS_ID = "01000000"
        const val SERVICE_SUCCESS = 0
        const val BIND_TIMEOUT_MILLIS = 5_000L
    }
}
