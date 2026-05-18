package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.device.PosDeviceAdapter
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KeyRepository(
    private val posDeviceAdapter: PosDeviceAdapter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getKeyStatus(): KeyStatus {
        return withContext(ioDispatcher) {
            posDeviceAdapter.getKeyStatus()
        }
    }

    suspend fun injectDemoKeys(): KeyStatus {
        return withContext(ioDispatcher) {
            posDeviceAdapter.injectDemoKeys()
        }
    }

    suspend fun clearKeys(): KeyStatus {
        return withContext(ioDispatcher) {
            posDeviceAdapter.clearKeys()
        }
    }
}
