package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.device.PosDeviceAdapter
import com.franktardencilla.mfdemoapp.device.TrackAKeyInjectionEventSink
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
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

    suspend fun injectTrackAKeys(
        request: TrackAKeyInjectionRequest,
        events: TrackAKeyInjectionEventSink
    ): KeyStatus {
        return withContext(ioDispatcher) {
            posDeviceAdapter.injectTrackAKeys(request, events)
        }
    }

    suspend fun clearKeys(): KeyStatus {
        return withContext(ioDispatcher) {
            posDeviceAdapter.clearKeys()
        }
    }
}
